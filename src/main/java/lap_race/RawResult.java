package lap_race;

import java.time.Duration;

public class RawResult {

     int bib_number;
     Duration recorded_finish_time;  // Relative to start of leg 1.

    public RawResult(String file_line) {

        String[] elements = file_line.split("\t");

        bib_number = Integer.parseInt(elements[0]);
        recorded_finish_time = parseTime(elements[1]);
    }

    static Duration parseTime(final String element) {

        String[] parts = element.split(":");
        String time_as_ISO = "PT" + hours(parts) + minutes(parts) + seconds(parts);
        return Duration.parse(time_as_ISO);
    }

    static String hours(final String[] parts) {

        return parts.length > 2 ? parts[0] + "H" : "";
    }

    static String minutes(final String[] parts) {

        return (parts.length > 2 ? parts[1] : parts[0]) + "M";
    }

    static String seconds(final String[] parts) {

        return (parts.length > 2 ? parts[2] : parts[1]) + "S";
    }

    public String toString() {
        return "bib: " + bib_number + ", time: " + recorded_finish_time;
    }
}
