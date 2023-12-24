package com.civitai.server.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressBarUtils {

    public static void copyInputStreamWithProgress(InputStream inputStream, Path targetPath, long totalSize,
            String fileName)
            throws IOException {
        byte[] buffer = new byte[8192]; // You can adjust the buffer size
        long bytesRead = 0;
        int read;

        try {
            Files.createDirectories(targetPath.getParent());

            try (var outputStream = Files.newOutputStream(targetPath)) {
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    bytesRead += read;

                    // Update the progress bar in the console
                    updateProgressBar(bytesRead, totalSize, fileName);
                }
            }
        } finally {
            // Ensure the input stream is closed
            inputStream.close();
        }

        // New line to separate the progress bar from other console output
        System.out.println();
    }

    public static void updateProgressBar(long bytesRead, long totalSize, String fileName) {
        if (totalSize > 0) {
            int progress = (int) ((bytesRead * 100) / totalSize);
            System.out.print("\rProgress (" + fileName + "): " + progress + "%");
        } else {
            System.out.print("\rProgress (" + fileName + "): " + bytesRead + " bytes");
        }
    }

    public static long calculateTotalSize(Path zipFilePath) throws IOException {
        AtomicLong totalSize = new AtomicLong();

        Files.walkFileTree(zipFilePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                totalSize.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });

        return totalSize.get();
    }

}
