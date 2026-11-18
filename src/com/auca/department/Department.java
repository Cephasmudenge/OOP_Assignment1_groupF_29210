package com.auca.department;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.teacher.Teacher;

public class Department implements Identifiable, Registrable {
    private final String departmentId;
    private String departmentName;
    private String departmentCode;
    private String officeLocation;
    private String contactEmail;
    private Teacher headTeacher; 

    public Department(String departmentId, String departmentName, String departmentCode,
                      String officeLocation, String contactEmail, Teacher headTeacher) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.officeLocation = officeLocation;
        this.contactEmail = contactEmail;
        this.headTeacher = headTeacher;
    }

    public String getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public Teacher getHeadTeacher() { return headTeacher; }
    public void setHeadTeacher(Teacher headTeacher) { this.headTeacher = headTeacher; }

    @Override
    public String getId() { return departmentId; }

    @Override
    public String getDisplayName() { return departmentName; }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireText("Department ID", departmentId, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireText("Department name", departmentName, MIN_NAME_LENGTH, MAX_NAME_LENGTH);
        Registrable.requireText("Department code", departmentCode, 2, 10);
        Registrable.requireText("Office location", officeLocation, 2, 60);
        Registrable.requireEmail("Contact email", contactEmail);
    }

    @Override
    public void printDetails() {
        System.out.println("--- DEPARTMENT ---");
        System.out.printf("  %-20s: %s%n", "Department ID", departmentId);
        System.out.printf("  %-20s: %s%n", "Department name", departmentName);
        System.out.printf("  %-20s: %s%n", "Department code", departmentCode);
        System.out.printf("  %-20s: %s%n", "Office location", officeLocation);
        System.out.printf("  %-20s: %s%n", "Contact email", contactEmail);
        System.out.printf("  %-20s: %s%n", "Head teacher", headTeacher == null ? "(not assigned)" : headTeacher.getDisplayName());
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + departmentId + "] " + departmentName + " (" + departmentCode + ") head: "
                + (headTeacher == null ? "-" : headTeacher.getDisplayName());
    }

    @Override
    public String toString() {
        return "Department{id=" + departmentId + ", name=" + departmentName + ", code=" + departmentCode + "}";
    }
}
