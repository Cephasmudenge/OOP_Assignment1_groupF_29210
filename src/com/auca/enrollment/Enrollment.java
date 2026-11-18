package com.auca.enrollment;

import com.auca.course.Course;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.student.Student;
import java.time.LocalDate;
import java.util.List;

public class Enrollment implements Identifiable, Registrable {
    public static final List<String> SEMESTERS = List.of("FALL", "SPRING", "SUMMER");
    public static final List<String> STATUSES = List.of("ACTIVE", "COMPLETED", "DROPPED");

    private final String enrollmentId;
    private Student student;
    private Course course;
    private LocalDate enrollmentDate;
    private String semester;
    private String enrollmentStatus;

    public Enrollment(String enrollmentId, Student student, Course course, LocalDate enrollmentDate,
                      String semester, String enrollmentStatus) {
        this.enrollmentId = enrollmentId;
        this.student = student;
        this.course = course;
        this.enrollmentDate = enrollmentDate;
        this.semester = semester;
        this.enrollmentStatus = enrollmentStatus;
    }

    public String getEnrollmentId() { return enrollmentId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }

    @Override
    public String getId() { return enrollmentId; }

    @Override
    public String getDisplayName() {
        return (student == null ? "?" : student.getDisplayName()) + " in "
                + (course == null ? "?" : course.getCourseCode()) + " (" + semester + ")";
    }

    @Override
    public boolean isActive() { return "ACTIVE".equals(enrollmentStatus); }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireText("Enrollment ID", enrollmentId, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireNotNull("Student", student);
        Registrable.requireNotNull("Course", course);
        Registrable.requireNotFuture("Enrollment date", enrollmentDate);
        Registrable.requireAllowed("Semester", semester, SEMESTERS);
        Registrable.requireAllowed("Enrollment status", enrollmentStatus, STATUSES);
    }

    @Override
    public void printDetails() {
        System.out.println("--- ENROLLMENT ---");
        System.out.printf("  %-20s: %s%n", "Enrollment ID", enrollmentId);
        System.out.printf("  %-20s: %s%n", "Student", student == null ? "(none)" : student.getDisplayName());
        System.out.printf("  %-20s: %s%n", "Course", course == null ? "(none)" : course.getDisplayName());
        System.out.printf("  %-20s: %s%n", "Enrollment date", enrollmentDate);
        System.out.printf("  %-20s: %s%n", "Semester", semester);
        System.out.printf("  %-20s: %s%n", "Status", enrollmentStatus);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + enrollmentId + "] " + getDisplayName() + " - " + enrollmentStatus;
    }

    @Override
    public String toString() {
        return "Enrollment{id=" + enrollmentId + ", " + getDisplayName() + ", status=" + enrollmentStatus + "}";
    }
}
