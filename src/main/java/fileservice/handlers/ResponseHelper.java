package fileservice.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class ResponseHelper {

    public static void sendResponse(HttpExchange exchange, int status, String response)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    public static void sendError(HttpExchange exchange, int status, String message)
            throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        sendResponse(exchange, status, response);
    }

    private ResponseHelper() { }
}