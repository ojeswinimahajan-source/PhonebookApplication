package com.phonebook.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.phonebook.model.User;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<User> getUserForLogin(String username) {

        String sql = "{call dbo.sp_GetUserForLogin(?)}";

        return jdbcTemplate.query(
            sql,
            ps -> ps.setString(1, username),
            rs -> {
                if (!rs.next()) {
                    return Optional.empty();
                }

                User user = new User();
                user.setId(rs.getInt("Id"));
                user.setUsername(rs.getString("Username"));
                user.setPasswordHash(rs.getString("PasswordHash"));

                return Optional.of(user);
            }
        );
    }

    @Override
    public boolean insertUserIfNotExists(User user) {

        String sql = """
            EXEC dbo.sp_InsertUserIfNotExists
                @Username = ?,
                @PasswordHash = ?
            """;

        try {
            Integer result = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                user.getUsername(),
                user.getPasswordHash()
            );

            return result != null && result > 0;

        } catch (DataAccessException ex) {
            if (isDuplicateKeyError(ex)) {
                return false;
            }

            throw ex;
        }
    }

    private boolean isDuplicateKeyError(Throwable exception) {

        Throwable current = exception;

        while (current != null) {
            if (current instanceof SQLException sqlException) {
                int errorCode = sqlException.getErrorCode();

                if (errorCode == 2601 || errorCode == 2627) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }
}