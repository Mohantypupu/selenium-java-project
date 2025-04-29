
package com.example.acknowledgment;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EmailListProvider {
    private final String filePath;

    public EmailListProvider(String filePath) {
        this.filePath = filePath;
    }

    public List<String> getEmails() throws IOException {
        List<String> emails = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                emails.add(line.trim());
            }
        }
        return emails;
    }
}
