package fileservice;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class FileStorage {
    private final Path storageDir = Paths.get("file-storage");
    private final Path metadataFile = Paths.get("file-metadata.json");
    private final Map<String, FileMetadata> files = new ConcurrentHashMap<>();
    private static final long THIRTY_DAYS_MS = TimeUnit.DAYS.toMillis(30);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileStorage() throws IOException {
        Files.createDirectories(storageDir);
        loadMetadata();
        cleanupOldFiles();
    }

    public String saveFile(InputStream fileStream, String fileName) throws IOException {
        String fileId = UUID.randomUUID().toString();
        Path filePath = storageDir.resolve(fileId);

        Files.copy(fileStream, filePath);
        files.put(fileId, new FileMetadata(fileName, System.currentTimeMillis()));
        saveMetadata();

        System.out.printf("Saved file:%s, (ID:%s)%n", fileName, fileId);
        return fileId;
    }

    public FileDownload getFile(String fileId) throws IOException {
        FileMetadata metadata = files.get(fileId);
        if (metadata == null) return null;

        Path filePath = storageDir.resolve(fileId);
        if (!Files.exists(filePath)) {
            files.remove(fileId);
            saveMetadata();
            return null;
        }

        metadata.lastAccessTime = System.currentTimeMillis();
        saveMetadata();

        return new FileDownload(filePath, metadata.fileName);
    }

    public void cleanupOldFiles() throws IOException {
        long cutoffTime = System.currentTimeMillis() - THIRTY_DAYS_MS;
        boolean changed = false;

        Iterator<Map.Entry<String, FileMetadata>> iterator = files.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FileMetadata> entry = iterator.next();
            if (entry.getValue().lastAccessTime < cutoffTime) {
                try {
                    Files.deleteIfExists(storageDir.resolve(entry.getKey()));
                    System.out.println("Deleted old file: " + entry.getValue().fileName);
                } catch (IOException e) {
                    System.err.println("Failed to delete old file: " + entry.getKey());
                }
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            saveMetadata();
        }

        System.out.println("Cleanup completed. Removed " + (changed ? "some" : "no") + " files.");
    }

    public Stats getStats() throws IOException {
        int fileCount = files.size();
        long totalSize = 0;

        for (String fileId : files.keySet()) {
            Path filePath = storageDir.resolve(fileId);
            if (Files.exists(filePath)) {
                totalSize += Files.size(filePath);
            }
        }

        return new Stats(fileCount, totalSize);
    }

    private void loadMetadata() {
        if (!Files.exists(metadataFile)) return;

        try {
            Map<String, FileMetadata> loadedFiles = objectMapper.readValue(
                    metadataFile.toFile(),
                    new TypeReference<>() {}
            );

            files.clear();
            files.putAll(loadedFiles);
            System.out.println("Loaded metadata for " + files.size() + " files");
        } catch (Exception e) {
            System.err.println("Error loading metadata: " + e.getMessage());

            try {
                Path backup = Paths.get("file-metadata-backup.json");
                Files.copy(metadataFile, backup, StandardCopyOption.REPLACE_EXISTING);
                System.err.println("Created backup of corrupted metadata file");
            } catch (IOException backupError) {
                System.err.println("Could not create backup: " + backupError.getMessage());
            }
        }
    }

    private void saveMetadata() throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(metadataFile.toFile(), files);
    }

    public static class FileMetadata {
        public String fileName;
        public long lastAccessTime;

        public FileMetadata() {}

        public FileMetadata(String fileName, long lastAccessTime) {
            this.fileName = fileName;
            this.lastAccessTime = lastAccessTime;
        }

        public String getFileName() { return fileName; }
        public long getLastAccessTime() { return lastAccessTime; }

        public void setFileName(String fileName) { this.fileName = fileName; }
        public void setLastAccessTime(long lastAccessTime) { this.lastAccessTime = lastAccessTime; }
    }

    public record FileDownload(Path path, String originalName) { }

    public record Stats(int fileCount, long totalSize) { }
}