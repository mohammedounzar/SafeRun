package com.example.saferun.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateTimeUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

    /**
     * Formats a Date object to a readable date string (e.g., "Jan 01, 2023")
     */
    public static String formatDate(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }

    /**
     * Formats a Date object to a readable time string (e.g., "12:30 PM")
     */
    public static String formatTime(Date date) {
        if (date == null) return "";
        return TIME_FORMAT.format(date);
    }

    /**
     * Formats a Date object to a readable date-time string (e.g., "Jan 01, 2023 at 12:30 PM")
     */
    public static String formatDateTime(Date date) {
        if (date == null) return "";
        return DATE_TIME_FORMAT.format(date);
    }

    /**
     * Formats a timestamp into a relative time string (e.g., "2 hours ago", "just now", etc.)
     */
    public static String formatRelativeTime(Date date) {
        if (date == null) return "";

        long timeDifferenceMillis = new Date().getTime() - date.getTime();
        long diffSeconds = TimeUnit.MILLISECONDS.toSeconds(timeDifferenceMillis);
        long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(timeDifferenceMillis);
        long diffHours = TimeUnit.MILLISECONDS.toHours(timeDifferenceMillis);
        long diffDays = TimeUnit.MILLISECONDS.toDays(timeDifferenceMillis);

        if (diffSeconds < 60) {
            return "just now";
        } else if (diffMinutes < 60) {
            return diffMinutes + " minute" + (diffMinutes > 1 ? "s" : "") + " ago";
        } else if (diffHours < 24) {
            return diffHours + " hour" + (diffHours > 1 ? "s" : "") + " ago";
        } else if (diffDays < 7) {
            return diffDays + " day" + (diffDays > 1 ? "s" : "") + " ago";
        } else {
            return formatDate(date);
        }
    }

    /**
     * Formats a timestamp for display in team requests (e.g., "Requested on Jan 01")
     */
    public static String formatTimestamp(Date timestamp) {
        if (timestamp == null) return "";

        long now = new Date().getTime();
        long timeDiff = now - timestamp.getTime();

        // If it's today
        if (timeDiff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(timeDiff);
            if (hours < 1) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff);
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
            }
            return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        }

        // If it's within the last week
        if (timeDiff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(timeDiff);
            return days + " day" + (days != 1 ? "s" : "") + " ago";
        }

        // Otherwise, show the date
        SimpleDateFormat format = new SimpleDateFormat("Requested on MMM d", Locale.getDefault());
        return format.format(timestamp);
    }

    /**
     * Formats milliseconds to readable duration (e.g., "1h 30m 45s")
     */
    public static String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.HOURS.toMillis(hours));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes));

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append("h ");
        }

        if (minutes > 0 || hours > 0) {
            result.append(minutes).append("m ");
        }

        result.append(seconds).append("s");

        return result.toString();
    }
}