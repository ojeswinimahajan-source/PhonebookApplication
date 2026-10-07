package com.phonebook.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebook.model.ContactExportRecord;

@Service
public class ContactExportService {

    private final ObjectMapper objectMapper;

    public ContactExportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] createCsv(List<ContactExportRecord> contacts) {
        StringBuilder csv = new StringBuilder();

        csv.append("Id,Name,PhoneNumber,Email,Address\r\n");

        for (ContactExportRecord contact : contacts) {
            csv.append(escapeCsv(String.valueOf(contact.getId()))).append(',');
            csv.append(escapeCsv(contact.getName())).append(',');
            csv.append(escapeCsv(contact.getPhoneNumber())).append(',');
            csv.append(escapeCsv(contact.getEmail())).append(',');
            csv.append(escapeCsv(contact.getAddress())).append("\r\n");
        }

        // UTF-8 without a byte-order mark (BOM), matching the .NET implementation.
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] createJson(List<ContactExportRecord> contacts) {
        try {
            // Jackson uses the Java bean property names, e.g. phoneNumber.
            return objectMapper.writeValueAsBytes(contacts);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to generate contacts JSON export.",
                    exception
            );
        }
    }

    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        boolean requiresQuotes =
                value.contains(",")
                || value.contains("\"")
                || value.contains("\r")
                || value.contains("\n");

        if (!requiresQuotes) {
            return value;
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}