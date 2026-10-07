package com.phonebook.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.phonebook.model.Contact;
import com.phonebook.model.PagedResult;
import com.phonebook.repository.ContactRepository;

import com.phonebook.model.ContactExportRecord;

@Service
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public PagedResult getContactsPaged(
            int pageNumber,
            int pageSize,
            String searchTerm) {

        return contactRepository.getContactsPaged(
                pageNumber,
                pageSize,
                searchTerm
        );
    }

    public List<ContactExportRecord> getContactsForExport() {
        return contactRepository.getContactsForExport();
    }

    public Optional<Contact> getContactById(int id) {
        return contactRepository.getContactById(id);
    }

    public int createContact(Contact contact) {
        return contactRepository.insertContact(contact);
    }

    public boolean updateContact(int id, Contact contact) {
        return contactRepository.updateContact(id, contact);
    }

    public boolean deleteContact(int id) {
        return contactRepository.deleteContact(id);
    }
}