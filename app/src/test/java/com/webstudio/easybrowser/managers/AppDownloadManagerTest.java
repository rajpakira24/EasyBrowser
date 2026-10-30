package com.webstudio.easybrowser.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

public class AppDownloadManagerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void queuedDownloadsReserveDifferentFilesBeforeWriting() throws IOException {
        File directory = temporaryFolder.newFolder();
        File first = AppDownloadManager.createUniqueFileInDirectory(directory, "report.pdf");
        File second = AppDownloadManager.createUniqueFileInDirectory(directory, "report.pdf");

        assertTrue(first.isFile());
        assertTrue(second.isFile());
        assertEquals("report.pdf", first.getName());
        assertEquals("report (1).pdf", second.getName());
        assertNotEquals(first, second);
    }

    @Test
    public void concurrentDownloadsReserveDifferentFiles() throws Exception {
        File directory = temporaryFolder.newFolder();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Future<File>> reservations = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                reservations.add(executor.submit(() ->
                        AppDownloadManager.createUniqueFileInDirectory(directory, "report.pdf")));
            }
            Set<File> files = new HashSet<>();
            for (Future<File> reservation : reservations) {
                File file = reservation.get();
                assertTrue(file.isFile());
                assertTrue(files.add(file));
            }
            assertEquals(12, files.size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void reservationFailsWhenDestinationIsNotADirectory() throws IOException {
        File directory = temporaryFolder.newFile();
        assertThrows(IOException.class, () ->
                AppDownloadManager.createUniqueFileInDirectory(directory, "report.pdf"));
    }

    @Test
    public void matchingPartialResponseAppendsToSavedBytes() throws IOException {
        assertTrue(AppDownloadManager.shouldAppendResponse(true, 500, 206, "bytes 500-999/1000"));
    }

    @Test
    public void fullResponseRestartsInsteadOfAppending() throws IOException {
        assertFalse(AppDownloadManager.shouldAppendResponse(true, 500, 200, null));
        assertFalse(AppDownloadManager.shouldAppendResponse(false, 0, 200, null));
    }

    @Test
    public void mismatchedPartialResponseIsRejectedBeforeWriting() {
        assertThrows(IOException.class, () ->
                AppDownloadManager.shouldAppendResponse(true, 500, 206, "bytes 200-999/1000"));
        assertThrows(IOException.class, () ->
                AppDownloadManager.shouldAppendResponse(true, 500, 206, null));
        assertThrows(IOException.class, () ->
                AppDownloadManager.shouldAppendResponse(false, 0, 206, "bytes 200-999/1000"));
    }
}
