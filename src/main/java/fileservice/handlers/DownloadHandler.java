package fileservice.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fileservice.FileStorage;

import java.io.IOException;
import java.nio.file.Files;

public class DownloadHandler implements HttpHandler {
    private final FileStorage storage;

    public DownloadHandler(FileStorage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("id=")) {
            ResponseHelper.sendError(exchange, 400, "Missing file ID");
            return;
        }

        String fileId = query.substring(3);
        try {
            FileStorage.FileDownload file = storage.getFile(fileId);
            if (file == null) {
                ResponseHelper.sendError(exchange, 404, "File not found");
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"" + file.originalName() + "\"");

            exchange.sendResponseHeaders(200, Files.size(file.path()));
            Files.copy(file.path(), exchange.getResponseBody());
            exchange.getResponseBody().close();

        } catch (Exception e) {
            System.err.println("Download error for file " + fileId + ": " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Download failed: " + e.getMessage());
        }
    }
}