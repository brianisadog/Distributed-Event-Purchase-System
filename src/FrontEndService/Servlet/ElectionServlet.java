package FrontEndService.Servlet;

import FrontEndService.FrontEndServiceDriver;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;

public class ElectionServlet extends BaseServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String requestBody = parseRequest(request);
            JsonObject body = (JsonObject) parseJson(requestBody);
            String address = request.getRemoteAddr() + ":" + body.get("port").getAsString();

            System.out.println("[Servlet] POST request /election from " + address +
                    " announcing there is a new primary");

            FrontEndServiceDriver.primaryEventService = address;
            response.setStatus(HttpURLConnection.HTTP_OK);
        }
        catch (Exception ioe) {
            response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
            ioe.printStackTrace();
        }

        response.setContentType(FrontEndServiceDriver.APP_TYPE);
    }
}
