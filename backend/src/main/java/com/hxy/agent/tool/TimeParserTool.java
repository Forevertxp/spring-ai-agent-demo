package com.hxy.agent.tool;

import com.hxy.agent.entity.TimeRange;
import com.hxy.agent.entity.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TimeParserTool {

    public TimeRange parseTime(String timeExpression) {
        LocalDate today = LocalDate.now();
        log.info("解析时间表达式: {}", timeExpression);

        if (timeExpression.contains("本月")) {
            return new TimeRange(
                    today.withDayOfMonth(1).toString(),
                    today.toString(),
                    TimeUnit.MONTH
            );
        }

        if (timeExpression.contains("上个月") || timeExpression.contains("上月")) {
            LocalDate lastMonthStart = today.minusMonths(1).withDayOfMonth(1);
            LocalDate lastMonthEnd = today.withDayOfMonth(1).minusDays(1);
            return new TimeRange(
                    lastMonthStart.toString(),
                    lastMonthEnd.toString(),
                    TimeUnit.MONTH
            );
        }

        if (timeExpression.contains("最近") || timeExpression.contains("近")) {
            Pattern pattern = Pattern.compile("(最近|近)(\\d+)个?(月|周|天)");
            Matcher matcher = pattern.matcher(timeExpression);
            if (matcher.find()) {
                int count = Integer.parseInt(matcher.group(2));
                String unitStr = matcher.group(3);

                if (unitStr.equals("月")) {
                    LocalDate start = today.minusMonths(count);
                    return new TimeRange(start.toString(), today.toString(), TimeUnit.MONTH);
                } else if (unitStr.equals("周")) {
                    LocalDate start = today.minusWeeks(count);
                    return new TimeRange(start.toString(), today.toString(), TimeUnit.WEEK);
                } else if (unitStr.equals("天")) {
                    LocalDate start = today.minusDays(count);
                    return new TimeRange(start.toString(), today.toString(), TimeUnit.DAY);
                }
            }
        }

        if (timeExpression.contains("季度") || timeExpression.contains("Q")) {
            return parseQuarter(timeExpression, today);
        }

        if (timeExpression.contains("今年")) {
            return new TimeRange(
                    today.withDayOfYear(1).toString(),
                    today.toString(),
                    TimeUnit.YEAR
            );
        }

        if (timeExpression.contains("去年")) {
            LocalDate lastYearStart = today.minusYears(1).withDayOfYear(1);
            LocalDate lastYearEnd = today.withDayOfYear(1).minusDays(1);
            return new TimeRange(
                    lastYearStart.toString(),
                    lastYearEnd.toString(),
                    TimeUnit.YEAR
            );
        }

        if (timeExpression.contains("本周")) {
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            return new TimeRange(weekStart.toString(), today.toString(), TimeUnit.WEEK);
        }

        if (timeExpression.contains("上周")) {
            LocalDate lastWeekStart = today.minusWeeks(1).minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate lastWeekEnd = lastWeekStart.plusDays(6);
            return new TimeRange(lastWeekStart.toString(), lastWeekEnd.toString(), TimeUnit.WEEK);
        }

        return TimeRange.unknown();
    }

    private TimeRange parseQuarter(String expression, LocalDate today) {
        int year = today.getYear();
        int quarter = 1;

        Pattern qPattern = Pattern.compile("Q(\\d)|第(\\d)季度");
        Matcher matcher = qPattern.matcher(expression);
        if (matcher.find()) {
            quarter = Integer.parseInt(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }

        if (expression.contains("今年")) {
            year = today.getYear();
        } else if (expression.contains("去年")) {
            year = today.getYear() - 1;
        } else if (expression.contains("上季度") || expression.contains("上个季度")) {
            quarter = (today.getMonthValue() - 1) / 3 + 1;
            if (quarter == 1) {
                quarter = 4;
                year = year - 1;
            } else {
                quarter = quarter - 1;
            }
        }

        LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);

        return new TimeRange(start.toString(), end.toString(), TimeUnit.QUARTER);
    }

    public String getDescription() {
        return """
                时间解析工具，将相对时间转换为具体日期：
                - 支持: 本月、上个月、最近N个月、Q1/第一季度、今年、去年、本周、上周

                示例：
                - "上个月" → 2026-04-01 到 2026-04-30
                - "最近三个月" → 2026-02-16 到 2026-05-16
                - "Q1" → 2026-01-01 到 2026-03-31
                """;
    }
}