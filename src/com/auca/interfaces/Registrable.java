package com.auca.interfaces;

import com.auca.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;


public interface Registrable {
    int MIN_NAME_LENGTH = 2;
    int MAX_NAME_LENGTH = 60;
    int MAX_EMAIL_LENGTH = 100;
    int MIN_PHONE_LENGTH = 10;
    int MAX_PHONE_LENGTH = 15;
    String DATE_PATTERN = "yyyy-MM-dd";

    void validate() throws ValidationException;

    void printDetails();

    String getRegistrationSummary();

    
    static String requireText(String label, String value, int min, int max) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(label + " must not be blank.");
        }
        String t = value.trim();
        if (t.length() < min || t.length() > max) {
            throw new ValidationException(label + " must be " + min + "-" + max + " characters long.");
        }
        return t;
    }

    static String requireEmail(String label, String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(label + " must not be blank.");
        }
        String t = value.trim();
        if (t.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException(label + " must not exceed " + MAX_EMAIL_LENGTH + " characters.");
        }
        if (!t.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")) {
            throw new ValidationException(label + " is not a valid email address (example: name@auca.ac.rw).");
        }
        return t;
    }

    static String requirePhone(String label, String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(label + " must not be blank.");
        }
        String t = value.trim();
        if (!t.matches("\\+?\\d{" + MIN_PHONE_LENGTH + "," + MAX_PHONE_LENGTH + "}")) {
            throw new ValidationException(label + " must contain " + MIN_PHONE_LENGTH + "-" + MAX_PHONE_LENGTH
                    + " digits with an optional leading + (no spaces).");
        }
        return t;
    }

    static LocalDate parseDate(String label, String text) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException(label + " must not be blank. Use YYYY-MM-DD (example: 2004-05-16).");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date: use YYYY-MM-DD (example: 2004-05-16).");
        }
    }

    static void requirePast(String label, LocalDate d) throws ValidationException {
        requireNotNull(label, d);
        if (!d.isBefore(LocalDate.now())) {
            throw new ValidationException(label + " must be in the past.");
        }
    }

    static void requireNotFuture(String label, LocalDate d) throws ValidationException {
        requireNotNull(label, d);
        if (d.isAfter(LocalDate.now())) {
            throw new ValidationException(label + " must not be in the future.");
        }
    }

    static void requireNotNull(String label, Object o) throws ValidationException {
        if (o == null) {
            throw new ValidationException(label + " is required.");
        }
    }

    static void requireRange(String label, long v, long min, long max) throws ValidationException {
        if (v < min || v > max) {
            throw new ValidationException(label + " must be between " + min + " and " + max + ".");
        }
    }

    static void requireAllowed(String label, String value, java.util.List<String> allowed) throws ValidationException {
        if (value == null || !allowed.contains(value)) {
            throw new ValidationException(label + " must be one of " + allowed + ".");
        }
    }
}
