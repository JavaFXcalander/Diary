package com.taskmanager.timeline;

import java.time.LocalTime;

public class TimeUtil {

    public static final LocalTime TIMELINE_VIEW_START_HOUR = LocalTime.of(5, 0); // Timeline view starts at 5 AM
    public static final int TIMELINE_DURATION_HOURS = 19; // Timeline shows 23 hours (e.g., 5 AM to 4 AM next day)

    /**
     * Converts a LocalTime to a Y-coordinate on the timeline pane.
     *
     * @param time       The LocalTime to convert.
     * @param paneHeight The total height of the pane available for drawing the timeline.
     * @return The Y-coordinate.
     */
    public static double toY(LocalTime time, double paneHeight) {
        long secondsIntoTimelineView;
        long timeOfDaySeconds = time.toSecondOfDay();
        long timelineStartSeconds = TIMELINE_VIEW_START_HOUR.toSecondOfDay();

        
    
        // Time is between TIMELINE_VIEW_START_HOUR and 23:59:59
        secondsIntoTimelineView = timeOfDaySeconds - timelineStartSeconds;


        double proportion = (double) secondsIntoTimelineView / (TIMELINE_DURATION_HOURS * 3600.0);
        // Ensure proportion is within [0, 1] in case of rounding or edge issues, though logic should handle it.
        proportion = Math.max(0, Math.min(1, proportion)); 
        return proportion * paneHeight;
    }
}
