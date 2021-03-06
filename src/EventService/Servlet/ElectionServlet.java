package EventService.Servlet;

import EventService.EventServiceDriver;
import EventService.MultithreadingProcess.BullyElection;
import Usage.State;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * ElectionServlet to handle election request.
 */
public class ElectionServlet extends BaseServlet {

    /**
     * Reply HTTP 200 and start sending election request to other event services.
     * If no reply, announce new primary. Otherwise, wait for announcement or timeout and retry.
     *
     * @param request
     * @param response
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("[Servlet] GET request /election");

        /*
        Only send the election request when current service hasn't sent one.
        When current state is "candidate", it means this service has already sent an election request.
         */
        if (EventServiceDriver.state != State.CANDIDATE) {
            // change current state
            System.out.println("[State] Change into candidate state");
            EventServiceDriver.state = State.CANDIDATE;

            // start sending election requests to services with higher rank
            Thread election = new Thread(new BullyElection());
            election.start();
        }

        // when receive a election request, reply directly to services with lower rank
        response.setContentType(EventServiceDriver.APP_TYPE);
        response.setStatus(HttpURLConnection.HTTP_OK);
    }

    /**
     * Receive announcement and change the configuration of the primary event service.
     * Turn into secondary state.
     *
     * @param request
     * @param response
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String requestBody = parseRequest(request);
            JsonObject body = (JsonObject) parseJson(requestBody);

            String host = request.getRemoteAddr();
            String port = body.get("port").getAsString();

            System.out.println("[Servlet] POST request /election from " + host + ":" + port +
                    " announcing there is a new primary");

            EventServiceDriver.eventServiceList.setPrimary(host + ":" + port);
            EventServiceDriver.state = State.SECONDARY;
            System.out.println("[State] Change into secondary state");
            response.setStatus(HttpURLConnection.HTTP_OK);
        }
        catch (IOException ioe) {
            response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
            ioe.printStackTrace();
        }

        response.setContentType(EventServiceDriver.APP_TYPE);
    }
}
