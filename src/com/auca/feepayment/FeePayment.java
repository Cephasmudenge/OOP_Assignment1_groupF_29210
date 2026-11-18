package com.auca.feepayment;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.student.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FeePayment implements Identifiable, Registrable {
    public static final List<String> METHODS = List.of("CASH", "MOBILE_MONEY", "BANK_TRANSFER", "CARD");

    private final String paymentId;
    private Student student;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String receiptNumber;

    public FeePayment(String paymentId, Student student, BigDecimal amount, LocalDate paymentDate,
                      String paymentMethod, String receiptNumber) {
        this.paymentId = paymentId;
        this.student = student;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.receiptNumber = receiptNumber;
    }

    public String getPaymentId() { return paymentId; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    @Override
    public String getId() { return paymentId; }

    @Override
    public String getDisplayName() {
        return "Receipt " + receiptNumber + " - " + (amount == null ? "?" : amount.toPlainString());
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireText("Payment ID", paymentId, MIN_ID_LENGTH, MAX_ID_LENGTH);
        Registrable.requireNotNull("Student", student);
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Amount must be strictly positive.");
        }
        Registrable.requireNotFuture("Payment date", paymentDate);
        Registrable.requireAllowed("Payment method", paymentMethod, METHODS);
        Registrable.requireText("Receipt number", receiptNumber, 3, 30);
    }

    @Override
    public void printDetails() {
        System.out.println("--- FEE PAYMENT ---");
        System.out.printf("  %-20s: %s%n", "Payment ID", paymentId);
        System.out.printf("  %-20s: %s%n", "Student", student == null ? "(none)" : student.getDisplayName());
        System.out.printf("  %-20s: %s%n", "Amount", amount == null ? "(none)" : amount.toPlainString());
        System.out.printf("  %-20s: %s%n", "Payment date", paymentDate);
        System.out.printf("  %-20s: %s%n", "Payment method", paymentMethod);
        System.out.printf("  %-20s: %s%n", "Receipt number", receiptNumber);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + paymentId + "] " + (student == null ? "?" : student.getDisplayName()) + " paid "
                + (amount == null ? "?" : amount.toPlainString()) + " by " + paymentMethod + " on " + paymentDate
                + " (receipt " + receiptNumber + ")";
    }

    @Override
    public String toString() {
        return "FeePayment{id=" + paymentId + ", amount=" + amount + ", receipt=" + receiptNumber + "}";
    }
}
