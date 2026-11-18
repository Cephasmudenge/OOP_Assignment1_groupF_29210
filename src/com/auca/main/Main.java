package com.auca.main;

import com.auca.abstracts.AcademicMember;
import com.auca.abstracts.Person;
import com.auca.administrator.Administrator;
import com.auca.assessment.Assessment;
import com.auca.bookloan.BookLoan;
import com.auca.classroom.Classroom;
import com.auca.course.Course;
import com.auca.department.Department;
import com.auca.enrollment.Enrollment;
import com.auca.exceptions.BookUnavailableException;
import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.RecordNotFoundException;
import com.auca.exceptions.ValidationException;
import com.auca.feepayment.FeePayment;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.librarybook.LibraryBook;
import com.auca.student.Student;
import com.auca.teacher.Teacher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class Main {

    
    private static final Scanner SC = new Scanner(System.in); 
    private static final List<Department> departments = new ArrayList<>();
    private static final List<Student> students = new ArrayList<>();
    private static final List<Teacher> teachers = new ArrayList<>();
    private static final List<Administrator> administrators = new ArrayList<>();
    private static final List<Course> courses = new ArrayList<>();
    private static final List<Classroom> classrooms = new ArrayList<>();
    private static final List<Enrollment> enrollments = new ArrayList<>();
    private static final List<Assessment> assessments = new ArrayList<>();
    private static final List<FeePayment> payments = new ArrayList<>();
    private static final List<LibraryBook> books = new ArrayList<>();
    private static final List<BookLoan> loans = new ArrayList<>();

    private static final int ID_MIN = Identifiable.MIN_ID_LENGTH;
    private static final int ID_MAX = Identifiable.MAX_ID_LENGTH;
    private static final int MAX_DEPTH = 4; 
    private static int depth = 0;

    private static final Object BACK = new Object();
    private static final Object CANCEL = new Object();
    private static final int OK_ = 0, BACK_ = 1, CANCEL_ = 2;
    private static final String LINE = "+------------------------------------------------------------+";

   
    private static final class InputClosed extends RuntimeException {
        private static final long serialVersionUID = 1L;

        InputClosed() { super("Input stream closed"); }
    }

    @FunctionalInterface
    private interface Parser {
        Object parse(String s, Object[] v) throws ValidationException, DuplicateRecordException;
    }

    @FunctionalInterface
    private interface Chooser {
        Object choose() throws PrerequisiteMissingException;
    }

    @FunctionalInterface
    private interface Checker<T> {
        void check(T t) throws Exception;
    }

    
    private static final class Field<E> {
        final int step;
        final String label;
        final String hint;
        Parser parser;
        Chooser chooser;
        boolean derived;
        Function<E, Object> getter;
        BiConsumer<E, Object> setter;
        Object value;

        Field(int step, String label, String hint) {
            this.step = step;
            this.label = label;
            this.hint = hint;
        }

        Field<E> parse(Parser p) { parser = p; return this; }

        Field<E> choose(Chooser c) { chooser = c; return this; }

        Field<E> derive() { derived = true; return this; }

        Field<E> bind(Function<E, Object> g, BiConsumer<E, Object> s) { getter = g; setter = s; return this; }

        
        int fill(int no, Object[] v) throws PrerequisiteMissingException {
            if (chooser != null) {
                System.out.printf("%02d %-20s [%s]%n", no, label, hint);
                Object r = chooser.choose();
                if (r == CANCEL) return CANCEL_;
                if (r == BACK) return BACK_;
                value = r;
                return OK_;
            }
            while (true) {
                String s = prompt(String.format("%02d %-20s [%s] : ", no, label, hint));
                if (s.equalsIgnoreCase("CANCEL")) return CANCEL_;
                if (s.equalsIgnoreCase("BACK")) return BACK_;
                try {
                    value = parser.parse(s, v);
                    return OK_;
                } catch (ValidationException | DuplicateRecordException e) {
                    System.out.println("! " + e.getMessage());
                    System.out.printf("  Please re-enter field %02d.%n", no);
                }
            }
        }
    }

    
    private static final class Spec<E extends Identifiable & Registrable> {
        final String noun;
        final String[] steps;
        final List<Field<E>> fields = new ArrayList<>();
        final List<E> store;
        Function<Object[], E> factory;
        Checker<E> checker = e -> { };
        Consumer<E> after = e -> { };

        Spec(String noun, String[] steps, List<E> store) {
            this.noun = noun;
            this.steps = steps;
            this.store = store;
        }

        Field<E> add(int step, String label, String hint) {
            Field<E> f = new Field<>(step, label, hint);
            fields.add(f);
            return f;
        }
    }

    
    private static String prompt(String text) {
        System.out.print(text);
        if (!SC.hasNextLine()) throw new InputClosed();
        return SC.nextLine().trim();
    }

    private static int menuChoice(String text, int min, int max) {
        while (true) {
            String s = prompt(text);
            try {
                int k = Integer.parseInt(s);
                if (k >= min && k <= max) return k;
                System.out.println("! Please choose a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("! '" + s + "' is not a valid number. Enter digits only.");
            }
        }
    }

    private static boolean confirm(String question) {
        System.out.println(question);
        return menuChoice("  [1] Yes  [0] No : ", 0, 1) == 1;
    }

    private static String tag(Identifiable x) {
        return "[" + x.getId() + "] " + x.getDisplayName();
    }

    private static String show(Object o) {
        if (o == null) return "(none)";
        if (o instanceof Identifiable i) return tag(i);
        if (o instanceof Boolean b) return b ? "YES" : "NO";
        if (o instanceof BigDecimal d) return d.toPlainString();
        return o.toString();
    }

    private static void dup(boolean exists, String message) throws DuplicateRecordException {
        if (exists) throw new DuplicateRecordException(message);
    }

    private static void must(List<?> list, Object o, String name) throws RecordNotFoundException {
        if (o == null || !list.contains(o)) {
            throw new RecordNotFoundException(name + " is not a registered record.");
        }
    }

    private static List<Identifiable> all() {
        List<Identifiable> l = new ArrayList<>();
        l.addAll(departments); l.addAll(students); l.addAll(teachers); l.addAll(administrators);
        l.addAll(courses); l.addAll(classrooms); l.addAll(enrollments); l.addAll(assessments);
        l.addAll(payments); l.addAll(books); l.addAll(loans);
        return l;
    }

    private static boolean idExists(String id, Object self) {
        return all().stream().anyMatch(x -> x != self && x.getId().equalsIgnoreCase(id));
    }

    private static List<Person> persons() {
        List<Person> l = new ArrayList<>();
        l.addAll(students); l.addAll(teachers); l.addAll(administrators);
        return l;
    }

    private static List<AcademicMember> members() {
        List<AcademicMember> l = new ArrayList<>();
        l.addAll(students); l.addAll(teachers);
        return l;
    }

    
    private static Parser text(String label, int min, int max) {
        return (s, v) -> Registrable.requireText(label, s, min, max);
    }

    private static Parser uniq(String label, int min, int max, Predicate<String> exists, String what) {
        return (s, v) -> {
            String t = Registrable.requireText(label, s, min, max);
            if (exists.test(t)) throw new DuplicateRecordException(what + " " + t + " already exists.");
            return t;
        };
    }

    private static Parser email(String label, Predicate<String> exists) {
        return (s, v) -> {
            String t = Registrable.requireEmail(label, s);
            if (exists.test(t)) throw new DuplicateRecordException(label + " " + t + " is already used.");
            return t;
        };
    }

    private static Parser phone(String label) {
        return (s, v) -> Registrable.requirePhone(label, s);
    }

    private static Parser date(String label, boolean mustBePast) {
        return (s, v) -> {
            LocalDate d = Registrable.parseDate(label, s);
            if (mustBePast) Registrable.requirePast(label, d);
            else Registrable.requireNotFuture(label, d);
            return d;
        };
    }

    private static Parser intR(String label, int min, int max) {
        return (s, v) -> {
            try {
                int k = Integer.parseInt(s.trim());
                Registrable.requireRange(label, k, min, max);
                return k;
            } catch (NumberFormatException e) {
                throw new ValidationException(label + " must be a whole number (" + min + "-" + max + ").");
            }
        };
    }

    private static double parseDouble(String label, String s) throws ValidationException {
        try {
            double d = Double.parseDouble(s.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a number such as 18.5.");
        }
    }

    private static Parser money(String label) {
        return (s, v) -> {
            BigDecimal b;
            try {
                b = new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                throw new ValidationException(label + " must be a number such as 1500.50.");
            }
            if (b.signum() <= 0) throw new ValidationException(label + " must be strictly greater than zero.");
            if (b.scale() > 2) throw new ValidationException(label + " may have at most 2 decimal places.");
            return b;
        };
    }

    private static String choiceHint(List<String> opts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < opts.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(i + 1).append(' ').append(opts.get(i));
        }
        return sb.toString();
    }

    private static Parser choice(String label, List<String> opts) {
        return (s, v) -> {
            String t = s.trim();
            if (t.matches("\\d{1,3}")) {
                int k = Integer.parseInt(t);
                if (k >= 1 && k <= opts.size()) return opts.get(k - 1);
            }
            for (String o : opts) if (o.equalsIgnoreCase(t)) return o;
            throw new ValidationException(label + ": choose one of " + choiceHint(opts) + ".");
        };
    }

    private static Parser yesNo(String label) {
        return (s, v) -> {
            String t = s.trim();
            if (t.equals("1") || t.equalsIgnoreCase("YES") || t.equalsIgnoreCase("Y")) return Boolean.TRUE;
            if (t.equals("2") || t.equalsIgnoreCase("NO") || t.equalsIgnoreCase("N")) return Boolean.FALSE;
            throw new ValidationException(label + ": choose 1 YES or 2 NO.");
        };
    }

    private static Parser isbn(Predicate<String> exists) {
        return (s, v) -> {
            String t = Registrable.requireText("ISBN", s, 10, 17);
            String digits = t.replace("-", "").replace(" ", "");
            if (!digits.matches("\\d{9}[\\dXx]|\\d{13}")) {
                throw new ValidationException("ISBN must contain 10 or 13 digits (hyphens allowed).");
            }
            if (exists.test(digits)) throw new DuplicateRecordException("ISBN " + t + " already exists.");
            return t;
        };
    }

   
    private static void nested(Supplier<?> registrar) {
        depth++;
        try {
            registrar.get();
        } finally {
            depth--;
        }
        System.out.println("-- Returning to the interrupted form (your earlier entries are kept) --");
    }

    private static <T> Chooser pick(String noun, String action, Supplier<List<T>> items, Function<T, String> lbl,
                                    Supplier<?> registrar, boolean optional, Checker<T> check) {
        return () -> {
            while (true) {
                List<T> list = items.get();
                boolean canRegister = registrar != null && depth < MAX_DEPTH;
                if (list.isEmpty()) {
                    if (optional) {
                        System.out.println("  No " + noun + " is registered yet - this field is optional;"
                                + " it can be assigned later with 'Update Existing Record'.");
                        return null;
                    }
                    System.out.println("  No " + noun + " is registered. Register a " + noun + " before " + action + ".");
                    if (!canRegister) throw new PrerequisiteMissingException("No " + noun + " exists.");
                    System.out.println("  1 Register " + noun + " now    0 Cancel");
                    String s = prompt("  Select: ");
                    if (s.equals("1")) { nested(registrar); continue; }
                    if (s.equalsIgnoreCase("BACK")) return BACK;
                    if (s.equals("0") || s.equalsIgnoreCase("CANCEL")) {
                        throw new PrerequisiteMissingException("No " + noun + " exists, so " + action + " was cancelled.");
                    }
                    System.out.println("! Invalid selection. Enter 1 or 0.");
                    continue;
                }
                System.out.println("  +--------------- CHOOSE " + noun.toUpperCase() + " ---------------+");
                for (int i = 0; i < list.size(); i++) System.out.println("  " + (i + 1) + " " + lbl.apply(list.get(i)));
                int n = list.size();
                int reg = canRegister ? n + 1 : -1;
                int skip = optional ? (canRegister ? n + 2 : n + 1) : -1;
                if (canRegister) System.out.println("  " + reg + " [+] Register a new " + noun);
                if (optional) System.out.println("  " + skip + " (none) leave empty");
                System.out.println("  0 Cancel");
                String s = prompt("  Select option: ");
                if (s.equalsIgnoreCase("CANCEL") || s.equals("0")) return CANCEL;
                if (s.equalsIgnoreCase("BACK")) return BACK;
                int k;
                try {
                    k = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    System.out.println("! Enter the number of an option from the list.");
                    continue;
                }
                if (k >= 1 && k <= n) {
                    T t = list.get(k - 1);
                    if (check != null) {
                        try {
                            check.check(t);
                        } catch (InputClosed ic) {
                            throw ic;
                        } catch (Exception e) {
                            System.out.println("! " + e.getMessage());
                            continue;
                        }
                    }
                    return t;
                }
                if (k == reg) { nested(registrar); continue; }
                if (k == skip) return null;
                System.out.println("! Option " + k + " does not exist.");
            }
        };
    }

    
    private static Object[] values(Spec<?> sp) {
        Object[] v = new Object[sp.fields.size()];
        for (int i = 0; i < v.length; i++) v[i] = sp.fields.get(i).value;
        return v;
    }

    private static void banner(Spec<?> sp, String mode) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("| AUCA_ID / SCHOOL MANAGEMENT");
        System.out.println("| " + sp.noun.toUpperCase() + " " + mode + "   (" + sp.steps.length + " steps)");
        System.out.println(LINE);
        for (int i = 0; i < sp.steps.length; i++) System.out.println("| [" + (i + 1) + "] " + sp.steps[i]);
        System.out.println(LINE);
        System.out.println("| Type BACK to return to the previous field;");
        System.out.println("| CANCEL to discard the unsaved " + sp.noun.toLowerCase() + ".");
        System.out.println(LINE);
    }

    private static void stepHeader(Spec<?> sp, int step) {
        System.out.println();
        System.out.println("[STEP " + step + "/" + sp.steps.length + "] " + sp.steps[step - 1].toUpperCase());
    }

    private static void printReview(Spec<?> sp, boolean update) {
        System.out.println();
        System.out.println("+---------------- REVIEW " + sp.noun.toUpperCase() + (update ? " (UPDATE)" : "") + " ----------------+");
        int shown = -1;
        for (int i = 0; i < sp.fields.size(); i++) {
            Field<?> f = sp.fields.get(i);
            if (f.step != shown) {
                System.out.println("| " + sp.steps[f.step - 1].toUpperCase());
                shown = f.step;
            }
            String lock = update && f.setter == null ? "   [locked]" : "";
            System.out.printf("|  %02d %-20s: %s%s%n", i + 1, f.label, show(f.value), lock);
        }
        System.out.println(LINE);
        System.out.println("| [1] " + (update ? "Save changes" : "Save " + sp.noun.toLowerCase()) + "    [2] Edit field    [0] Cancel");
        System.out.println(LINE);
    }

    
    private static <E extends Identifiable & Registrable> E register(Spec<E> sp) {
        banner(sp, "REGISTRATION");
        int i = 0, shown = -1, n = sp.fields.size();
        try {
            while (i < n) {
                Field<E> f = sp.fields.get(i);
                if (f.step != shown) { stepHeader(sp, f.step); shown = f.step; }
                if (f.derived) {
                    System.out.printf("%02d %-20s [%s]%n", i + 1, f.label, f.hint);
                    i++;
                    continue;
                }
                int r = f.fill(i + 1, values(sp));
                if (r == CANCEL_) {
                    System.out.println("Registration cancelled. Nothing was saved.");
                    return null;
                }
                if (r == BACK_) {
                    if (i == 0) {
                        System.out.println("Already at the first field.");
                    } else {
                        i--;
                        while (i > 0 && sp.fields.get(i).derived) i--;
                    }
                    continue;
                }
                i++;
            }
        } catch (PrerequisiteMissingException e) {
            System.out.println("! " + e.getMessage());
            System.out.println("Registration cancelled. Nothing was saved.");
            return null;
        }
        return review(sp, null);
    }

    private static <E extends Identifiable & Registrable> E review(Spec<E> sp, E existing) {
        boolean update = existing != null;
        while (true) {
            printReview(sp, update);
            String opt = prompt("Select option: ");
            if (opt.equals("1")) {
                if (update && !confirm("Apply these changes to " + sp.noun + " " + existing.getId() + "?")) continue;
                try {
                    if (update) {
                        commitUpdate(sp, existing);
                        System.out.println("+ " + sp.noun + " " + existing.getId() + " updated successfully.");
                        return existing;
                    }
                    E e = sp.factory.apply(values(sp));
                    e.validate();
                    sp.checker.check(e);
                    sp.store.add(e);
                    sp.after.accept(e);
                    System.out.println("+ " + sp.noun + " " + e.getId() + " registered successfully.");
                    return e;
                } catch (InputClosed ic) {
                    throw ic;
                } catch (Exception ex) {
                    String m = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    System.out.println("! Cannot save: " + m);
                    System.out.println("  Use option 2 to correct the field, or 0 to cancel.");
                }
            } else if (opt.equals("2")) {
                editField(sp, existing);
            } else if (opt.equals("0")) {
                System.out.println((update ? "Update" : "Registration") + " cancelled. Nothing was changed.");
                return null;
            } else {
                System.out.println("! Invalid option. Choose 1, 2 or 0.");
            }
        }
    }

    private static <E extends Identifiable & Registrable> void editField(Spec<E> sp, E existing) {
        int n = sp.fields.size();
        String s = prompt("Field number to edit (01-" + String.format("%02d", n) + "), 0 to go back: ");
        int k;
        try {
            k = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println("! Enter a field number such as 06.");
            return;
        }
        if (k == 0) return;
        if (k < 1 || k > n) {
            System.out.println("! Field number must be between 01 and " + String.format("%02d", n) + ".");
            return;
        }
        Field<E> f = sp.fields.get(k - 1);
        if (f.derived) {
            System.out.println("! Field " + String.format("%02d", k) + " is derived and cannot be edited.");
            return;
        }
        if (existing != null && f.setter == null) {
            System.out.println("! Field " + String.format("%02d", k) + " (" + f.label + ") is locked and cannot be changed after registration.");
            return;
        }
        System.out.println("Current value: " + show(f.value));
        try {
            int r = f.fill(k, values(sp));
            if (r != OK_) System.out.println("Edit abandoned; the old value is kept.");
        } catch (PrerequisiteMissingException e) {
            System.out.println("! " + e.getMessage() + " The old value is kept.");
        }
    }

    private static <E extends Identifiable & Registrable> void commitUpdate(Spec<E> sp, E e) throws Exception {
        List<Object> old = new ArrayList<>();
        for (Field<E> f : sp.fields) old.add(f.getter == null ? null : f.getter.apply(e));
        try {
            for (Field<E> f : sp.fields) if (f.setter != null) f.setter.accept(e, f.value);
            e.validate();
            sp.checker.check(e);
        } catch (Exception ex) { 
            for (int i = 0; i < sp.fields.size(); i++) {
                Field<E> f = sp.fields.get(i);
                if (f.setter != null) f.setter.accept(e, old.get(i));
            }
            throw ex;
        }
    }

    
    private static <E extends Identifiable & Registrable> void run(Supplier<Spec<E>> maker) {
        while (true) {
            E e = register(maker.get());
            if (e == null) return;
            while (true) {
                System.out.println(" [1] Register another   [2] View record   [0] Main menu");
                String s = prompt("Select option: ");
                if (s.equals("1")) break;
                if (s.equals("2")) e.printDetails();
                else if (s.equals("0")) return;
                else System.out.println("! Invalid option.");
            }
        }
    }

    
    private static void checkId(Identifiable e) throws DuplicateRecordException {
        dup(idExists(e.getId(), e), "ID " + e.getId() + " already exists.");
    }

    private static void checkPerson(Person e) throws DuplicateRecordException {
        checkId(e);
        dup(persons().stream().anyMatch(x -> x != e && x.getEmail().equalsIgnoreCase(e.getEmail())),
                "Email " + e.getEmail() + " is already used.");
    }

    private static void checkMember(AcademicMember e) throws DuplicateRecordException, RecordNotFoundException {
        dup(members().stream().anyMatch(x -> x != e && x.getMemberNumber().equalsIgnoreCase(e.getMemberNumber())),
                "Member number " + e.getMemberNumber() + " already exists.");
        dup(members().stream().anyMatch(x -> x != e && x.getInstitutionalEmail().equalsIgnoreCase(e.getInstitutionalEmail())),
                "Institutional email " + e.getInstitutionalEmail() + " is already used.");
        must(departments, e.getDepartment(), "Department");
    }

    private static <E extends Person> void personFields(Spec<E> sp, E ex) {
        sp.add(1, "Person ID", "String | unique | 4-30 chars")
                .parse(uniq("Person ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Person ID"))
                .bind(Person::getPersonId, null);
        sp.add(1, "First name", "String | 2-60 chars")
                .parse(text("First name", 2, 60))
                .bind(Person::getFirstName, (o, v) -> o.setFirstName((String) v));
        sp.add(1, "Last name", "String | 2-60 chars")
                .parse(text("Last name", 2, 60))
                .bind(Person::getLastName, (o, v) -> o.setLastName((String) v));
        sp.add(1, "Email", "String | unique | email")
                .parse(email("Email", s -> persons().stream().anyMatch(x -> x != ex && x.getEmail().equalsIgnoreCase(s))))
                .bind(Person::getEmail, (o, v) -> o.setEmail((String) v));
        sp.add(1, "Phone", "String | 10-15 digits, optional +")
                .parse(phone("Phone"))
                .bind(Person::getPhoneNumber, (o, v) -> o.setPhoneNumber((String) v));
        sp.add(1, "Date of birth", "LocalDate | YYYY-MM-DD")
                .parse(date("Date of birth", true))
                .bind(Person::getDateOfBirth, (o, v) -> o.setDateOfBirth((LocalDate) v));
    }

    private static <E extends AcademicMember> void academicFields(Spec<E> sp, E ex) {
        sp.add(2, "Member number", "String | unique | 4-30")
                .parse(uniq("Member number", ID_MIN, ID_MAX,
                        s -> members().stream().anyMatch(x -> x != ex && x.getMemberNumber().equalsIgnoreCase(s)), "Member number"))
                .bind(AcademicMember::getMemberNumber, (o, v) -> o.setMemberNumber((String) v));
        sp.add(2, "Department", "Department | select")
                .choose(pick("Department", "registering this member", () -> departments, Main::tag,
                        () -> registerDepartment(), false, null))
                .bind(AcademicMember::getDepartment, (o, v) -> o.setDepartment((Department) v));
        sp.add(2, "Join date", "LocalDate | YYYY-MM-DD")
                .parse(date("Join date", false))
                .bind(AcademicMember::getJoinDate, (o, v) -> o.setJoinDate((LocalDate) v));
        sp.add(2, "Academic status", choiceHint(AcademicMember.STATUSES))
                .parse(choice("Academic status", AcademicMember.STATUSES))
                .bind(AcademicMember::getAcademicStatus, (o, v) -> o.setAcademicStatus((String) v));
        sp.add(2, "Institutional email", "String | unique | email")
                .parse(email("Institutional email",
                        s -> members().stream().anyMatch(x -> x != ex && x.getInstitutionalEmail().equalsIgnoreCase(s))))
                .bind(AcademicMember::getInstitutionalEmail, (o, v) -> o.setInstitutionalEmail((String) v));
        sp.add(2, "Advisor name", "String | 2-120 chars")
                .parse(text("Advisor name", 2, 120))
                .bind(AcademicMember::getAdvisorName, (o, v) -> o.setAdvisorName((String) v));
    }

    private static final String REVIEW = "REVIEW & CONFIRM";

   
    private static Spec<Department> departmentSpec(Department ex) {
        Spec<Department> sp = new Spec<>("Department",
                new String[]{"DEPARTMENT IDENTITY", "CONTACT & LOCATION", "HEAD TEACHER", REVIEW}, departments);
        sp.add(1, "Department ID", "String | unique | 4-30 chars")
                .parse(uniq("Department ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Department ID"))
                .bind(Department::getDepartmentId, null);
        sp.add(1, "Department name", "String | unique | 2-60 chars")
                .parse(uniq("Department name", 2, 60,
                        s -> departments.stream().anyMatch(x -> x != ex && x.getDepartmentName().equalsIgnoreCase(s)), "Department name"))
                .bind(Department::getDepartmentName, (o, v) -> o.setDepartmentName((String) v));
        sp.add(1, "Department code", "String | unique | 2-10 chars")
                .parse(uniq("Department code", 2, 10,
                        s -> departments.stream().anyMatch(x -> x != ex && x.getDepartmentCode().equalsIgnoreCase(s)), "Department code"))
                .bind(Department::getDepartmentCode, (o, v) -> o.setDepartmentCode((String) v));
        sp.add(2, "Office location", "String | 2-60 chars")
                .parse(text("Office location", 2, 60))
                .bind(Department::getOfficeLocation, (o, v) -> o.setOfficeLocation((String) v));
        sp.add(2, "Contact email", "String | email")
                .parse(email("Contact email", s -> false))
                .bind(Department::getContactEmail, (o, v) -> o.setContactEmail((String) v));
        sp.add(3, "Head teacher", "Teacher | optional | select")
                .choose(pick("Teacher", "assigning a head teacher", () -> teachers, Main::tag, null, true, null))
                .bind(Department::getHeadTeacher, (o, v) -> o.setHeadTeacher((Teacher) v));
        sp.factory = v -> new Department((String) v[0], (String) v[1], (String) v[2], (String) v[3], (String) v[4], (Teacher) v[5]);
        sp.checker = e -> {
            checkId(e);
            dup(departments.stream().anyMatch(x -> x != e && x.getDepartmentName().equalsIgnoreCase(e.getDepartmentName())),
                    "Department name " + e.getDepartmentName() + " already exists.");
            dup(departments.stream().anyMatch(x -> x != e && x.getDepartmentCode().equalsIgnoreCase(e.getDepartmentCode())),
                    "Department code " + e.getDepartmentCode() + " already exists.");
            if (e.getHeadTeacher() != null) must(teachers, e.getHeadTeacher(), "Head teacher");
        };
        return sp;
    }

    private static Spec<Student> studentSpec(Student ex) {
        Spec<Student> sp = new Spec<>("Student",
                new String[]{"PERSONAL DETAILS", "ACADEMIC DETAILS", "STUDENT DETAILS", REVIEW}, students);
        personFields(sp, ex);
        academicFields(sp, ex);
        sp.add(3, "Student number", "String | unique | 4-30")
                .parse(uniq("Student number", ID_MIN, ID_MAX,
                        s -> students.stream().anyMatch(x -> x != ex && x.getStudentNumber().equalsIgnoreCase(s)), "Student number"))
                .bind(Student::getStudentNumber, (o, v) -> o.setStudentNumber((String) v));
        sp.add(3, "Program name", "String | 2-120 chars")
                .parse(text("Program name", 2, 120))
                .bind(Student::getProgramName, (o, v) -> o.setProgramName((String) v));
        sp.add(3, "Admission date", "LocalDate | YYYY-MM-DD")
                .parse(date("Admission date", false))
                .bind(Student::getAdmissionDate, (o, v) -> o.setAdmissionDate((LocalDate) v));
        sp.add(3, "Study year", "int | 1-" + Student.MAX_STUDY_YEAR)
                .parse(intR("Study year", 1, Student.MAX_STUDY_YEAR))
                .bind(Student::getStudyYear, (o, v) -> o.setStudyYear((Integer) v));
        sp.add(3, "Guardian phone", "String | 10-15 digits")
                .parse(phone("Guardian phone"))
                .bind(Student::getGuardianPhone, (o, v) -> o.setGuardianPhone((String) v));
        sp.add(3, "Scholarship", "1 YES / 2 NO")
                .parse(yesNo("Scholarship"))
                .bind(Student::isScholarshipStatus, (o, v) -> o.setScholarshipStatus((Boolean) v));
        sp.factory = v -> new Student((String) v[0], (String) v[1], (String) v[2], (String) v[3], (String) v[4],
                (LocalDate) v[5], (String) v[6], (Department) v[7], (LocalDate) v[8], (String) v[9], (String) v[10],
                (String) v[11], (String) v[12], (String) v[13], (LocalDate) v[14], (Integer) v[15], (String) v[16],
                (Boolean) v[17]);
        sp.checker = e -> {
            checkPerson(e);
            checkMember(e);
            dup(students.stream().anyMatch(x -> x != e && x.getStudentNumber().equalsIgnoreCase(e.getStudentNumber())),
                    "Student number " + e.getStudentNumber() + " already exists.");
        };
        return sp;
    }

    private static Spec<Teacher> teacherSpec(Teacher ex) {
        Spec<Teacher> sp = new Spec<>("Teacher",
                new String[]{"PERSONAL DETAILS", "ACADEMIC DETAILS", "TEACHER DETAILS", REVIEW}, teachers);
        personFields(sp, ex);
        academicFields(sp, ex);
        sp.add(3, "Employee number", "String | unique | 4-30")
                .parse(uniq("Employee number", ID_MIN, ID_MAX,
                        s -> teachers.stream().anyMatch(x -> x != ex && x.getEmployeeNumber().equalsIgnoreCase(s)), "Employee number"))
                .bind(Teacher::getEmployeeNumber, (o, v) -> o.setEmployeeNumber((String) v));
        sp.add(3, "Specialization", "String | 2-100 chars")
                .parse(text("Specialization", 2, 100))
                .bind(Teacher::getSpecialization, (o, v) -> o.setSpecialization((String) v));
        sp.add(3, "Hire date", "LocalDate | YYYY-MM-DD")
                .parse(date("Hire date", false))
                .bind(Teacher::getHireDate, (o, v) -> o.setHireDate((LocalDate) v));
        sp.add(3, "Salary", "BigDecimal | > 0")
                .parse(money("Salary"))
                .bind(Teacher::getSalary, (o, v) -> o.setSalary((BigDecimal) v));
        sp.add(3, "Office number", "String | 1-20 chars")
                .parse(text("Office number", 1, 20))
                .bind(Teacher::getOfficeNumber, (o, v) -> o.setOfficeNumber((String) v));
        sp.add(3, "Employment type", choiceHint(Teacher.EMPLOYMENT_TYPES))
                .parse(choice("Employment type", Teacher.EMPLOYMENT_TYPES))
                .bind(Teacher::getEmploymentType, (o, v) -> o.setEmploymentType((String) v));
        sp.factory = v -> new Teacher((String) v[0], (String) v[1], (String) v[2], (String) v[3], (String) v[4],
                (LocalDate) v[5], (String) v[6], (Department) v[7], (LocalDate) v[8], (String) v[9], (String) v[10],
                (String) v[11], (String) v[12], (String) v[13], (LocalDate) v[14], (BigDecimal) v[15], (String) v[16],
                (String) v[17]);
        sp.checker = e -> {
            checkPerson(e);
            checkMember(e);
            dup(teachers.stream().anyMatch(x -> x != e && x.getEmployeeNumber().equalsIgnoreCase(e.getEmployeeNumber())),
                    "Employee number " + e.getEmployeeNumber() + " already exists.");
        };
        return sp;
    }

    private static Spec<Administrator> administratorSpec(Administrator ex) {
        Spec<Administrator> sp = new Spec<>("Administrator",
                new String[]{"PERSONAL DETAILS", "POSITION DETAILS", "EMPLOYMENT & ACCESS", REVIEW}, administrators);
        personFields(sp, ex);
        sp.add(2, "Staff number", "String | unique | 4-30")
                .parse(uniq("Staff number", ID_MIN, ID_MAX,
                        s -> administrators.stream().anyMatch(x -> x != ex && x.getStaffNumber().equalsIgnoreCase(s)), "Staff number"))
                .bind(Administrator::getStaffNumber, (o, v) -> o.setStaffNumber((String) v));
        sp.add(2, "Job title", "String | 2-60 chars")
                .parse(text("Job title", 2, 60))
                .bind(Administrator::getJobTitle, (o, v) -> o.setJobTitle((String) v));
        sp.add(2, "Assigned office", "String | 1-60 chars")
                .parse(text("Assigned office", 1, 60))
                .bind(Administrator::getAssignedOffice, (o, v) -> o.setAssignedOffice((String) v));
        sp.add(3, "Employment date", "LocalDate | YYYY-MM-DD")
                .parse(date("Employment date", false))
                .bind(Administrator::getEmploymentDate, (o, v) -> o.setEmploymentDate((LocalDate) v));
        sp.add(3, "Access level", "int | 1-5")
                .parse(intR("Access level", 1, 5))
                .bind(Administrator::getAccessLevel, (o, v) -> o.setAccessLevel((Integer) v));
        sp.add(3, "Active", "1 YES / 2 NO")
                .parse(yesNo("Active"))
                .bind(Administrator::isActive, (o, v) -> o.setActive((Boolean) v));
        sp.factory = v -> new Administrator((String) v[0], (String) v[1], (String) v[2], (String) v[3], (String) v[4],
                (LocalDate) v[5], (String) v[6], (String) v[7], (String) v[8], (LocalDate) v[9], (Integer) v[10],
                (Boolean) v[11]);
        sp.checker = e -> {
            checkPerson(e);
            dup(administrators.stream().anyMatch(x -> x != e && x.getStaffNumber().equalsIgnoreCase(e.getStaffNumber())),
                    "Staff number " + e.getStaffNumber() + " already exists.");
        };
        return sp;
    }

    private static Spec<Course> courseSpec(Course ex) {
        Spec<Course> sp = new Spec<>("Course",
                new String[]{"COURSE IDENTITY", "CREDITS & DEPARTMENT", "TEACHER", REVIEW}, courses);
        sp.add(1, "Course ID", "String | unique | 4-30 chars")
                .parse(uniq("Course ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Course ID"))
                .bind(Course::getCourseId, null);
        sp.add(1, "Course code", "String | unique | 3-12 chars")
                .parse(uniq("Course code", 3, 12,
                        s -> courses.stream().anyMatch(x -> x != ex && x.getCourseCode().equalsIgnoreCase(s)), "Course code"))
                .bind(Course::getCourseCode, (o, v) -> o.setCourseCode((String) v));
        sp.add(1, "Course title", "String | 2-100 chars")
                .parse(text("Course title", 2, 100))
                .bind(Course::getCourseTitle, (o, v) -> o.setCourseTitle((String) v));
        sp.add(2, "Credit hours", "int | 1-6")
                .parse(intR("Credit hours", 1, 6))
                .bind(Course::getCreditHours, (o, v) -> o.setCreditHours((Integer) v));
        sp.add(2, "Department", "Department | select")
                .choose(pick("Department", "registering a course", () -> departments, Main::tag,
                        () -> registerDepartment(), false, null))
                .bind(Course::getDepartment, (o, v) -> o.setDepartment((Department) v));
        sp.add(3, "Assigned teacher", "Teacher | select")
                .choose(pick("Teacher", "registering a course", () -> teachers, Main::tag,
                        () -> registerTeacher(), false, null))
                .bind(Course::getAssignedTeacher, (o, v) -> o.setAssignedTeacher((Teacher) v));
        sp.factory = v -> new Course((String) v[0], (String) v[1], (String) v[2], (Integer) v[3], (Department) v[4], (Teacher) v[5]);
        sp.checker = e -> {
            checkId(e);
            dup(courses.stream().anyMatch(x -> x != e && x.getCourseCode().equalsIgnoreCase(e.getCourseCode())),
                    "Course code " + e.getCourseCode() + " already exists.");
            must(departments, e.getDepartment(), "Department");
            must(teachers, e.getAssignedTeacher(), "Teacher");
        };
        return sp;
    }

    private static Spec<Classroom> classroomSpec(Classroom ex) {
        Spec<Classroom> sp = new Spec<>("Classroom",
                new String[]{"CLASSROOM LOCATION", "SIZE & FLOOR", "AVAILABILITY", REVIEW}, classrooms);
        sp.add(1, "Classroom ID", "String | unique | 4-30 chars")
                .parse(uniq("Classroom ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Classroom ID"))
                .bind(Classroom::getClassroomId, null);
        sp.add(1, "Building name", "String | 2-60 chars")
                .parse(text("Building name", 2, 60))
                .bind(Classroom::getBuildingName, (o, v) -> o.setBuildingName((String) v));
        sp.add(1, "Room number", "String | 1-20 chars")
                .parse(text("Room number", 1, 20))
                .bind(Classroom::getRoomNumber, (o, v) -> o.setRoomNumber((String) v));
        sp.add(2, "Capacity", "int | > 0")
                .parse(intR("Capacity", 1, 10000))
                .bind(Classroom::getCapacity, (o, v) -> o.setCapacity((Integer) v));
        sp.add(2, "Floor number", "int | >= 0")
                .parse(intR("Floor number", 0, 200))
                .bind(Classroom::getFloorNumber, (o, v) -> o.setFloorNumber((Integer) v));
        sp.add(3, "Available", "1 YES / 2 NO")
                .parse(yesNo("Available"))
                .bind(Classroom::isActive, (o, v) -> o.setAvailable((Boolean) v));
        sp.factory = v -> new Classroom((String) v[0], (String) v[1], (String) v[2], (Integer) v[3], (Integer) v[4], (Boolean) v[5]);
        sp.checker = e -> {
            checkId(e);
            dup(classrooms.stream().anyMatch(x -> x != e && x.getBuildingName().equalsIgnoreCase(e.getBuildingName())
                    && x.getRoomNumber().equalsIgnoreCase(e.getRoomNumber())),
                    "Room " + e.getRoomNumber() + " in " + e.getBuildingName() + " is already registered.");
        };
        return sp;
    }

    private static Spec<Enrollment> enrollmentSpec(Enrollment ex) {
        Spec<Enrollment> sp = new Spec<>("Enrollment",
                new String[]{"STUDENT & COURSE", "TERM DETAILS", "STATUS", REVIEW}, enrollments);
        sp.add(1, "Enrollment ID", "String | unique | 4-30 chars")
                .parse(uniq("Enrollment ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Enrollment ID"))
                .bind(Enrollment::getEnrollmentId, null);
        sp.add(1, "Student", "Student | select")
                .choose(pick("Student", "enrolling a student", () -> students, Main::tag, () -> registerStudent(), false, null))
                .bind(Enrollment::getStudent, (o, v) -> o.setStudent((Student) v));
        sp.add(1, "Course", "Course | select")
                .choose(pick("Course", "enrolling a student", () -> courses, Main::tag, () -> registerCourse(), false, null))
                .bind(Enrollment::getCourse, (o, v) -> o.setCourse((Course) v));
        sp.add(2, "Enrollment date", "LocalDate | YYYY-MM-DD")
                .parse(date("Enrollment date", false))
                .bind(Enrollment::getEnrollmentDate, (o, v) -> o.setEnrollmentDate((LocalDate) v));
        sp.add(2, "Semester", choiceHint(Enrollment.SEMESTERS))
                .parse(choice("Semester", Enrollment.SEMESTERS))
                .bind(Enrollment::getSemester, (o, v) -> o.setSemester((String) v));
        sp.add(3, "Status", choiceHint(Enrollment.STATUSES))
                .parse(choice("Enrollment status", Enrollment.STATUSES))
                .bind(Enrollment::getEnrollmentStatus, (o, v) -> o.setEnrollmentStatus((String) v));
        sp.factory = v -> new Enrollment((String) v[0], (Student) v[1], (Course) v[2], (LocalDate) v[3], (String) v[4], (String) v[5]);
        sp.checker = e -> {
            checkId(e);
            must(students, e.getStudent(), "Student");
            must(courses, e.getCourse(), "Course");
            if (e.isActive()) {
                dup(enrollments.stream().anyMatch(x -> x != e && x.isActive() && x.getStudent() == e.getStudent()
                        && x.getCourse() == e.getCourse() && x.getSemester().equals(e.getSemester())),
                        e.getStudent().getDisplayName() + " already has an active enrollment in "
                                + e.getCourse().getCourseCode() + " for " + e.getSemester() + ".");
            }
        };
        return sp;
    }

    private static Spec<Assessment> assessmentSpec(Assessment ex) {
        Spec<Assessment> sp = new Spec<>("Assessment",
                new String[]{"ENROLLMENT & TYPE", "SCORING", "DATE", REVIEW}, assessments);
        sp.add(1, "Assessment ID", "String | unique | 4-30 chars")
                .parse(uniq("Assessment ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Assessment ID"))
                .bind(Assessment::getAssessmentId, null);
        sp.add(1, "Enrollment", "Enrollment | active | select")
                .choose(pick("active Enrollment", "recording an assessment",
                        () -> enrollments.stream().filter(Enrollment::isActive).toList(), Main::tag,
                        () -> registerEnrollment(), false, null))
                .bind(Assessment::getEnrollment, (o, v) -> o.setEnrollment((Enrollment) v));
        sp.add(1, "Assessment type", choiceHint(Assessment.TYPES))
                .parse(choice("Assessment type", Assessment.TYPES))
                .bind(Assessment::getAssessmentType, (o, v) -> o.setAssessmentType((String) v));
        sp.add(2, "Maximum score", "double | > 0")
                .parse((s, v) -> {
                    double d = parseDouble("Maximum score", s);
                    if (d <= 0) throw new ValidationException("Maximum score must be greater than zero.");
                    return d;
                })
                .bind(Assessment::getMaximumScore, (o, v) -> o.setMaximumScore((Double) v));
        sp.add(2, "Obtained score", "double | 0 .. maximum")
                .parse((s, v) -> {
                    double d = parseDouble("Obtained score", s);
                    Double max = (Double) v[3];
                    if (d < 0 || (max != null && d > max)) {
                        throw new ValidationException("Obtained score must be between 0 and "
                                + (max == null ? "the maximum score" : max.toString()) + ".");
                    }
                    return d;
                })
                .bind(Assessment::getObtainedScore, (o, v) -> o.setObtainedScore((Double) v));
        sp.add(3, "Assessment date", "LocalDate | YYYY-MM-DD")
                .parse(date("Assessment date", false))
                .bind(Assessment::getAssessmentDate, (o, v) -> o.setAssessmentDate((LocalDate) v));
        sp.factory = v -> new Assessment((String) v[0], (Enrollment) v[1], (String) v[2], (Double) v[3], (Double) v[4], (LocalDate) v[5]);
        sp.checker = e -> {
            checkId(e);
            must(enrollments, e.getEnrollment(), "Enrollment");
            if (!assessments.contains(e) && !e.getEnrollment().isActive()) {
                throw new ValidationException("Only an active enrollment can be assessed.");
            }
        };
        return sp;
    }

    private static Spec<FeePayment> paymentSpec(FeePayment ex) {
        Spec<FeePayment> sp = new Spec<>("Fee payment",
                new String[]{"PAYER", "AMOUNT & DATE", "METHOD & RECEIPT", REVIEW}, payments);
        sp.add(1, "Payment ID", "String | unique | 4-30 chars")
                .parse(uniq("Payment ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Payment ID"))
                .bind(FeePayment::getPaymentId, null);
        sp.add(1, "Student", "Student | select")
                .choose(pick("Student", "recording a payment", () -> students, Main::tag, () -> registerStudent(), false, null))
                .bind(FeePayment::getStudent, (o, v) -> o.setStudent((Student) v));
        sp.add(2, "Amount", "BigDecimal | > 0, max 2 decimals")
                .parse(money("Amount"))
                .bind(FeePayment::getAmount, (o, v) -> o.setAmount((BigDecimal) v));
        sp.add(2, "Payment date", "LocalDate | YYYY-MM-DD")
                .parse(date("Payment date", false))
                .bind(FeePayment::getPaymentDate, (o, v) -> o.setPaymentDate((LocalDate) v));
        sp.add(3, "Payment method", choiceHint(FeePayment.METHODS))
                .parse(choice("Payment method", FeePayment.METHODS))
                .bind(FeePayment::getPaymentMethod, (o, v) -> o.setPaymentMethod((String) v));
        sp.add(3, "Receipt number", "String | unique | 3-30 chars")
                .parse(uniq("Receipt number", 3, 30,
                        s -> payments.stream().anyMatch(x -> x != ex && x.getReceiptNumber().equalsIgnoreCase(s)), "Receipt number"))
                .bind(FeePayment::getReceiptNumber, (o, v) -> o.setReceiptNumber((String) v));
        sp.factory = v -> new FeePayment((String) v[0], (Student) v[1], (BigDecimal) v[2], (LocalDate) v[3], (String) v[4], (String) v[5]);
        sp.checker = e -> {
            checkId(e);
            must(students, e.getStudent(), "Student");
            dup(payments.stream().anyMatch(x -> x != e && x.getReceiptNumber().equalsIgnoreCase(e.getReceiptNumber())),
                    "Receipt number " + e.getReceiptNumber() + " already exists.");
        };
        return sp;
    }

    private static Spec<LibraryBook> bookSpec(LibraryBook ex) {
        Spec<LibraryBook> sp = new Spec<>("Library book",
                new String[]{"BOOK IDENTITY", "BOOK DETAILS", "STOCK", REVIEW}, books);
        sp.add(1, "Book ID", "String | unique | 4-30 chars")
                .parse(uniq("Book ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Book ID"))
                .bind(LibraryBook::getBookId, null);
        sp.add(1, "ISBN", "String | unique | 10 or 13 digits")
                .parse(isbn(s -> books.stream().anyMatch(x -> x != ex
                        && x.getIsbn().replace("-", "").replace(" ", "").equalsIgnoreCase(s))))
                .bind(LibraryBook::getIsbn, (o, v) -> o.setIsbn((String) v));
        sp.add(2, "Title", "String | 1-150 chars")
                .parse(text("Title", 1, 150))
                .bind(LibraryBook::getTitle, (o, v) -> o.setTitle((String) v));
        sp.add(2, "Author", "String | 2-100 chars")
                .parse(text("Author", 2, 100))
                .bind(LibraryBook::getAuthor, (o, v) -> o.setAuthor((String) v));
        sp.add(3, "Total copies", "int | >= 0")
                .parse(intR("Total copies", 0, 100000))
                .bind(LibraryBook::getTotalCopies, (o, v) -> o.setTotalCopies((Integer) v));
        sp.add(3, "Available copies", "int | 0 .. total copies")
                .parse((s, v) -> {
                    int a;
                    try {
                        a = Integer.parseInt(s.trim());
                    } catch (NumberFormatException e) {
                        throw new ValidationException("Available copies must be a whole number.");
                    }
                    Integer total = (Integer) v[4];
                    if (a < 0 || (total != null && a > total)) {
                        throw new ValidationException("Available copies must be between 0 and total copies"
                                + (total == null ? "." : " (" + total + ")."));
                    }
                    return a;
                })
                .bind(LibraryBook::getAvailableCopies, (o, v) -> o.setAvailableCopies((Integer) v));
        sp.factory = v -> new LibraryBook((String) v[0], (String) v[1], (String) v[2], (String) v[3], (Integer) v[4], (Integer) v[5]);
        sp.checker = e -> {
            checkId(e);
            String d = e.getIsbn().replace("-", "").replace(" ", "");
            dup(books.stream().anyMatch(x -> x != e && x.getIsbn().replace("-", "").replace(" ", "").equalsIgnoreCase(d)),
                    "ISBN " + e.getIsbn() + " already exists.");
        };
        return sp;
    }

   
    private static Spec<BookLoan> loanSpec(BookLoan ex) {
        Spec<BookLoan> sp = new Spec<>("Book loan",
                new String[]{"BORROWER & BOOK", "DATES", "RETURN (DERIVED)", REVIEW}, loans);
        sp.add(1, "Loan ID", "String | unique | 4-30 chars")
                .parse(uniq("Loan ID", ID_MIN, ID_MAX, s -> idExists(s, null), "Loan ID"))
                .bind(BookLoan::getLoanId, null);
        sp.add(1, "Student", "Student | select")
                .choose(pick("Student", "borrowing a book", () -> students, Main::tag, () -> registerStudent(), false, null))
                .bind(BookLoan::getStudent, null);
        sp.add(1, "Book", "LibraryBook | select (copy must be available)")
                .choose(pick("Library Book", "borrowing a book", () -> books,
                        b -> tag(b) + "  (" + b.getAvailableCopies() + " of " + b.getTotalCopies() + " available)",
                        () -> registerBook(), false,
                        b -> {
                            if (b.getAvailableCopies() <= 0) {
                                throw new BookUnavailableException("No copy of '" + b.getTitle() + "' is available right now.");
                            }
                        }))
                .bind(BookLoan::getBook, null);
        sp.add(2, "Borrowed date", "LocalDate | YYYY-MM-DD")
                .parse(date("Borrowed date", false))
                .bind(BookLoan::getBorrowedDate, null);
        sp.add(2, "Due date", "LocalDate | after borrowed date")
                .parse((s, v) -> {
                    LocalDate d = Registrable.parseDate("Due date", s);
                    LocalDate b = (LocalDate) v[3];
                    if (b != null && !d.isAfter(b)) {
                        throw new ValidationException("Due date must be after the borrowed date (" + b + ").");
                    }
                    return d;
                })
                .bind(BookLoan::getDueDate, (o, v) -> o.setDueDate((LocalDate) v));
        sp.add(3, "Returned date", "LocalDate | derived: empty until returned")
                .derive()
                .bind(BookLoan::getReturnedDate, null);
        sp.factory = v -> new BookLoan((String) v[0], (Student) v[1], (LibraryBook) v[2], (LocalDate) v[3], (LocalDate) v[4], null);
        sp.checker = e -> {
            checkId(e);
            must(students, e.getStudent(), "Student");
            must(books, e.getBook(), "Library book");
            if (!loans.contains(e) && e.getBook().getAvailableCopies() <= 0) { // only when creating a new loan
                throw new BookUnavailableException("No copy of '" + e.getBook().getTitle() + "' is available.");
            }
        };
        sp.after = e -> e.getBook().setAvailableCopies(e.getBook().getAvailableCopies() - 1); // stock -1, once
        return sp;
    }

    
    private static Department registerDepartment() { return register(departmentSpec(null)); }

    private static Student registerStudent() { return register(studentSpec(null)); }

    private static Teacher registerTeacher() { return register(teacherSpec(null)); }

    private static Course registerCourse() { return register(courseSpec(null)); }

    private static Enrollment registerEnrollment() { return register(enrollmentSpec(null)); }

    private static LibraryBook registerBook() { return register(bookSpec(null)); }

    
    private static <T> T selectOne(String title, List<T> list, Function<T, String> lbl) {
        System.out.println();
        System.out.println("+---------------- " + title + " ----------------+");
        for (int i = 0; i < list.size(); i++) System.out.println(" " + (i + 1) + " " + lbl.apply(list.get(i)));
        System.out.println(" 0 Cancel");
        int k = menuChoice("Select option: ", 0, list.size());
        return k == 0 ? null : list.get(k - 1);
    }

    private static void returnBook() throws RecordNotFoundException {
        System.out.println("\n-------------- RETURN LIBRARY BOOK --------------");
        List<BookLoan> active = loans.stream().filter(BookLoan::isActive).toList();
        if (active.isEmpty()) {
            throw new RecordNotFoundException("There is no book currently on loan. Borrow a book first (option 11).");
        }
        BookLoan loan = selectOne("CHOOSE LOAN TO RETURN", active,
                l -> tag(l) + " | borrowed " + l.getBorrowedDate() + ", due " + l.getDueDate());
        if (loan == null) {
            System.out.println("Return cancelled. Stock unchanged.");
            return;
        }
        LocalDate d = null;
        while (d == null) {
            String s = prompt("Return date (LocalDate, YYYY-MM-DD) [CANCEL to abort]: ");
            if (s.equalsIgnoreCase("CANCEL") || s.equals("0")) {
                System.out.println("Return cancelled. Stock unchanged.");
                return;
            }
            try {
                LocalDate t = Registrable.parseDate("Return date", s);
                Registrable.requireNotFuture("Return date", t);
                if (t.isBefore(loan.getBorrowedDate())) {
                    throw new ValidationException("Return date cannot be before the borrowed date (" + loan.getBorrowedDate() + ").");
                }
                d = t;
            } catch (ValidationException e) {
                System.out.println("! " + e.getMessage());
            }
        }
        LibraryBook b = loan.getBook();
        System.out.println("Return " + loan.getRegistrationSummary() + " on " + d + "?");
        if (!confirm("Confirm return?")) {
            System.out.println("Return cancelled. Stock unchanged.");
            return;
        }
        try {
            if (b.getAvailableCopies() >= b.getTotalCopies()) {
                throw new ValidationException("Stock is already full for '" + b.getTitle() + "'; return refused to avoid a wrong count.");
            }
            loan.setReturnedDate(d);
            b.setAvailableCopies(b.getAvailableCopies() + 1); // stock +1, once
            System.out.println("+ Loan " + loan.getId() + " returned" + (d.isAfter(loan.getDueDate()) ? " (LATE)" : " on time")
                    + ". '" + b.getTitle() + "' now has " + b.getAvailableCopies() + "/" + b.getTotalCopies() + " copies available.");
        } catch (ValidationException e) {
            System.out.println("! " + e.getMessage());
        }
    }

    private static void section(String title, List<? extends Registrable> list) {
        System.out.println("\n=== " + title + " (" + list.size() + ") ===");
        if (list.isEmpty()) {
            System.out.println("  (no records registered)");
            return;
        }
        for (int i = 0; i < list.size(); i++) System.out.println("  " + (i + 1) + ". " + list.get(i).getRegistrationSummary());
    }

    private static void viewAll() {
        section("DEPARTMENTS", departments);
        section("STUDENTS", students);
        section("TEACHERS", teachers);
        section("ADMINISTRATORS", administrators);
        section("COURSES", courses);
        section("CLASSROOMS", classrooms);
        section("ENROLLMENTS", enrollments);
        section("ASSESSMENTS", assessments);
        section("FEE PAYMENTS", payments);
        section("LIBRARY BOOKS", books);
        section("BOOK LOANS", loans);
        prompt("\nPress ENTER to return to the main menu...");
    }

    private static void searchById() throws RecordNotFoundException, ValidationException {
        String id = Registrable.requireText("ID", prompt("Enter the record ID to search: "), 1, ID_MAX);
        Identifiable found = all().stream().filter(x -> x.getId().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new RecordNotFoundException("No record with ID '" + id + "' exists."));
        System.out.println("Found: " + found.getClass().getSimpleName() + " " + tag(found) + (found.isActive() ? " (active)" : " (inactive)"));
        ((Registrable) found).printDetails();
    }

    private static <E extends Identifiable & Registrable> void updateOf(String noun, List<E> list, Function<E, Spec<E>> mk) {
        if (list.isEmpty()) {
            System.out.println("No " + noun + " records exist. Nothing to update.");
            return;
        }
        E chosen = selectOne("CHOOSE " + noun.toUpperCase() + " TO UPDATE", list, Main::tag);
        if (chosen == null) {
            System.out.println("Update cancelled.");
            return;
        }
        Spec<E> sp = mk.apply(chosen);
        banner(sp, "UPDATE");
        for (Field<E> f : sp.fields) f.value = f.getter == null ? null : f.getter.apply(chosen);
        review(sp, chosen);
    }

    private static void updateRecord() {
        System.out.println("\n--------------- UPDATE EXISTING RECORD ---------------");
        System.out.println(" 1 Department   2 Student    3 Teacher   4 Administrator");
        System.out.println(" 5 Course       6 Classroom  7 Enrollment 8 Assessment");
        System.out.println(" 9 Fee payment 10 Library book 11 Book loan (due date only)   0 Back");
        switch (menuChoice("Select record type: ", 0, 11)) {
            case 1 -> updateOf("Department", departments, Main::departmentSpec);
            case 2 -> updateOf("Student", students, Main::studentSpec);
            case 3 -> updateOf("Teacher", teachers, Main::teacherSpec);
            case 4 -> updateOf("Administrator", administrators, Main::administratorSpec);
            case 5 -> updateOf("Course", courses, Main::courseSpec);
            case 6 -> updateOf("Classroom", classrooms, Main::classroomSpec);
            case 7 -> updateOf("Enrollment", enrollments, Main::enrollmentSpec);
            case 8 -> updateOf("Assessment", assessments, Main::assessmentSpec);
            case 9 -> updateOf("Fee payment", payments, Main::paymentSpec);
            case 10 -> updateOf("Library book", books, Main::bookSpec);
            case 11 -> updateOf("Book loan", loans, Main::loanSpec);
            default -> System.out.println("Back to main menu.");
        }
    }

    private static Student chooseStudent(String purpose) throws PrerequisiteMissingException {
        if (students.isEmpty()) {
            throw new PrerequisiteMissingException("No student is registered. Register a student (option 2) before " + purpose + ".");
        }
        return selectOne("CHOOSE STUDENT", students, Main::tag);
    }

    private static void studentSummary() throws PrerequisiteMissingException {
        Student s = chooseStudent("viewing an academic summary");
        if (s == null) return;
        System.out.println("\n========== ACADEMIC SUMMARY ==========");
        System.out.println(s.getRegistrationSummary());
        System.out.println("Department: " + s.getDepartment().getDepartmentName() + " | Program: " + s.getProgramName()
                + " | Year: " + s.getStudyYear() + " | Scholarship: " + (s.isScholarshipStatus() ? "YES" : "NO"));
        double sum = 0;
        int count = 0;
        List<Enrollment> mine = enrollments.stream().filter(e -> e.getStudent() == s).toList();
        if (mine.isEmpty()) System.out.println("\nNo enrollments recorded for this student.");
        for (Enrollment e : mine) {
            System.out.println("\n" + e.getCourse().getCourseCode() + " " + e.getCourse().getCourseTitle() + " ("
                    + e.getCourse().getCreditHours() + " cr) - " + e.getSemester() + " - " + e.getEnrollmentStatus());
            List<Assessment> as = assessments.stream().filter(a -> a.getEnrollment() == e).toList();
            if (as.isEmpty()) System.out.println("   (no assessments yet)");
            for (Assessment a : as) {
                double pct = a.getObtainedScore() * 100.0 / a.getMaximumScore();
                System.out.printf("   %-11s %6.2f / %-6.2f  %5.1f%%  (%s)%n", a.getAssessmentType(), a.getObtainedScore(),
                        a.getMaximumScore(), pct, a.getAssessmentDate());
                sum += pct;
                count++;
            }
        }
        System.out.println("\nAssessments recorded : " + count);
        System.out.println("Average percentage   : " + (count == 0 ? "n/a" : String.format("%.1f%%", sum / count)));
        BigDecimal paid = payments.stream().filter(p -> p.getStudent() == s).map(FeePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Total fees paid      : " + paid.toPlainString());
        System.out.println("Books on loan        : " + loans.stream().filter(l -> l.getStudent() == s && l.isActive()).count());
    }

    private static void paymentHistory() throws PrerequisiteMissingException {
        Student s = chooseStudent("viewing a payment history");
        if (s == null) return;
        List<FeePayment> mine = payments.stream().filter(p -> p.getStudent() == s)
                .sorted(Comparator.comparing(FeePayment::getPaymentDate)).toList();
        System.out.println("\n========== PAYMENT HISTORY: " + s.getDisplayName() + " ==========");
        if (mine.isEmpty()) {
            System.out.println("No payments recorded for this student.");
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (FeePayment p : mine) {
            System.out.println(" " + p.getPaymentDate() + "  " + p.getAmount().toPlainString() + "  " + p.getPaymentMethod()
                    + "  receipt " + p.getReceiptNumber() + "  [" + p.getPaymentId() + "]");
            total = total.add(p.getAmount());
        }
        System.out.println(" TOTAL PAID: " + total.toPlainString() + " (" + mine.size() + " payments)");
    }

    private static void availableBooks() {
        List<LibraryBook> av = books.stream().filter(LibraryBook::isActive).toList();
        System.out.println("\n========== AVAILABLE BOOKS ==========");
        if (av.isEmpty()) {
            System.out.println(books.isEmpty() ? "No book is registered yet (use option 10)." : "All books are currently on loan.");
            return;
        }
        for (LibraryBook b : av) System.out.println(" " + b.getRegistrationSummary());
    }

    private static String chain(Class<?> c) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (sb.length() > 0) sb.append(" -> ");
            sb.append(k.getSimpleName());
        }
        return sb.toString();
    }

    private static void demonstratePolymorphism() {
        System.out.println("\n========== RUNTIME POLYMORPHISM ==========");
        List<Person> people = new ArrayList<>();
        people.addAll(students);
        people.addAll(teachers);
        people.addAll(administrators);
        System.out.println("--- 1) List<Person>: same call, different overridden behaviour ---");
        if (people.isEmpty()) {
            System.out.println("No Student, Teacher or Administrator is registered yet. Register some (options 2, 3, 4) and try again.");
        }
        for (Person p : people) {
            System.out.println(p.getClass().getSimpleName() + ": " + p.getRoleDescription() + " | name=" + p.getDisplayName()
                    + " | active=" + p.isActive());
            if (p instanceof AcademicMember am) System.out.println("   getAcademicCategory() -> " + am.getAcademicCategory());
            System.out.println("   inheritance chain     -> " + chain(p.getClass()));
        }
        System.out.println("\n--- 2) List<Identifiable>: unrelated classes used through one interface ---");
        List<Identifiable> ids = all();
        if (ids.isEmpty()) System.out.println("(no records registered yet)");
        for (Identifiable i : ids) {
            System.out.println(String.format("%-14s", i.getClass().getSimpleName()) + " id=" + i.getId() + " | " + i.getDisplayName()
                    + " | active=" + i.isActive());
        }
        System.out.println("\n--- 3) Why Person and AcademicMember cannot be instantiated ---");
        System.out.println("Both are abstract: Person declares getRoleDescription() and AcademicMember declares getAcademicCategory()");
        System.out.println("without bodies, so 'new Person(...)' would be an object with missing behaviour; the compiler forbids it.");
        System.out.println("They exist to share state/validation and force concrete subclasses (Student, Teacher, Administrator) to complete them.");
    }

    
    private static void printMenu() {
        String[] left = {"Register Department", "Register Student", "Register Teacher", "Register Administrator",
                "Register Course", "Register Classroom", "Enroll Student", "Record Assessment",
                "Record Fee Payment", "Register Library Book"};
        String[] right = {"Borrow Library Book", "Return Library Book", "View All Records", "Search Record by ID",
                "Update Existing Record", "Student Academic Summary", "Student Payment History",
                "View Available Books", "Demonstrate Polymorphism", "Exit"};
        System.out.println("\n============== AUCA_ID SCHOOL MANAGEMENT ==============");
        for (int i = 0; i < 10; i++) {
            System.out.println(String.format("%2d. %-26s %2d. %s", i + 1, left[i], i == 9 ? 0 : i + 11, right[i]));
        }
    }

    public static void main(String[] args) {
        System.out.println("Welcome to AUCA_ID - School Management System");
        boolean running = true;
        while (running) {
            printMenu();
            try {
                switch (menuChoice("Select an option: ", 0, 19)) {
                    case 1 -> run(() -> departmentSpec(null));
                    case 2 -> run(() -> studentSpec(null));
                    case 3 -> run(() -> teacherSpec(null));
                    case 4 -> run(() -> administratorSpec(null));
                    case 5 -> run(() -> courseSpec(null));
                    case 6 -> run(() -> classroomSpec(null));
                    case 7 -> run(() -> enrollmentSpec(null));
                    case 8 -> run(() -> assessmentSpec(null));
                    case 9 -> run(() -> paymentSpec(null));
                    case 10 -> run(() -> bookSpec(null));
                    case 11 -> run(() -> loanSpec(null));
                    case 12 -> returnBook();
                    case 13 -> viewAll();
                    case 14 -> searchById();
                    case 15 -> updateRecord();
                    case 16 -> studentSummary();
                    case 17 -> paymentHistory();
                    case 18 -> availableBooks();
                    case 19 -> demonstratePolymorphism();
                    case 0 -> {
                        System.out.println("Goodbye - AUCA_ID closed.");
                        running = false;
                    }
                    default -> System.out.println("! Unknown option.");
                }
            } catch (InputClosed e) {
                System.out.println("\nInput closed. Goodbye.");
                running = false;
            } catch (RecordNotFoundException | PrerequisiteMissingException | ValidationException e) {
                System.out.println("! " + e.getMessage());
            } catch (Exception e) { 
                System.out.println("! Unexpected problem (" + e.getClass().getSimpleName() + "): " + e.getMessage()
                        + ". Returning to the main menu.");
            }
        }
    }
}
