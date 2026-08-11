package tech.aiflowy.common.constant.enums;

import tech.aiflowy.common.annotation.DictDef;

@DictDef(name = "系统变量", code = "systemVariable", keyField = "code", labelField = "text")
public enum EnumSystemVariable {

    TODAY("today", "今天"),
    YESTERDAY("yesterday", "昨天"),
    TOMORROW("tomorrow", "明天"),
    CURRENT_DATE("currentDate", "当前日期"),
    CURRENT_TIME("currentTime", "当前时间"),
    CURRENT_DATETIME("currentDatetime", "当前日期时间"),
    NOW("now", "现在"),
    THIS_WEEK("thisWeek", "本周"),
    LAST_WEEK("lastWeek", "上周"),
    NEXT_WEEK("nextWeek", "下周"),
    THIS_MONTH("thisMonth", "本月"),
    LAST_MONTH("lastMonth", "上月"),
    NEXT_MONTH("nextMonth", "下月"),
    THIS_YEAR("thisYear", "本年"),
    LAST_YEAR("lastYear", "去年"),
    NEXT_YEAR("nextYear", "明年"),
    TIMESTAMP("timestamp", "时间戳"),
    MILLISECONDS("milliseconds", "毫秒数"),
    WEEK_DAY("weekDay", "星期几"),
    DAY_OF_MONTH("dayOfMonth", "本月第几天"),
    MONTH("month", "月份"),
    YEAR("year", "年份"),
    ;

    private final String code;
    private final String text;

    EnumSystemVariable(String code, String text) {
        this.code = code;
        this.text = text;
    }

    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
}
