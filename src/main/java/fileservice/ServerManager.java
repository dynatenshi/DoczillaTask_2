package fileservice;

import com.sun.net.httpserver.HttpServer;
import fileservice.handlers.DownloadHandler;
import fileservice.handlers.StaticHandler;
import fileservice.handlers.StatsHandler;
import fileservice.handlers.UploadHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerManager {
    private final HttpServer server;
    private final FileStorage storage;

    public ServerManager(int port, FileStorage storage) throws IOException {
        this.storage = storage;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        setupRoutes();
    }

    private void setupRoutes() {
        server.createContext("/upload", new UploadHandler(storage));
        server.createContext("/download", new DownloadHandler(storage));
        server.createContext("/stats", new StatsHandler(storage));
        server.createContext("/", new StaticHandler());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }
}