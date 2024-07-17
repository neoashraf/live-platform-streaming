package com.tanvir.core.util;

import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

@Component
@Slf4j
public class DateTimeUtil {

    public String convertDateToString(Date date, String dateFormat) {
        DateFormat formatter = new SimpleDateFormat(dateFormat);
        String formattedDate = formatter.format(date);
        return formattedDate;
    }

    public LocalDateTime convertLocalDateObjectIntoLocalDateTime(Date dateToConvert) {
        return dateToConvert.toInstant().atZone(ZoneId.of("Asia/Dhaka"))
                .toLocalDateTime();
    }

    public LocalDateTime convertStringToLocalDateTime(String date, String dateFormat) throws ExceptionHandlerUtil {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat);
        return LocalDateTime.parse(date, dtf);
    }

    public Date convertStringToDate(String date, String dateFormat) throws ExceptionHandlerUtil {
        if (date == null)
            return new Date();

        DateFormat formatter = new SimpleDateFormat(dateFormat);
        Date formattedDate = null;
        try {
            formattedDate = formatter.parse(date);
        } catch (ParseException e) {
            throw new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Unable to parse date");
        }
        return formattedDate;
    }

    public String convertStringToFormattedDateString(String date, String fromDateFormat, String toDateFormat) throws ExceptionHandlerUtil {
        SimpleDateFormat format1 = new SimpleDateFormat(fromDateFormat);
        SimpleDateFormat format2 = new SimpleDateFormat(toDateFormat);
        Date convertedDate = null;
        try {
            convertedDate = format1.parse(date);
        } catch (ParseException e) {
            throw new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Unable to parse date");
        }
        return format2.format(convertedDate);
    }

    public Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    public Timestamp formatTimeStamp(String dateTime, String format) {
        try {
            return Timestamp.valueOf(convertStringToLocalDateTime(dateTime, format));
        } catch (ExceptionHandlerUtil e) {
            log.error("error occurred while convert date to timeStamp");
            return null;
        }
    }

    public String convertDateStringToFormattedDateString(String date, String fromDateFormat, String toDateFormat) {
        SimpleDateFormat format1 = new SimpleDateFormat(fromDateFormat);
        SimpleDateFormat format2 = new SimpleDateFormat(toDateFormat);
        Date convertedDate;
        try {
            convertedDate = format1.parse(date);
        } catch (ParseException e) {
            return null;
        }
        return format2.format(convertedDate);
    }
}
