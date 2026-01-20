package org.example.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class DateUtils {

    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(TIME_FORMATTER);
    }
}
