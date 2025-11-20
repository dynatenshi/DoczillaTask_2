package fileservice.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fileservice.FileStorage;

import java.io.IOException;

public class UploadHandler implements HttpHandler {
    private final FileStorage storage;

    public UploadHandler(FileStorage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String fileName = exchange.getRequestHeaders().getFirst("X-File-Name");
            if (fileName == null) {
                fileName = "file_" + System.currentTimeMillis();
            }

            String fileId = storage.saveFile(exchange.getRequestBody(), fileName);
            String response = "{\"downloadUrl\":\"/download?id=" + fileId + "\"}";
            ResponseHelper.sendResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Upload failed: " + e.getMessage());
        }
    }
}