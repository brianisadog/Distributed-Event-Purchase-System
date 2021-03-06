package EventService.Servlet;

import EventService.EventConcurrency.Event;
import EventService.EventServiceDriver;
import Usage.State;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * PurchaseServlet class to handle request for purchasing tickets.
 */
public class PurchaseServlet extends BaseServlet {

    /**
     * doPost to purchase tickets.
     * After purchased, send a POST request to User Service to add tickets into a user's account.
     * If the User Service response with 400, rollback the tickets just purchased.
     *
     * @param request
     * @param response
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("[Servlet] POST request " + request.getRequestURI());

        response.setContentType(EventServiceDriver.APP_TYPE);
        response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);

        try {
            String requestBody = parseRequest(request);
            JsonObject body = (JsonObject) parseJson(requestBody);

            // for secondary to check the order of the timestamp
            timestampBlock(body);

            String uuid = body.get("uuid").getAsString();
            int eventIdURI = Integer.parseInt(request.getRequestURI().replaceFirst("/purchase/", ""));
            int eventId = body.get("eventid").getAsInt();
            int userId = body.get("userid").getAsInt();
            int tickets = body.get("tickets").getAsInt();

            if (eventIdURI == eventId && tickets > 0) {
                Event event = EventServiceDriver.eventList.get(eventId);

                if (event != null) {
                    List<Integer> timestamp = new ArrayList<>();
                    if (EventServiceDriver.state == State.PRIMARY) {
                        // secondary won't rollback, so no need to lock the Lamport timestamp for the entire write
                        EventServiceDriver.lamportTimestamps.lockWrite();
                    }
                    else {
                        // for checking the match between uuid and timestamp
                        timestamp.add(body.get("timestamp").getAsInt());
                    }

                    boolean success = event.purchase(uuid, tickets, timestamp);

                    if (success && EventServiceDriver.state == State.PRIMARY) {
                        int responseCode = doPostUserTickets(userId, eventId, tickets);

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            // for primary to start the replication
                            primaryReplication(request.getRequestURI(), body, timestamp.get(0));
                            response.setStatus(responseCode);
                        }
                        else { // rollback
                            tickets *= -1;
                            event.purchase(uuid, tickets, timestamp);
                        }
                    }
                    else if (success) {
                        response.setStatus(HttpURLConnection.HTTP_OK);
                    }

                    if (EventServiceDriver.state == State.PRIMARY) {
                        EventServiceDriver.lamportTimestamps.unlockWrite();
                    }
                }
            }
        }
        catch (Exception ignored) {}
    }

    /**
     * Send a POST request to User Service to add tickets
     * into user's account and get the response code.
     *
     * @param userId
     * @param eventId
     * @param tickets
     * @return int
     */
    private int doPostUserTickets(int userId, int eventId, int tickets) {
        String primaryUser = EventServiceDriver.primaryUserService;
        String url = primaryUser + "/" + userId + "/tickets/add";
        JsonObject body = new JsonObject();
        body.addProperty("eventid", eventId);
        body.addProperty("tickets", tickets);

        int responseCode;
        try {
            HttpURLConnection connection = doPostRequest(url, body);
            responseCode = connection.getResponseCode();
        }
        catch (IOException ignored) {
            responseCode = HttpURLConnection.HTTP_BAD_REQUEST;
        }

        return responseCode;
    }
}
