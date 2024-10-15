package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.shell.TextColor;

import java.util.Arrays;

import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

public class PercentageBar {

    private static final int MAX_PROGRESS = 100;
    private static final int PROGRESS_BAR_SIZE = 10;
    private static final String DONE_MARKER = "█";
    private static final String REMAINS_MARKER = "░";
    private static final String LEFT_DELIMITER = "[";
    private static final String RIGHT_DELIMITER = "]";

    private PercentageBar() {
    }

    public static String of(int percentage, String statusMessage) {
        return generateBar(percentage, statusMessage, false);
    }

    public static String error(int percentage, String statusMessage) {
        return generateBar(percentage, statusMessage, true);
    }

    private static String generateBar(int percentage, String statusMessage, boolean isError) {
        percentage = Math.min(MAX_PROGRESS, Math.max(0, percentage));
        int doneSize = percentage / (MAX_PROGRESS / PROGRESS_BAR_SIZE);
        int remainsSize = PROGRESS_BAR_SIZE - doneSize;

        TextColor doneColor = getDoneColor(percentage, isError);

        String done = generateProgressSegment(doneSize, DONE_MARKER, doneColor);
        String remains = generateProgressSegment(remainsSize, REMAINS_MARKER, TextColor.CYAN);

        return formatBar(done, remains, String.format("%3d%%", percentage), statusMessage);
    }

    private static TextColor getDoneColor(int percentage, boolean isError) {
        if (isError) {
            return TextColor.RED;
        } else if (percentage < MAX_PROGRESS) {
            return TextColor.YELLOW;
        } else {
            return TextColor.GREEN;
        }
    }

    private static String formatBar(String done, String remains, String percentage, String statusMessage) {
        return String.format("%s%s%s%s %s %s", LEFT_DELIMITER, done, remains, RIGHT_DELIMITER, percentage, statusMessage);
    }

    private static String generateProgressSegment(int size, String marker, TextColor color) {
        char[] segmentChars = new char[size];
        Arrays.fill(segmentChars, marker.charAt(0));
        return colored(new String(segmentChars), color);
    }
}
