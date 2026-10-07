package com.phonebook.repository;

import java.util.List;
import java.util.Optional;

import com.phonebook.model.Contact;
import com.phonebook.model.PagedResult;

import com.phonebook.model.ContactExportRecord;

public interface ContactRepository {

    PagedResult getContactsPaged(
            int pageNumber,
            int pageSize,
            String searchTerm
    );

    List<ContactExportRecord> getContactsForExport();

    Optional<Contact> getContactById(int id);

    int insertContact(Contact contact);

    boolean updateContact(int id, Contact contact);

    boolean deleteContact(int id);
}