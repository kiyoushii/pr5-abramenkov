package ru.edu.pr02;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Единственная точка входа. Режим 1: unit-проверки в коде. Режим 2: data-driven по data/sample.csv.
 * Режим 3: интерактивный ввод строк табеля (выход: exit или пустая строка).
 */
public class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;

    // ------------------------------------------------------------------ вспомогательные методы

    private static void report(String name, boolean ok, String details) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name + ": " + details);
        }
    }

    private static void checkParse(String name, String line, int[] expected) {
        try {
            int[] actual = TimesheetAnalyzer.parseHours(line);
            report(name, Arrays.equals(actual, expected), Arrays.toString(actual));
        } catch (IllegalArgumentException e) {
            report(name, false, "неожиданная ошибка: " + e.getMessage());
        }
    }

    private static void checkParseError(String name, String line, String expectedPart) {
        try {
            TimesheetAnalyzer.parseHours(line);
            report(name, false, "ожидалась ошибка");
        } catch (IllegalArgumentException e) {
            report(name, e.getMessage().contains(expectedPart), e.getMessage());
        }
    }

    private static void checkInt(String name, int actual, int expected) {
        report(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
    }

    private static void checkDouble(String name, double actual, double expected) {
        report(name, Math.abs(actual - expected) < 1e-9, "ожидалось " + expected + ", получено " + actual);
    }

    private static void checkText(String name, String actual, String expected) {
        report(name, expected.equals(actual), "ожидалось " + expected + ", получено " + actual);
    }

    /** Защитное условие: метод должен бросить IllegalArgumentException с данным фрагментом сообщения. */
    private static void checkGuard(String name, String method, int[] hours, String expectedPart) {
        try {
            switch (method) {
                case "total" -> TimesheetAnalyzer.total(hours);
                case "average" -> TimesheetAnalyzer.average(hours);
                case "overtimeDays" -> TimesheetAnalyzer.overtimeDays(hours);
                case "weeklyOvertime" -> TimesheetAnalyzer.weeklyOvertime(hours);
                default -> TimesheetAnalyzer.addProductivityLevel(hours);
            }
            report(name, false, "ожидалась ошибка");
        } catch (IllegalArgumentException e) {
            report(name, e.getMessage().contains(expectedPart), e.getMessage());
        }
    }

    // ------------------------------------------------------------------ режим 1: unit-проверки

    private static void checkParseHours() {
        checkParse("parseHours typical", "8;9;10;8;8", new int[] {8, 9, 10, 8, 8});
        checkParseError("parseHours empty line", "", "структура");
        checkParseError("parseHours 17 at pos 3", "8;8;17;8;8", "позиция 3");
        checkParseError("parseHours not a number", "8;abc;8;8;8", "позиция 2");
        checkParseError("parseHours four values", "8;8;8;8", "структура");
        checkParseError("parseHours trailing ;", "8;8;8;8;8;", "структура");
        checkParseError("parseHours leading zero", "08;8;8;8;8", "позиция 1");
        checkParseError("parseHours surrounding space", "8; 9;10;8;8", "позиция 2");
        checkParseError("parseHours empty field", "8;;9;10;8", "позиция 2");
        checkParseError("parseHours plus sign", "+8;8;8;8;8", "позиция 1");
        checkParseError("parseHours negative", "-3;8;8;8;8", "позиция 1");
        checkParseError("parseHours fraction", "8;8.5;8;8;8", "позиция 2");
        checkParseError("parseHours huge value", "8;8;8;8;100", "позиция 5");
        checkParseError("parseHours null", null, "структура");
        checkParse("parseHours bounds 0 and 16", "0;16;0;16;0", new int[] {0, 16, 0, 16, 0});
    }

    private static void checkTotal() {
        checkInt("total typical", TimesheetAnalyzer.total(new int[] {8, 9, 10, 8, 8}), 43);
        checkInt("total lower bound", TimesheetAnalyzer.total(new int[] {0, 0, 0, 0, 0}), 0);
        checkInt("total upper bound", TimesheetAnalyzer.total(new int[] {16, 16, 16, 16, 16}), 80);
        checkGuard("total null", "total", null, "hours is null");
    }

    /** Вариант 1, часть А: average возвращает медиану. */
    private static void checkAverage() {
        checkDouble("average typical (median)", TimesheetAnalyzer.average(new int[] {8, 9, 10, 8, 8}), 8.0);
        checkDouble("average equal values", TimesheetAnalyzer.average(new int[] {8, 8, 8, 8, 8}), 8.0);
        checkDouble("average symmetric", TimesheetAnalyzer.average(new int[] {7, 8, 9, 8, 7}), 8.0);
        checkDouble("average median differs from mean", TimesheetAnalyzer.average(new int[] {0, 0, 12, 12, 12}), 12.0);
        checkDouble("average unsorted", TimesheetAnalyzer.average(new int[] {1, 16, 2, 15, 3}), 3.0);
        int[] source = {3, 1, 2, 5, 4};
        TimesheetAnalyzer.average(source);
        report("average does not change input", Arrays.equals(source, new int[] {3, 1, 2, 5, 4}), Arrays.toString(source));
        checkGuard("average wrong length", "average", new int[] {8, 8, 8}, "length must be 5");
    }

    private static void checkOvertime() {
        checkInt("overtimeDays none", TimesheetAnalyzer.overtimeDays(new int[] {8, 8, 8, 8, 8}), 0);
        checkInt("overtimeDays two", TimesheetAnalyzer.overtimeDays(new int[] {9, 8, 7, 10, 8}), 2);
        checkInt("overtimeDays boundary 8", TimesheetAnalyzer.overtimeDays(new int[] {8, 9, 8, 8, 8}), 1);
        checkInt("weeklyOvertime sum 40", TimesheetAnalyzer.weeklyOvertime(new int[] {8, 8, 8, 8, 8}), 0);
        checkInt("weeklyOvertime sum 50", TimesheetAnalyzer.weeklyOvertime(new int[] {10, 10, 10, 10, 10}), 10);
        checkInt("weeklyOvertime sum below 40", TimesheetAnalyzer.weeklyOvertime(new int[] {6, 6, 6, 6, 6}), 0);
        checkInt("weeklyOvertime sum 80", TimesheetAnalyzer.weeklyOvertime(new int[] {16, 16, 16, 16, 16}), 40);
        checkGuard("overtimeDays null", "overtimeDays", null, "hours is null");
        checkGuard("weeklyOvertime wrong length", "weeklyOvertime", new int[] {8, 8, 8, 8, 8, 8}, "got 6");
    }

    /** Вариант 1, часть Б: addProductivityLevel. */
    private static void checkProductivity() {
        checkText("productivity low", TimesheetAnalyzer.addProductivityLevel(new int[] {3, 4, 5, 6, 7}), "низкая");
        checkText("productivity boundary 6", TimesheetAnalyzer.addProductivityLevel(new int[] {6, 6, 6, 6, 6}), "средняя");
        checkText("productivity middle", TimesheetAnalyzer.addProductivityLevel(new int[] {8, 8, 8, 8, 8}), "средняя");
        checkText("productivity boundary 9", TimesheetAnalyzer.addProductivityLevel(new int[] {9, 9, 9, 9, 9}), "высокая");
        checkText("productivity high", TimesheetAnalyzer.addProductivityLevel(new int[] {10, 10, 10, 10, 10}), "высокая");
        checkText("productivity median ignores outliers", TimesheetAnalyzer.addProductivityLevel(new int[] {0, 0, 0, 16, 16}), "низкая");
        checkGuard("productivity null", "addProductivityLevel", null, "hours is null");
    }

    private static void checkConsistency() {
        int[][] samples = {{8, 8, 8, 8, 8}, {10, 10, 10, 10, 10}, {0, 0, 0, 0, 0}, {16, 9, 7, 8, 11}};
        boolean ok = true;
        for (int[] s : samples) {
            ok = ok && TimesheetAnalyzer.weeklyOvertime(s) == Math.max(0, TimesheetAnalyzer.total(s) - 40);
        }
        report("weeklyOvertime == max(0, total - 40)", ok, "несогласованность метрик");
    }

    // ------------------------------------------------------------------ режим 2: data/sample.csv

    /** Формат: вход|OK:sum=S,avg=A.AA,od=O,wo=W[,pl=уровень] или ERROR:позиция N / ERROR:структура|комментарий */
    private static String actualMetrics(String line) {
        int[] h = TimesheetAnalyzer.parseHours(line);
        return String.format(Locale.US, "sum=%d,avg=%.2f,od=%d,wo=%d,pl=%s",
                TimesheetAnalyzer.total(h), TimesheetAnalyzer.average(h), TimesheetAnalyzer.overtimeDays(h),
                TimesheetAnalyzer.weeklyOvertime(h), TimesheetAnalyzer.addProductivityLevel(h));
    }

    private static boolean metricsMatch(String actual, String expected) {
        // expected может не содержать pl=: тогда сравниваем без него
        String trimmed = expected.contains(",pl=") ? actual : actual.substring(0, actual.indexOf(",pl="));
        return trimmed.equals(expected);
    }

    private static void runDataLine(String line, int n) {
        String[] parts = line.split("\\|", -1);
        String input = parts[0].equals("EMPTY") ? "" : parts[0];
        String expected = parts[1];
        String name = "data/" + n + " (" + parts[2] + ")";
        try {
            String actual = actualMetrics(input);
            boolean ok = expected.startsWith("OK:") && metricsMatch(actual, expected.substring(3));
            report(name, ok, "получено " + actual + ", ожидалось " + expected);
        } catch (IllegalArgumentException e) {
            boolean ok = expected.startsWith("ERROR:") && e.getMessage().contains(expected.substring(6));
            report(name, ok, "получено «" + e.getMessage() + "», ожидалось " + expected);
        }
    }

    private static void runDataDriven() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("data/sample.csv"));
        int before = passed;
        int n = 0;
        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            n++;
            runDataLine(line, n);
        }
        System.out.println("Data-driven: " + (passed - before) + "/" + n + " passed");
    }

    // ------------------------------------------------------------------ режим 3: интерактив

    private static void printReport(int[] h) {
        System.out.println("Сумма: " + TimesheetAnalyzer.total(h));
        System.out.println(String.format(Locale.US, "Среднее (медиана): %.2f", TimesheetAnalyzer.average(h)));
        System.out.println("Дней с переработкой: " + TimesheetAnalyzer.overtimeDays(h));
        System.out.println("Недельная переработка: " + TimesheetAnalyzer.weeklyOvertime(h));
        System.out.println("Уровень продуктивности: " + TimesheetAnalyzer.addProductivityLevel(h));
    }

    private static void runInteractive() {
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("Строка табеля (exit или пустая строка — выход): ");
            if (!in.hasNextLine()) {
                break;
            }
            String line = in.nextLine();
            if (line.isEmpty() || line.equals("exit")) {
                break;
            }
            try {
                printReport(TimesheetAnalyzer.parseHours(line));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ точка входа

    public static void main(String[] args) throws IOException {
        checkParseHours();
        checkTotal();
        checkAverage();
        checkOvertime();
        checkProductivity();
        checkConsistency();
        System.out.println("Unit: " + passed + "/" + (passed + failed) + " passed");
        runDataDriven();
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED: " + failed);
        if (failed != 0) {
            System.exit(1);
        }
        runInteractive();
    }
}
