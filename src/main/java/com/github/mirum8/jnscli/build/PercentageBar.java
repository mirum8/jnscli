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
        percentage = Math.min(MAX_PROGRESS, Math.max(0, percentage));
        int doneSize = percentage / (MAX_PROGRESS / PROGRESS_BAR_SIZE);
        int remainsSize = PROGRESS_BAR_SIZE - doneSize;

        String done = generateProgressSegment(doneSize, DONE_MARKER, percentage < MAX_PROGRESS ? TextColor.YELLOW : TextColor.GREEN);
        String remains = generateProgressSegment(remainsSize, REMAINS_MARKER, TextColor.CYAN);

        return String.format("%s%s%s%s %3d%% %s", LEFT_DELIMITER, done, remains, RIGHT_DELIMITER, percentage, statusMessage);
    }

    private static String generateProgressSegment(int size, String marker, TextColor color) {
        char[] segmentChars = new char[size];
        Arrays.fill(segmentChars, marker.charAt(0));
        return colored(new String(segmentChars), color);
    }
}
