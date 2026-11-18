package com.auca.teacher;

import com.auca.abstracts.AcademicMember;
import com.auca.department.Department;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Registrable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public class Teacher extends AcademicMember {
    public static final List<String> EMPLOYMENT_TYPES = List.of("FULL_TIME", "PART_TIME", "CONTRACT");

    private String employeeNumber;
    private String specialization;
    private LocalDate hireDate;
    private BigDecimal salary;
    private String officeNumber;
    private String employmentType;

    public Teacher(String personId, String firstName, String lastName, String email, String phoneNumber,
                   LocalDate dateOfBirth, String memberNumber, Department department, LocalDate joinDate,
                   String academicStatus, String institutionalEmail, String advisorName,
                   String employeeNumber, String specialization, LocalDate hireDate, BigDecimal salary,
                   String officeNumber, String employmentType) {
        super(personId, firstName, lastName, email, phoneNumber, dateOfBirth, memberNumber, department,
                joinDate, academicStatus, institutionalEmail, advisorName);
        this.employeeNumber = employeeNumber;
        this.specialization = specialization;
        this.hireDate = hireDate;
        this.salary = salary;
        this.officeNumber = officeNumber;
        this.employmentType = employmentType;
    }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public String getOfficeNumber() { return officeNumber; }
    public void setOfficeNumber(String officeNumber) { this.officeNumber = officeNumber; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    @Override
    public String getRoleDescription() { return "Teacher specialised in " + specialization; }

    @Override
    public String getAcademicCategory() { return "Faculty"; }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Registrable.requireText("Employee number", employeeNumber, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireText("Specialization", specialization, 2, 100);
        Registrable.requireNotFuture("Hire date", hireDate);
        if (salary == null || salary.signum() <= 0) {
            throw new ValidationException("Salary must be greater than zero.");
        }
        Registrable.requireText("Office number", officeNumber, 1, 20);
        Registrable.requireAllowed("Employment type", employmentType, EMPLOYMENT_TYPES);
    }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.println("--- TEACHER ---");
        System.out.printf("  %-20s: %s%n", "Employee number", employeeNumber);
        System.out.printf("  %-20s: %s%n", "Specialization", specialization);
        System.out.printf("  %-20s: %s%n", "Hire date", hireDate);
        System.out.printf("  %-20s: %s%n", "Salary", salary == null ? "(none)" : salary.toPlainString());
        System.out.printf("  %-20s: %s%n", "Office number", officeNumber);
        System.out.printf("  %-20s: %s%n", "Employment type", employmentType);
    }

    @Override
    public String getRegistrationSummary() {
        return super.getRegistrationSummary() + " | " + employeeNumber;
    }

    @Override
    public String toString() {
        return "Teacher{id=" + getPersonId() + ", employee=" + employeeNumber + ", name=" + getDisplayName()
                + ", specialization=" + specialization + "}";
    }
}
