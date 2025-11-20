package fileservice.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fileservice.FileStorage;

import java.io.IOException;

public class StatsHandler implements HttpHandler {
    private final FileStorage storage;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatsHandler(FileStorage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            FileStorage.Stats stats = storage.getStats();

            StatsResponse responseObj = new StatsResponse(stats.fileCount(), stats.totalSize());

            String response = objectMapper.writeValueAsString(responseObj);
            ResponseHelper.sendResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Stats error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Error getting stats: " + e.getMessage());
        }
    }

    private record StatsResponse(int fileCount, long totalSize) { }
}