package ru.edu.pr02;

import java.util.Arrays;

/**
 * Анализ строки табеля за рабочую неделю (5 значений через «;»).
 * Класс без состояния: все методы статические.
 *
 * Вариант 1.
 *   Часть А: average возвращает МЕДИАНУ (3-й элемент отсортированной копии массива).
 *   Часть Б: addProductivityLevel возвращает текстовую оценку продуктивности по результату average.
 */
public class TimesheetAnalyzer {

    private static final int DAYS = 5;
    private static final int MIN_HOURS = 0;
    private static final int MAX_HOURS = 16;
    private static final int DAILY_NORMAL_HOURS = 8;
    private static final int WEEKLY_NORMAL_HOURS = 40;
    private static final double LOW_LEVEL_LIMIT = 6.0;
    private static final double HIGH_LEVEL_LIMIT = 9.0;

    /**
     * R1–R9. Единственная точка валидации строкового ввода.
     * Fail-fast: при первой ошибке бросает IllegalArgumentException с позицией (с 1).
     */
    public static int[] parseHours(String line) {
        String[] parts = splitLine(line);
        int[] result = new int[DAYS];
        for (int i = 0; i < DAYS; i++) {
            result[i] = parseValue(parts[i], i + 1);
        }
        return result;
    }

    /** R1: после split(";", -1) должно быть ровно 5 элементов. */
    private static String[] splitLine(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Ошибка: структура – ожидалось 5 значений, получено 0");
        }
        String[] parts = line.split(";", -1);
        if (parts.length != DAYS) {
            throw new IllegalArgumentException(
                    "Ошибка: структура – ожидалось 5 значений, получено " + parts.length);
        }
        return parts;
    }

    /** R2, R3, R9: формат без ведущих нулей и знака, затем диапазон [0, 16]. */
    private static int parseValue(String text, int position) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Ошибка: позиция " + position + " – пустое значение");
        }
        if (!text.matches("^(0|[1-9][0-9]*)$")) {
            throw new IllegalArgumentException("Ошибка: позиция " + position
                    + " – значение \"" + text + "\" не является целым числом");
        }
        // Больше двух цифр после regex означает число не меньше 100: вне диапазона, parseInt не нужен.
        if (text.length() > 2 || Integer.parseInt(text) > MAX_HOURS) {
            throw new IllegalArgumentException("Ошибка: позиция " + position
                    + " – значение " + text + " вне диапазона [" + MIN_HOURS + ", " + MAX_HOURS + "]");
        }
        return Integer.parseInt(text);
    }

    /** Защитные условия: программные контракты, а не проверка пользовательского ввода. */
    private static void requireWeek(int[] hours) {
        if (hours == null) {
            throw new IllegalArgumentException("hours is null");
        }
        if (hours.length != DAYS) {
            throw new IllegalArgumentException("hours length must be 5, got " + hours.length);
        }
    }

    /** Сумма часов за неделю. */
    public static int total(int[] hours) {
        requireWeek(hours);
        int sum = 0;
        for (int h : hours) {
            sum += h;
        }
        return sum;
    }

    /**
     * Вариант 1, часть А: медиана — 3-й элемент отсортированной КОПИИ массива.
     * Исходный массив не изменяется. Для 5 целых чисел результат целый, но тип double
     * сохранён по сигнатуре; значение выводится с двумя знаками после запятой.
     */
    public static double average(int[] hours) {
        requireWeek(hours);
        int[] sorted = Arrays.copyOf(hours, hours.length);
        Arrays.sort(sorted);
        return sorted[DAYS / 2];
    }

    /** Количество дней с переработкой: значение строго больше 8 (R5). */
    public static int overtimeDays(int[] hours) {
        requireWeek(hours);
        int days = 0;
        for (int h : hours) {
            if (h > DAILY_NORMAL_HOURS) {
                days++;
            }
        }
        return days;
    }

    /** Недельная переработка: max(0, сумма − 40) (R6). Сумма берётся из total (DRY). */
    public static int weeklyOvertime(int[] hours) {
        return Math.max(0, total(hours) - WEEKLY_NORMAL_HOURS);
    }

    /**
     * Вариант 1, часть Б: оценка продуктивности по результату average.
     * Меньше 6 — «низкая», от 6 (включительно) до 9 (не включая) — «средняя», 9 и больше — «высокая».
     */
    public static String addProductivityLevel(int[] hours) {
        double level = average(hours);
        if (level < LOW_LEVEL_LIMIT) {
            return "низкая";
        }
        if (level < HIGH_LEVEL_LIMIT) {
            return "средняя";
        }
        return "высокая";
    }
}
