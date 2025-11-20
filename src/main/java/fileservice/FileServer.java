package fileservice;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileServer {
    private static final int PORT = 8080;
    private final ServerManager serverManager;
    private final FileStorage storage;
    private final ScheduledExecutorService scheduler;

    public FileServer() throws IOException {
        this.storage = new FileStorage();
        this.serverManager = new ServerManager(PORT, storage);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        serverManager.start();
        scheduleCleanup();
        setupShutdownHook();

        System.out.println("File Server started on http://localhost:" + PORT);
    }

    private void scheduleCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                storage.cleanupOldFiles();
            } catch (Exception e) {
                System.err.println("Scheduled cleanup failed: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.DAYS);
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            serverManager.stop();
            scheduler.shutdown();
            System.out.println("Server stopped gracefully");
        }));
    }

    public static void main(String[] args) throws IOException {
        new FileServer().start();
    }
}