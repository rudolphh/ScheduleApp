package utils;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeChanger {

    private static final ZoneId utc = ZoneId.of("UTC");
    private static final ZoneId local = ZoneId.systemDefault();

    public static String makeDateString(int year, int month, int day){
        DecimalFormat formatter = new DecimalFormat("00");
        return year + "-" + formatter.format(month) + "-" + formatter.format(day) + " 00:00:00";
    }

    // take a String, and according to the DateTime format given, convert to LocalDateTime
    public static LocalDateTime ldtFromString(String ldtStr, String pattern){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(ldtStr, formatter);
    }

    // convert Timestamp in UTC from DB to LocalDateTime for use in the program
    public static LocalDateTime utcToLocal(Timestamp timestamp){
        // convert to local date time without zone information
        LocalDateTime localDateTime = timestamp.toLocalDateTime();

        // attach UTC time zone information
        ZonedDateTime utcZoned = localDateTime.atZone(utc);

        // change zone to local to get local time offset from UTC
        ZonedDateTime localZoned = utcZoned.withZoneSameInstant(local);

        return localZoned.toLocalDateTime();// return the local time
    }

    // convert LocalDateTime to UTC Timestamp for DB
    public static Timestamp localToUtc(LocalDateTime localDateTime){

        ZonedDateTime localZoned = localDateTime.atZone(local);// attach local time zone information
        ZonedDateTime utcZoned = localZoned.withZoneSameInstant(utc);// switch to UTC zone

        return Timestamp.valueOf(utcZoned.toLocalDateTime());// return timestamp in UTC
    }
}
