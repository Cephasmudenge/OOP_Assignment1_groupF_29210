package com.auca.student;

import com.auca.abstracts.AcademicMember;
import com.auca.department.Department;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Registrable;
import java.time.LocalDate;


public class Student extends AcademicMember {
    public static final int MAX_STUDY_YEAR = 6;

    private String studentNumber;
    private String programName;
    private LocalDate admissionDate;
    private int studyYear;
    private String guardianPhone;
    private boolean scholarshipStatus;

    public Student(String personId, String firstName, String lastName, String email, String phoneNumber,
                   LocalDate dateOfBirth, String memberNumber, Department department, LocalDate joinDate,
                   String academicStatus, String institutionalEmail, String advisorName,
                   String studentNumber, String programName, LocalDate admissionDate, int studyYear,
                   String guardianPhone, boolean scholarshipStatus) {
        super(personId, firstName, lastName, email, phoneNumber, dateOfBirth, memberNumber, department,
                joinDate, academicStatus, institutionalEmail, advisorName); 
        this.studentNumber = studentNumber;
        this.programName = programName;
        this.admissionDate = admissionDate;
        this.studyYear = studyYear;
        this.guardianPhone = guardianPhone;
        this.scholarshipStatus = scholarshipStatus;
    }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }
    public int getStudyYear() { return studyYear; }
    public void setStudyYear(int studyYear) { this.studyYear = studyYear; }
    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }
    public boolean isScholarshipStatus() { return scholarshipStatus; }
    public void setScholarshipStatus(boolean scholarshipStatus) { this.scholarshipStatus = scholarshipStatus; }

    @Override
    public String getRoleDescription() { return "Student of " + programName + " (year " + studyYear + ")"; }

    @Override
    public String getAcademicCategory() { return "Learner"; }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        Registrable.requireText("Student number", studentNumber, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireText("Program name", programName, 2, 120);
        Registrable.requireNotFuture("Admission date", admissionDate);
        Registrable.requireRange("Study year", studyYear, 1, MAX_STUDY_YEAR);
        Registrable.requirePhone("Guardian phone", guardianPhone);
    }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.println("--- STUDENT ---");
        System.out.printf("  %-20s: %s%n", "Student number", studentNumber);
        System.out.printf("  %-20s: %s%n", "Program name", programName);
        System.out.printf("  %-20s: %s%n", "Admission date", admissionDate);
        System.out.printf("  %-20s: %d%n", "Study year", studyYear);
        System.out.printf("  %-20s: %s%n", "Guardian phone", guardianPhone);
        System.out.printf("  %-20s: %s%n", "Scholarship", scholarshipStatus ? "YES" : "NO");
    }

    @Override
    public String getRegistrationSummary() {
        return super.getRegistrationSummary() + " | " + studentNumber;
    }

    @Override
    public String toString() {
        return "Student{id=" + getPersonId() + ", number=" + studentNumber + ", name=" + getDisplayName()
                + ", program=" + programName + ", year=" + studyYear + "}";
    }
}
