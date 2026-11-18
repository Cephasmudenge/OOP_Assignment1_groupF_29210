package com.auca.librarybook;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;

public class LibraryBook implements Identifiable, Registrable {
    private final String bookId;
    private String isbn;
    private String title;
    private String author;
    private int totalCopies;
    private int availableCopies;

    public LibraryBook(String bookId, String isbn, String title, String author, int totalCopies, int availableCopies) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public String getBookId() { return bookId; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    @Override
    public String getId() { return bookId; }

    @Override
    public String getDisplayName() { return title; }

    @Override
    public boolean isActive() { return availableCopies > 0; }

    @Override
    public void validate() throws ValidationException {
        Registrable.requireText("Book ID", bookId, MIN_ID_LENGTH, MAX_ID_LENGTH);
        String t = Registrable.requireText("ISBN", isbn, 10, 17).replace("-", "").replace(" ", "");
        if (!t.matches("\\d{9}[\\dXx]|\\d{13}")) {
            throw new ValidationException("ISBN must contain 10 or 13 digits (hyphens allowed).");
        }
        Registrable.requireText("Title", title, 1, 150);
        Registrable.requireText("Author", author, 2, 100);
        if (totalCopies < 0 || availableCopies < 0) {
            throw new ValidationException("Copy counts must not be negative.");
        }
        if (availableCopies > totalCopies) {
            throw new ValidationException("Available copies (" + availableCopies + ") cannot exceed total copies (" + totalCopies + ").");
        }
    }

    @Override
    public void printDetails() {
        System.out.println("--- LIBRARY BOOK ---");
        System.out.printf("  %-20s: %s%n", "Book ID", bookId);
        System.out.printf("  %-20s: %s%n", "ISBN", isbn);
        System.out.printf("  %-20s: %s%n", "Title", title);
        System.out.printf("  %-20s: %s%n", "Author", author);
        System.out.printf("  %-20s: %d%n", "Total copies", totalCopies);
        System.out.printf("  %-20s: %d%n", "Available copies", availableCopies);
    }

    @Override
    public String getRegistrationSummary() {
        return "[" + bookId + "] " + title + " by " + author + " (ISBN " + isbn + ") - " + availableCopies + "/" + totalCopies + " available";
    }

    @Override
    public String toString() {
        return "LibraryBook{id=" + bookId + ", title=" + title + ", available=" + availableCopies + "/" + totalCopies + "}";
    }
}
