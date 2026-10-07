package com.phonebook.repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;

import com.phonebook.model.Contact;
import com.phonebook.model.PagedResult;
import com.phonebook.exception.DuplicatePhoneException;

import com.phonebook.model.ContactExportRecord;

@Repository
public class ContactRepositoryImpl implements ContactRepository {

    private final JdbcTemplate jdbcTemplate;

    public ContactRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PagedResult getContactsPaged(
            int pageNumber,
            int pageSize,
            String searchTerm) {

        return jdbcTemplate.execute((ConnectionCallback<PagedResult>) connection -> {
            try (CallableStatement statement =
                    connection.prepareCall("{call dbo.sp_GetContactsPaged(?,?,?)}")) {

                statement.setInt(1, pageNumber);
                statement.setInt(2, pageSize);

                if (searchTerm == null || searchTerm.isBlank()) {
                    statement.setNull(3, Types.NVARCHAR);
                } else {
                    statement.setString(3, searchTerm);
                }

                ResultSet contactsResult = executeAndGetResultSet(statement);

                if (contactsResult == null) {
                    throw new SQLException(
                            "sp_GetContactsPaged did not return the contacts result set.");
                }

                List<Contact> contacts = new ArrayList<>();

                try (ResultSet rs = contactsResult) {
                    while (rs.next()) {
                        contacts.add(mapContact(rs, true));
                    }
                }

                ResultSet countResult = getNextResultSet(statement);

                if (countResult == null) {
                    throw new SQLException(
                            "sp_GetContactsPaged did not return the total count result set.");
                }

                int totalCount;

                try (ResultSet rs = countResult) {
                    totalCount = rs.next() ? rs.getInt("TotalCount") : 0;
                }

                return new PagedResult(
                        contacts,
                        totalCount,
                        pageNumber,
                        pageSize
                );
            }
        });
    }

    @Override
    public List<ContactExportRecord> getContactsForExport() {
        return jdbcTemplate.execute((ConnectionCallback<List<ContactExportRecord>>) connection -> {
            try (CallableStatement statement =
                    connection.prepareCall("{call dbo.sp_GetContactsForExport}")) {

                ResultSet result = executeAndGetResultSet(statement);
                List<ContactExportRecord> contacts = new ArrayList<>();

                if (result != null) {
                    try (ResultSet rs = result) {
                        while (rs.next()) {
                            contacts.add(new ContactExportRecord(
                                    rs.getInt("Id"),
                                    rs.getString("Name"),
                                    rs.getString("PhoneNumber"),
                                    rs.getString("Email"),
                                    rs.getString("Address")
                            ));
                        }
                    }
                }

                return contacts;
            }
        });
    }

    @Override
    public Optional<Contact> getContactById(int id) {
        return jdbcTemplate.execute((ConnectionCallback<Optional<Contact>>) connection -> {
            try (CallableStatement statement =
                    connection.prepareCall("{call dbo.sp_GetContactById(?)}")) {

                statement.setInt(1, id);

                ResultSet result = executeAndGetResultSet(statement);

                if (result == null) {
                    return Optional.empty();
                }

                try (ResultSet rs = result) {
                    if (rs.next()) {
                        return Optional.of(mapContact(rs, true));
                    }
                }

                return Optional.empty();
            }
        });
    }

    @Override
    public int insertContact(Contact contact) {
        try {
            return jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
                try (CallableStatement statement =
                        connection.prepareCall("{call dbo.sp_InsertContact(?,?,?,?)}")) {

                    statement.setString(1, contact.getName());
                    statement.setString(2, contact.getPhoneNumber());
                    setNullableString(statement, 3, contact.getEmail());
                    setNullableString(statement, 4, contact.getAddress());

                    ResultSet result = executeAndGetResultSet(statement);

                    if (result == null) {
                        throw new SQLException(
                                "sp_InsertContact did not return the new contact ID.");
                    }

                    try (ResultSet rs = result) {
                        if (rs.next()) {
                            return rs.getInt("Id");
                        }
                    }

                    throw new SQLException(
                            "sp_InsertContact returned no contact ID.");
                }
            });
        } catch (DataAccessException ex) {
            rethrowIfDuplicatePhone(ex);
            throw ex;
        }
    }

    @Override
    public boolean updateContact(int id, Contact contact) {
        try {
            return jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
                try (CallableStatement statement =
                        connection.prepareCall("{call dbo.sp_UpdateContact(?,?,?,?,?)}")) {

                    statement.setInt(1, id);
                    statement.setString(2, contact.getName());
                    statement.setString(3, contact.getPhoneNumber());
                    setNullableString(statement, 4, contact.getEmail());
                    setNullableString(statement, 5, contact.getAddress());

                    ResultSet result = executeAndGetResultSet(statement);

                    if (result == null) {
                        return false;
                    }

                    try (ResultSet rs = result) {
                        return rs.next() && rs.getInt("RowsAffected") > 0;
                    }
                }
            });
        } catch (DataAccessException ex) {
            rethrowIfDuplicatePhone(ex);
            throw ex;
        }
    }

    @Override
    public boolean deleteContact(int id) {
        return jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (CallableStatement statement =
                    connection.prepareCall("{call dbo.sp_DeleteContact(?)}")) {

                statement.setInt(1, id);

                ResultSet result = executeAndGetResultSet(statement);

                if (result == null) {
                    return false;
                }

                try (ResultSet rs = result) {
                    return rs.next() && rs.getInt("RowsAffected") > 0;
                }
            }
        });
    }

    private static ResultSet executeAndGetResultSet(
            CallableStatement statement) throws SQLException {

        boolean hasResultSet = statement.execute();

        while (!hasResultSet && statement.getUpdateCount() != -1) {
            hasResultSet = statement.getMoreResults();
        }

        return hasResultSet ? statement.getResultSet() : null;
    }

    private static ResultSet getNextResultSet(
            CallableStatement statement) throws SQLException {

        boolean hasResultSet = statement.getMoreResults();

        while (!hasResultSet && statement.getUpdateCount() != -1) {
            hasResultSet = statement.getMoreResults();
        }

        return hasResultSet ? statement.getResultSet() : null;
    }

    private static Contact mapContact(
            ResultSet rs,
            boolean includeCreatedAt) throws SQLException {

        Contact contact = new Contact();

        contact.setId(rs.getInt("Id"));
        contact.setName(rs.getString("Name"));
        contact.setPhoneNumber(rs.getString("PhoneNumber"));
        contact.setEmail(rs.getString("Email"));
        contact.setAddress(rs.getString("Address"));

        if (includeCreatedAt) {
            Timestamp createdAt = rs.getTimestamp("CreatedAt");

            if (createdAt != null) {
                contact.setCreatedAt(createdAt.toLocalDateTime());
            }
        }

        return contact;
    }

    private static void setNullableString(
            CallableStatement statement,
            int parameterIndex,
            String value) throws SQLException {

        if (value == null) {
            statement.setNull(parameterIndex, Types.NVARCHAR);
        } else {
            statement.setString(parameterIndex, value);
        }
    }
    
    private static void rethrowIfDuplicatePhone(DataAccessException exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                int errorCode = sqlException.getErrorCode();

                if (errorCode == 2601 || errorCode == 2627) {
                    throw new DuplicatePhoneException();
                }
            }

            cause = cause.getCause();
        }
    }
}