package com.abc.emergency;

import com.abc.emergency.util.FileUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilTest {

    @Test
    void testCSVWriteAndRead() throws Exception {

        Path file = Files.createTempFile("ambulance_test", ".csv");

        List<String[]> records = new ArrayList<>();

        records.add(new String[]{"101", "Rahul", "CRITICAL"});
        records.add(new String[]{"102", "Priya", "MODERATE"});

        FileUtil.writeCSV(file.toString(), records);

        List<String[]> result = FileUtil.readCSV(file.toString());

        assertEquals(2, result.size());
        assertEquals("101", result.get(0)[0]);
        assertEquals("Rahul", result.get(0)[1]);
        assertEquals("CRITICAL", result.get(0)[2]);

        Files.deleteIfExists(file);
    }

    @Test
    void testTextFileWriteAndRead() throws Exception {

        Path file = Files.createTempFile("ambulance_text_test", ".txt");

        String content = "Emergency Case Report";

        FileUtil.writeTextFile(file.toString(), content);

        String result = FileUtil.readTextFile(file.toString());

        assertEquals(content + System.lineSeparator(), result);

        Files.deleteIfExists(file);
    }

    @Test
    void testAppendToFile() throws Exception {

        Path file = Files.createTempFile("ambulance_append_test", ".txt");

        FileUtil.writeTextFile(file.toString(), "First Entry");
        FileUtil.appendToFile(file.toString(), "Second Entry");

        String result = FileUtil.readTextFile(file.toString());

        assertTrue(result.contains("First Entry"));
        assertTrue(result.contains("Second Entry"));

        Files.deleteIfExists(file);
    }
}