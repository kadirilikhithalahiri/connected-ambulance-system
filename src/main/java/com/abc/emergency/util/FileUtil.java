package com.abc.emergency.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    public static List<String[]> readCSV(String filePath) throws IOException {

        List<String[]> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (!line.trim().isEmpty()) {
                    records.add(line.split(",", -1));
                }
            }
        }

        return records;
    }

    public static void writeCSV(String filePath, List<String[]> records) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            for (String[] record : records) {

                writer.write(String.join(",", record));
                writer.newLine();
            }
        }
    }

    public static void appendToFile(String filePath, String content) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {

            writer.write(content);
            writer.newLine();
        }
    }

    public static String readTextFile(String filePath) throws IOException {

        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        }

        return content.toString();
    }

    public static void writeTextFile(String filePath, String content) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        }
    }
}