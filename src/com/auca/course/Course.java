package com.auca.course;

import com.auca.department.Department;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.teacher.Teacher;

public class Course implements Identifiable, Registrable {
    private final String courseId;
    private String courseCode;
    private String courseTitle;
    private int creditHours;
    private Department department;
    private Teacher assignedTeacher;

    public Course(String courseId, String courseCode, String courseTitle, int creditHours,
                  Department department, Teacher assignedTeacher) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.creditHours = creditHours;
        this.department = department;
        this.assignedTeacher = assignedTeacher;
    }

    public String getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public int getCreditHours() { return creditHours; }
    public void setCreditHours(int creditHours) { this.creditHours = creditHours; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Teacher getAssignedTeacher() { return assignedTeacher; }
    public void setAssignedTeacher(Teacher assignedTeacher) { this.assignedTeacher = assignedTeacher; }

    @Override
    public String getId() { return courseId; }

    @Override
    public String getDisplayName() { return courseCode + " - " + courseTitle; }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireText("Course ID", courseId, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireText("Course code", courseCode, 3, 12);
        Registrable.requireText("Course title", courseTitle, 2, 100);
        Registrable.requireRange("Credit hours", creditHours, 1, 6);
        Registrable.requireNotNull("Department", department);
        Registrable.requireNotNull("Assigned teacher", assignedTeacher);
    }

    @Override
    public void printDetails() {
        System.out.println("--- COURSE ---");
        System.out.printf("  %-20s: %s%n", "Course ID", courseId);
        System.out.printf("  %-20s: %s%n", "Course code", courseCode);
        System.out.printf("  %-20s: %s%n", "Course title", courseTitle);
        System.out.printf("  %-20s: %d%n", "Credit hours", creditHours);
        System.out.printf("  %-20s: %s%n", "Department", department == null ? "(none)" : department.getDisplayName());
        System.out.printf("  %-20s: %s%n", "Assigned teacher", assignedTeacher == null ? "(none)" : assignedTeacher.getDisplayName());
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + courseId + "] " + courseCode + " " + courseTitle + " (" + creditHours + " cr) taught by "
                + (assignedTeacher == null ? "-" : assignedTeacher.getDisplayName());
    }

    @Override
    public String toString() {
        return "Course{id=" + courseId + ", code=" + courseCode + ", title=" + courseTitle + ", credits=" + creditHours + "}";
    }
}
