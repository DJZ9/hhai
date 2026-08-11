package tech.aiflowy.common.util;

import tech.aiflowy.common.constant.enums.EnumSystemVariable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class SystemVariableUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getValue(String variableCode) {
        if (variableCode == null || variableCode.isEmpty()) {
            return "";
        }

        return switch (variableCode) {
            case "today" -> LocalDate.now().format(DATE_FORMATTER);
            case "yesterday" -> LocalDate.now().minusDays(1).format(DATE_FORMATTER);
            case "tomorrow" -> LocalDate.now().plusDays(1).format(DATE_FORMATTER);
            case "currentDate" -> LocalDate.now().format(DATE_FORMATTER);
            case "currentTime" -> LocalTime.now().format(TIME_FORMATTER);
            case "currentDatetime" -> LocalDateTime.now().format(DATETIME_FORMATTER);
            case "now" -> LocalDateTime.now().format(DATETIME_FORMATTER);
            case "thisWeek" -> getThisWeekRange();
            case "lastWeek" -> getLastWeekRange();
            case "nextWeek" -> getNextWeekRange();
            case "thisMonth" -> getThisMonthRange();
            case "lastMonth" -> getLastMonthRange();
            case "nextMonth" -> getNextMonthRange();
            case "thisYear" -> String.valueOf(LocalDate.now().getYear());
            case "lastYear" -> String.valueOf(LocalDate.now().minusYears(1).getYear());
            case "nextYear" -> String.valueOf(LocalDate.now().plusYears(1).getYear());
            case "timestamp" -> String.valueOf(System.currentTimeMillis() / 1000);
            case "milliseconds" -> String.valueOf(System.currentTimeMillis());
            case "weekDay" -> getWeekDay();
            case "dayOfMonth" -> String.valueOf(LocalDate.now().getDayOfMonth());
            case "month" -> String.valueOf(LocalDate.now().getMonthValue());
            case "year" -> String.valueOf(LocalDate.now().getYear());
            case "uuid" -> java.util.UUID.randomUUID().toString().replace("-", "");
            case "randomNumber" -> String.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000));
            default -> "";
        };
    }

    public static String getValue(EnumSystemVariable variable) {
        if (variable == null) {
            return "";
        }
        return getValue(variable.getCode());
    }
    
    /**
     * 获取所有系统变量
     * @return Map，key为变量名，value为变量值
     */
    public static Map<String, Object> getAllVariables() {
        Map<String, Object> variables = new HashMap<>();
        for (EnumSystemVariable variable : EnumSystemVariable.values()) {
            variables.put(variable.getCode(), getValue(variable));
        }
        return variables;
    }

    private static String getThisWeekRange() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate sunday = monday.plusDays(6);
        return monday.format(DATE_FORMATTER) + " 至 " + sunday.format(DATE_FORMATTER);
    }

    private static String getLastWeekRange() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.minusDays(now.getDayOfWeek().getValue() + 6);
        LocalDate sunday = monday.plusDays(6);
        return monday.format(DATE_FORMATTER) + " 至 " + sunday.format(DATE_FORMATTER);
    }

    private static String getNextWeekRange() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.plusDays(8 - now.getDayOfWeek().getValue());
        LocalDate sunday = monday.plusDays(6);
        return monday.format(DATE_FORMATTER) + " 至 " + sunday.format(DATE_FORMATTER);
    }

    private static String getThisMonthRange() {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.withDayOfMonth(1);
        LocalDate lastDay = now.withDayOfMonth(now.lengthOfMonth());
        return firstDay.format(DATE_FORMATTER) + " 至 " + lastDay.format(DATE_FORMATTER);
    }

    private static String getLastMonthRange() {
        LocalDate now = LocalDate.now().minusMonths(1);
        LocalDate firstDay = now.withDayOfMonth(1);
        LocalDate lastDay = now.withDayOfMonth(now.lengthOfMonth());
        return firstDay.format(DATE_FORMATTER) + " 至 " + lastDay.format(DATE_FORMATTER);
    }

    private static String getNextMonthRange() {
        LocalDate now = LocalDate.now().plusMonths(1);
        LocalDate firstDay = now.withDayOfMonth(1);
        LocalDate lastDay = now.withDayOfMonth(now.lengthOfMonth());
        return firstDay.format(DATE_FORMATTER) + " 至 " + lastDay.format(DATE_FORMATTER);
    }

    private static String getWeekDay() {
        String[] weeks = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        return weeks[dayOfWeek % 7];
    }

}