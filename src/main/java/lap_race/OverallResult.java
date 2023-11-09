package lap_race;

import java.time.Duration;

public class OverallResult implements Comparable<OverallResult> {

    Team team;
    LegResult[] leg_results;

    public OverallResult(Team team, int number_of_legs) {

        this.team = team;
        leg_results = new LegResult[number_of_legs];

        for (int i = 0; i < number_of_legs; i++) {

            leg_results[i] = new LegResult();
            leg_results[i].team = team;
            leg_results[i].leg_number = i + 1;
            leg_results[i].DNF = true;
        }
    }

    public Duration duration() {

        Duration overall = Results.ZERO_TIME;

        for (LegResult leg_result : leg_results) {
            if (leg_result.DNF) return Results.DNF_DUMMY_LEG_TIME;
            overall = overall.plus(leg_result.duration());
        }

        return overall;
    }

    public boolean dnf() {

        for (LegResult leg_result : leg_results)
            if (leg_result.DNF) return true;

        return false;
    }

    public String toString() {
        return team.bib_number + "," + team.name + "," + team.category + "," + (dnf() ? "DNF" : format(duration()));
    }

    @Override
    public int compareTo(OverallResult o) {
        return duration().compareTo(o.duration());
    }

    public static String format(Duration duration) {

        long s = duration.getSeconds();
        return String.format("0%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }
}
