package com.auca.assessment;

import com.auca.enrollment.Enrollment;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import java.time.LocalDate;
import java.util.List;

public class Assessment implements Identifiable, Registrable {
    public static final List<String> TYPES = List.of("QUIZ", "ASSIGNMENT", "MIDTERM", "FINAL_EXAM", "PROJECT");

    private final String assessmentId;
    private Enrollment enrollment;
    private String assessmentType;
    private double maximumScore;
    private double obtainedScore;
    private LocalDate assessmentDate;

    public Assessment(String assessmentId, Enrollment enrollment, String assessmentType,
                      double maximumScore, double obtainedScore, LocalDate assessmentDate) {
        this.assessmentId = assessmentId;
        this.enrollment = enrollment;
        this.assessmentType = assessmentType;
        this.maximumScore = maximumScore;
        this.obtainedScore = obtainedScore;
        this.assessmentDate = assessmentDate;
    }

    public String getAssessmentId() { return assessmentId; }
    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }
    public String getAssessmentType() { return assessmentType; }
    public void setAssessmentType(String assessmentType) { this.assessmentType = assessmentType; }
    public double getMaximumScore() { return maximumScore; }
    public void setMaximumScore(double maximumScore) { this.maximumScore = maximumScore; }
    public double getObtainedScore() { return obtainedScore; }
    public void setObtainedScore(double obtainedScore) { this.obtainedScore = obtainedScore; }
    public LocalDate getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDate assessmentDate) { this.assessmentDate = assessmentDate; }

    @Override
    public String getId() { return assessmentId; }

    @Override
    public String getDisplayName() {
        return assessmentType + " " + obtainedScore + "/" + maximumScore
                + (enrollment == null ? "" : " - " + enrollment.getDisplayName());
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireText("Assessment ID", assessmentId, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireNotNull("Enrollment", enrollment);
        Registrable.requireAllowed("Assessment type", assessmentType, TYPES);
        if (!(maximumScore > 0)) {
            throw new ValidationException("Maximum score must be greater than zero.");
        }
        if (obtainedScore < 0 || obtainedScore > maximumScore) {
            throw new ValidationException("Obtained score must be between 0 and " + maximumScore + ".");
        }
        Registrable.requireNotFuture("Assessment date", assessmentDate);
    }

    @Override
    public void printDetails() {
        System.out.println("--- ASSESSMENT ---");
        System.out.printf("  %-20s: %s%n", "Assessment ID", assessmentId);
        System.out.printf("  %-20s: %s%n", "Enrollment", enrollment == null ? "(none)" : enrollment.getDisplayName());
        System.out.printf("  %-20s: %s%n", "Type", assessmentType);
        System.out.printf("  %-20s: %.2f%n", "Maximum score", maximumScore);
        System.out.printf("  %-20s: %.2f%n", "Obtained score", obtainedScore);
        System.out.printf("  %-20s: %s%n", "Date", assessmentDate);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + assessmentId + "] " + getDisplayName() + " on " + assessmentDate;
    }

    @Override
    public String toString() {
        return "Assessment{id=" + assessmentId + ", type=" + assessmentType + ", score=" + obtainedScore + "/" + maximumScore + "}";
    }
}
