package com.auca.abstracts;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import java.time.LocalDate;


public abstract class Person implements Identifiable, Registrable {
    private final String personId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    protected Person(String personId, String firstName, String lastName, String email,
                     String phoneNumber, LocalDate dateOfBirth) {
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
    }

    public String getPersonId() { return personId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    
    public abstract String getRoleDescription();

    @Override
    public String getId() { return personId; }

    @Override
    public String getDisplayName() { return firstName + " " + lastName; }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireText("Person ID", personId, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireText("First name", firstName, MIN_NAME_LENGTH, MAX_NAME_LENGTH);
        Registrable.requireText("Last name", lastName, MIN_NAME_LENGTH, MAX_NAME_LENGTH);
        Registrable.requireEmail("Email", email);
        Registrable.requirePhone("Phone", phoneNumber);
        Registrable.requirePast("Date of birth", dateOfBirth);
    }

    @Override
    public void printDetails() {
        System.out.println("--- PERSON ---");
        System.out.printf("  %-20s: %s%n", "Person ID", personId);
        System.out.printf("  %-20s: %s%n", "First name", firstName);
        System.out.printf("  %-20s: %s%n", "Last name", lastName);
        System.out.printf("  %-20s: %s%n", "Email", email);
        System.out.printf("  %-20s: %s%n", "Phone", phoneNumber);
        System.out.printf("  %-20s: %s%n", "Date of birth", dateOfBirth);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + personId + "] " + getDisplayName() + " - " + getRoleDescription();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + personId + ", name=" + getDisplayName() + ", email=" + email + "}";
    }
}
