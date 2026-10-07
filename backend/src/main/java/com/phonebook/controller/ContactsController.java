package com.phonebook.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.phonebook.model.Contact;
import com.phonebook.model.PagedResult;
import com.phonebook.service.ContactService;

import jakarta.validation.Valid;


import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.phonebook.model.ContactExportRecord;
import com.phonebook.service.ContactExportService;

@RestController
@RequestMapping("/api/contacts")
public class ContactsController {

    private final ContactService contactService;
    
    private final ContactExportService contactExportService;

    public ContactsController(
            ContactService contactService,
            ContactExportService contactExportService) {

        this.contactService = contactService;
        this.contactExportService = contactExportService;
    }
    
    
    @GetMapping
    public ResponseEntity<?> getContacts(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String searchTerm) {

        if (pageNumber < 1) {
            return ResponseEntity.badRequest()
                    .body("Page number must be greater than or equal to 1.");
        }

        if (pageSize < 1) {
            return ResponseEntity.badRequest()
                    .body("Page size must be greater than or equal to 1.");
        }

        PagedResult result = contactService.getContactsPaged(
                pageNumber,
                pageSize,
                searchTerm
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getContactById(@PathVariable int id) {
        return contactService.getContactById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createContact(@Valid @RequestBody Contact contact) {
        int newId = contactService.createContact(contact);

        return contactService.getContactById(newId)
                .<ResponseEntity<?>>map(createdContact ->
                        ResponseEntity
                                .created(URI.create("/api/contacts/" + newId))
                                .body(createdContact))
                .orElseGet(() -> ResponseEntity.internalServerError().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContact(
            @PathVariable int id,
            @Valid @RequestBody Contact contact) {

        boolean updated = contactService.updateContact(id, contact);

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable int id) {
        boolean deleted = contactService.deleteContact(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
    
    
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        List<ContactExportRecord> contacts =
                contactService.getContactsForExport();

        byte[] fileContent = contactExportService.createCsv(contacts);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"contacts.csv\""
                )
                .body(fileContent);
    }

    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportJson() {
        List<ContactExportRecord> contacts =
                contactService.getContactsForExport();

        byte[] fileContent = contactExportService.createJson(contacts);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/json; charset=utf-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"contacts.json\""
                )
                .body(fileContent);
    }
    
}