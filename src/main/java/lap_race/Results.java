package lap_race;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class Results {

    ////////////////////////////////////////////  SET UP  ////////////////////////////////////////////
    //                                                                                              //
    //  1. If a time was recorded for any leg that should be treated as a DNF, e.g. if the runners  //
    //     reported that they missed a checkpoint, insert an additional dummy entry into the times  //
    //     file, following the recorded time, with the relevant bib number and time 00:00           //
    //                                                                                              //
    //  2. Edit the details in the following section as appropriate                                 //

    // format for mass start times
    //                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String DNF_STRING = "DNF";
    public static final String DNS_STRING = "DNS";
    private static final String NO_MASS_STARTS = "23:59:59,23:59:59,23:59:59,23:59:59";
    private static final String OVERALL_RESULTS_HEADER = "Pos,No,Team,Category,";

    //////////////////////////////////////////////////////////////////////////////////////////////////

    static Duration ZERO_TIME = RawResult.parseTime("0:0");

    private static final String COMMENT_PREFIX = "//";
    public static final Duration DNF_DUMMY_LEG_TIME = RawResult.parseTime("23:59:59");

    // Read from configuration file.
    Path working_directory_path;
    String entries_filename;
    String raw_results_filename;
    String year;
    int number_of_legs;
    String race_name_for_results;
    String race_name_for_filenames;

    // Derived.
    String overall_results_filename;
    String detailed_results_filename;
    String prizes_filename;

    Path input_directory_path;
    Path entries_path;
    Path raw_results_path;

    Path output_directory_path;
    Path overall_results_path;
    Path detailed_results_path;
    Path prizes_path;

    Team[] entries;
    RawResult[] raw_results;
    OverallResult[] results;
    Set<Team> prize_winners = new HashSet<>();

    Duration[] start_times_for_mass_starts;  // Relative to start of leg 1.

    boolean[] paired_legs;

    public Results(String config_file_path) throws IOException {

        this(loadProperties(config_file_path));
    }

    public Results(Properties properties) {

        loadConfiguration(properties);
    }

    public static void main(String[] args) throws IOException {

        // Path to configuration file should be first argument.

        if (args.length < 1)
            System.out.println("usage: java Results <config file path>");
        else
            new Results(args[0]).processResults();
    }

    private static Properties loadProperties(String config_file_path) throws IOException {

        try (FileInputStream in = new FileInputStream(config_file_path)) {

            Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    private void loadConfiguration(Properties properties) {

        working_directory_path = Paths.get(properties.getProperty("WORKING_DIRECTORY"));

        entries_filename = properties.getProperty("ENTRIES_FILENAME");
        raw_results_filename = properties.getProperty("RAW_RESULTS_FILENAME");
        year = properties.getProperty("YEAR");
        number_of_legs = Integer.parseInt(properties.getProperty("NUMBER_OF_LEGS"));
        race_name_for_results = properties.getProperty("RACE_NAME_FOR_RESULTS");
        race_name_for_filenames = properties.getProperty("RACE_NAME_FOR_FILENAMES");

        String paired_legs_string = properties.getProperty("PAIRED_LEGS");
        String dnf_legs_string = properties.getProperty("DNF_LEGS");
        String mass_start_elapsed_times_string = properties.getProperty("MASS_START_ELAPSED_TIMES");

        if (mass_start_elapsed_times_string.isBlank()) mass_start_elapsed_times_string = NO_MASS_STARTS;

        start_times_for_mass_starts = new Duration[number_of_legs];
        int leg = 0;
        for (String time_as_string : mass_start_elapsed_times_string.split(",")) {
            start_times_for_mass_starts[leg++] = RawResult.parseTime(time_as_string);
        }

        paired_legs = new boolean[number_of_legs];
        for (String s : paired_legs_string.split(",")) {
            paired_legs[Integer.parseInt(s) - 1] = true;
        }

//        if (!dnf_legs_string.isBlank()) Collections.addAll(dnf_legs, dnf_legs_string.split(","));

        overall_results_filename = race_name_for_filenames + "_overall_" + year + ".csv";
        detailed_results_filename = race_name_for_filenames + "_detailed_" + year + ".csv";
        prizes_filename = race_name_for_filenames + "_prizes_" + year + ".txt";

        input_directory_path = working_directory_path.resolve("input");
        entries_path = input_directory_path.resolve(entries_filename);
        raw_results_path = input_directory_path.resolve(raw_results_filename);

        output_directory_path = working_directory_path.resolve("output");
        overall_results_path = output_directory_path.resolve(overall_results_filename);
        detailed_results_path = output_directory_path.resolve(detailed_results_filename);
        prizes_path = output_directory_path.resolve(prizes_filename);
    }

    public void processResults() throws IOException {

        loadEntries();
        loadRawResults();
        initialiseResults();

        calculateResults();

//        calculateLegResults(); // Need to calculate leg results as they're used in following calculations.
//        calculateOverallResults();

        printOverallResults();
        printDetailedResults();
        printLegResults();
        printPrizes();
    }

    public void printOverallResults() throws IOException {

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(overall_results_path))) {

            printOverallResultsHeader(writer);
            printOverallResults(writer);
//            printDNFs(false, writer);
//            printDNSs(writer);
        }
    }

    private void printOverallResults(final OutputStreamWriter writer) throws IOException {

        for (int i = 0; i < results.length; i++) {
            writer.append(String.valueOf(i+1)).append(",").append(String.valueOf(results[i])).append("\n");
        }
    }

    private void loadEntries() throws IOException {

        List<String> lines = Files.readAllLines(entries_path);
        entries = new Team[lines.size()];

        int i = 0;
        for (String line : lines) {
            String[] elements = line.split("\t");
            entries[i++] = new Team(elements);
        }
    }

    private void loadRawResults() throws IOException {

        List<String> lines = Files.readAllLines(raw_results_path);
        raw_results = new RawResult[lines.size()];

        for (int i = 0; i < raw_results.length; i++) {
            raw_results[i] = new RawResult(lines.get(i));
        }
    }

    private void initialiseResults() {

        results = new OverallResult[entries.length];

        for (int i = 0; i < results.length; i++) {
            results[i] = new OverallResult(entries[i], number_of_legs);
        }
    }

    private void printOverallResultsHeader(OutputStreamWriter writer) throws IOException {

        writer.append(OVERALL_RESULTS_HEADER).append("Total\n");
    }

    private void calculateResults() {

        for (RawResult raw_result : raw_results) {

            int index = findIndexOfTeamWithBibNumber(raw_result.bib_number);
            OverallResult result = results[index];
            addNextLegResult(result, raw_result);
        }
    }

    private void addNextLegResult(OverallResult result, RawResult raw_result) {

        LegResult[] leg_results = result.leg_results;
        int index_of_next_leg_result = findIndexOfNextLegResult(leg_results);

        Duration leg_start_time = index_of_next_leg_result < 1 ? ZERO_TIME : leg_results[index_of_next_leg_result - 1].finish_time;

        LegResult leg_result = new LegResult();

        leg_result.team = result.team;
        leg_result.leg_number = index_of_next_leg_result + 1;
        leg_result.leg_duration = raw_result.recorded_time.minus(leg_start_time);
        leg_result.finish_time = raw_result.recorded_time;

        leg_results[index_of_next_leg_result] = leg_result;

        result.overall_duration = result.overall_duration.plus(leg_result.leg_duration);
    }

    private int findIndexOfNextLegResult(LegResult[] leg_results) {

        for (int i = 0; i < leg_results.length; i++) {
            if (leg_results[i] == null) return i;
        }

        return -1;
    }

    private int findIndexOfTeamWithBibNumber(int bib_number) {

        for (int i = 0; i < results.length; i++) {
            if (results[i].team.bib_number == bib_number) return i;
        }
        return -1;
    }


//    public void printEntries() {
//
//        for (Map.Entry<Integer, Team> entry : entries.entrySet()) {
//            System.out.println("team " + entry.getKey() + ": " + entry.getValue());
//        }
//    }
//
//    public void printRawResults() {
//
//        for (RawResult result : raw_results) {
//            System.out.println(result);
//        }
//    }

    public void printDetailedResults() throws IOException {

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(detailed_results_path))) {

            printDetailedResultsHeader(writer);
            printDetailedResults(writer);
//            printDNFs(true, writer);
//            printDNSs(writer);
        }
    }

    private void printDetailedResults(final OutputStreamWriter writer) throws IOException {

        int position = 1;

        for (OverallResult result : results) {

            Team team = result.team;

            int bib_number = team.bib_number;

            writer.append(String.valueOf(position++)).append(",");
            writer.append(String.valueOf(bib_number)).append(",");
            writer.append(team.name).append(",");
            writer.append(team.category.toString()).append(",");

            for (int leg = 1; leg <= number_of_legs; leg++) {

                writer.append(team.runners[leg - 1]).append(",");

                LegResult leg_result = result.leg_results[leg - 1];

                writer.append(OverallResult.format(leg_result.leg_duration)).append(",");
                writer.append(OverallResult.format(leg_result.finish_time));

                if (leg < number_of_legs) writer.append(",");
            }

            writer.append("\n");
        }
    }

    public void printLegResults() throws IOException {

        for (int leg = 1; leg <= number_of_legs; leg++) {

            Path leg_results_path = output_directory_path.resolve(race_name_for_filenames + "_leg_" + leg + "_" + year + ".csv");

            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(leg_results_path))) {

                printLegResultsHeader(leg, writer);

                LegResult[] leg_results = new LegResult[results.length];

                for (int i = 0; i < leg_results.length; i++) {
                    leg_results[i] = results[i].leg_results[leg - 1];
                }

                Arrays.sort(leg_results);

                for (int i = 0; i < leg_results.length; i++) {
//                    System.out.println(String.valueOf(i + 1) + leg_results[i].toString() + "\n");
                    writer.append(String.valueOf(i + 1)).append(",").append(leg_results[i].toString()).append("\n");
                }
            }
        }



//        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(leg_results_path))) {
//
//            int max_completions_in_any_leg = 0;
//            for (int leg = 1; leg <= number_of_legs; leg++) {
//                int completions_for_leg = countValidLegResults(leg_results.get(leg - 1));
//                if (max_completions_in_any_leg < completions_for_leg) max_completions_in_any_leg = completions_for_leg;
//            }
//
//            printLegResultsHeader(writer);
//
//            for (int pos = 1; pos <= max_completions_in_any_leg; pos++) {
//
//                writer.append(String.valueOf(pos));
//
//                for (int leg = 1; leg <= number_of_legs; leg++) {
//
//                    final List<LegResult> results_for_leg = leg_results.get(leg - 1);
//
//                    if (results_for_leg.size() >= pos) {
//                        LegResult leg_result = results_for_leg.get(pos - 1);
//                        if (!leg_result.DNF) {
//                            Team team = entries.get(leg_result.bib_number);
//                            writer.append(",").append(team.runners[leg - 1]).append(",").append(OverallResult.format(leg_result.leg_duration));
//                        } else {
//                            writer.append(",,");
//                        }
//                    } else {
//                        writer.append(",,");
//                    }
//                }
//
//                writer.append("\n");
//            }
//        }
    }

//    private int countValidLegResults(final List<LegResult> leg_results) {
//
//        int count = 0;
//        for (LegResult leg_result : leg_results) {
//            if (!leg_result.DNF) count++;
//        }
//        return count;
//    }

    public void printPrizes() throws IOException {

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(prizes_path))) {

            writer.append(race_name_for_results).append(" Results ").append(year).append("\n");
            writer.append("============================\n\n");

            for (Category category : Arrays.asList(
                    Category.FEMALE_SENIOR, Category.OPEN_SENIOR,
                    Category.FEMALE_40, Category.OPEN_40,
                    Category.FEMALE_50, Category.OPEN_50,
                    Category.FEMALE_60, Category.OPEN_60,
                    Category.MIXED_SENIOR, Category.MIXED_40)) {

                printPrizes(category, writer);
            }
        }
    }

    private void printPrizes(final Category category, final OutputStreamWriter writer) throws IOException {

        int number_of_prizes = category.getNumberOfPrizes();

        writer.append("Category: ").append(String.valueOf(category)).append("\n");
        printDashes(category.toString().length() + 10, writer);
        writer.append("\n");

        int position = 1;
        OverallResult[] prize_results = new OverallResult[number_of_prizes];

        for (OverallResult result : results) {

            if (categoryMatch(category, position, result) && !prize_winners.contains(result.team) && position <= number_of_prizes) {

                prize_results[position - 1] = result;

                prize_winners.add(result.team);
                position++;
            }
        }

        for (int i = 0; i < number_of_prizes; i++) {

            OverallResult prize_result = prize_results[i];
            if (prize_result != null)
                writer.append(String.valueOf(i + 1)).append(",").append(String.valueOf(prize_result)).append("\n");
        }

        writer.append("\n\n");
    }

    private boolean categoryMatch(final Category category, final int position, final OverallResult result) {

        // Only check for an older category being included in this category when looking for first prize,
        // since the first in an older category always gets that prize rather than a lower senior prize.

        // This won't work in the case where the top two teams are women or mixed...

        return category == result.team.category || (position == 1 && category.includes(result.team.category));
    }

    private void printDashes(final int n, final OutputStreamWriter writer) throws IOException {

        for (int i = 0; i < n; i++) writer.append("-");
        writer.append("\n");
    }

//    private void printDNFs(boolean include_leg_details, OutputStreamWriter writer) throws IOException {
//
//        for (Map.Entry<Integer, Team> entry : entries.entrySet()) {
//
//            final int bib_number = entry.getKey();
//            final int legs_completed = countLegsCompleted(bib_number);
//            if (legs_completed > 0 && legs_completed < number_of_legs) {
//
//                Team team = entry.getValue();
//                writer.append(",").append(String.valueOf(bib_number)).append(",").append(team.getName());
//                writer.append(",").append(String.valueOf(team.getCategory())).append(",");
//
//                if (include_leg_details) {
//                    extracted(bib_number, team, writer);
//                    writer.append("\n");
//                }
//                else
//                    writer.append("DNF\n");
//            }
//        }
//    }

//    private void extracted(int bib_number, Team team, OutputStreamWriter writer) throws IOException {
//
//        boolean an_earlier_leg_was_DNF = false;
//
//        for (int leg = 1; leg <= number_of_legs; leg++) {
//
//            writer.append(team.runners[leg - 1]).append(",");
//
//            try {
//                final LegResult leg_result = findLegResult(bib_number, leg - 1);
//
//                if (!leg_result.DNF) {
//                    writer.append(OverallResult.format(leg_result.leg_duration)).append(",");
//                    writer.append(an_earlier_leg_was_DNF ? DNF_STRING : OverallResult.format(leg_result.adjusted_split_time));
//                } else {
//                    writer.append(DNF_STRING).append(",").append(DNF_STRING);
//                    an_earlier_leg_was_DNF = true;
//                }
//            } catch (RuntimeException e) {
//                writer.append(DNF_STRING).append(",").append(DNF_STRING);
//            }
//
//            if (leg < number_of_legs) writer.append(",");
//        }
//    }

//    private void printDNSs(OutputStreamWriter writer) throws IOException {
//
//        for (Map.Entry<Integer, Team> entry : entries.entrySet()) {
//
//            final int bib_number = entry.getKey();
//
//            if (countLegsCompleted(bib_number) == 0) {
//
//                Team team = entry.getValue();
//                writer.append(",").append(String.valueOf(bib_number)).append(",").
//                        append(team.getName()).append(",").append(String.valueOf(team.getCategory())).
//                        append(",").append(DNS_STRING).append("\n");
//            }
//        }
//    }

//    private int countLegsCompleted(final int bib_number) {
//
//        int count = 0;
//
//        for (int leg_index = 0; leg_index < number_of_legs; leg_index++) {
//
//            final LegResult leg_result = findLegResult(bib_number, leg_index);
//            if (leg_result != null && !leg_result.DNF) count++;
//        }
//
//        return count;
//    }

    private void printDetailedResultsHeader(OutputStreamWriter writer) throws IOException {

        writer.append(OVERALL_RESULTS_HEADER);

        for (int leg = 1; leg <= number_of_legs; leg++) {
            writer.append("Runners ").append(String.valueOf(leg)).append(",Leg ").append(String.valueOf(leg)).append(",");
            if (leg < number_of_legs) writer.append("Split ").append(String.valueOf(leg)).append(",");
        }

        writer.append("Total\n");
    }

    private void printLegResultsHeader(int leg, OutputStreamWriter writer) throws IOException {

        // Pos,Runner,Time
        writer.append("Pos,Runner");
        if (paired_legs[leg - 1]) writer.append("s");
        writer.append(",Time\n");
    }

//    private void calculateLegResults() {
//
//        for (int i = 0; i < number_of_legs; i++) {
//            leg_results.add(new ArrayList<>());
//        }
//
//        for (RawResult raw_result : raw_results) {
//
//            int leg_index = findEarliestLegWithoutNumberRecorded(raw_result.bib_number);
//
//            final List<LegResult> this_leg_results = leg_results.get(leg_index);
//
//            // Do we need to store all 3 times? Combine adjustedSplitTime and adjustmentForFinishingAfterNextLegMassStart?
//            final Duration adjusted_split_time = adjustedSplitTime(raw_result, leg_index);
//            final Duration adjustment_for_finishing_after_next_leg_mass_start = adjustmentForFinishingAfterNextLegMassStart(raw_result, leg_index);
//            final Duration leg_time = calculateLegTime(raw_result, leg_index);
//
//            final LegResult leg_result = new LegResult(
//                    leg_index + 1,
//                    raw_result.bib_number,
//                    raw_result.recorded_time,
//                    adjusted_split_time,
//                    adjustment_for_finishing_after_next_leg_mass_start,
//                    leg_time,
//                    entries);
//
//            this_leg_results.add(leg_result);
//        }
//
//        for (String dnf_leg : dnf_legs) {
//
//            String[] split = dnf_leg.split(":");
//            int bib_number = Integer.parseInt(split[0]);
//            int leg_number = Integer.parseInt(split[1]);
//
//            LegResult leg_result = findLegResult(bib_number, leg_number - 1);
//            if (leg_result == null) throw new RuntimeException("non-existent DNF leg: " + bib_number + ", " + leg_number);
//            leg_result.DNF = true;
//            leg_result.leg_duration = DNF_DUMMY_LEG_TIME;
//        }
//
//        // Sort the results within each leg.
//        for (int i = 0; i < number_of_legs; i++) {
//            leg_results.get(i).sort(Comparator.comparing(o -> o.leg_time));
//        }
//    }

//    private void calculateOverallResults() {
//
//        for (int bib_number : entries.keySet()) {
//
//            Duration overall_time = ZERO_TIME;
//            boolean all_legs_completed = true;
//
//            for (int leg_index = 0; leg_index < number_of_legs; leg_index++) {
//
//                final LegResult leg_result = findLegResult(bib_number, leg_index);
//                if (leg_result == null || leg_result.DNF)
//                    all_legs_completed = false;
//                else
//                    overall_time = overall_time.plus(leg_result.leg_duration);
//            }
//
//            if (all_legs_completed)
//                overall_results.add(new OverallResult(bib_number, overall_time, entries));
//        }
//
//        overall_results.sort(Comparator.comparing(o -> o.overall_time));
//    }

//    private Duration adjustmentForFinishingAfterNextLegMassStart(final RawResult raw_result, final int leg_index) {
//
//        if (leg_index == number_of_legs - 1) return ZERO_TIME;   // This leg is last leg.
//
//        Duration difference = raw_result.recorded_time.minus(start_times_for_mass_starts.get(leg_index + 1));
//
//        return difference.isNegative() ? ZERO_TIME : difference;
//    }

//    private Duration adjustedSplitTime(final RawResult raw_result, final int leg_index) {
//
//        Duration adjusted_split_time = raw_result.recorded_time;
//
//        // Add on mass start adjustments from all previous legs.
//        for (int i = 0; i < leg_index; i++) {
//            LegResult earlier_leg_result = findLegResult(raw_result.bib_number, i);
//            adjusted_split_time = adjusted_split_time.plus(earlier_leg_result.adjustment_for_finishing_after_next_leg_mass_start);
//        }
//
//        return adjusted_split_time;
//    }

//    private Duration calculateLegTime(final RawResult raw_result, final int lap_index) {
//
//        Duration previous_lap_recorded_split_time = ZERO_TIME;
//
//        if (lap_index > 0) {
//
//            LegResult previous_lap_result = findLegResult(raw_result.bib_number, lap_index - 1);
//            previous_lap_recorded_split_time = previous_lap_result.recorded_split_time;
//        }
//
//        Duration lap_time = raw_result.recorded_time.minus(previous_lap_recorded_split_time);
//
//        // Add on mass start adjustment from previous lap.
//        if (lap_index > 0) {
//            LegResult earlier_lap_result = findLegResult(raw_result.bib_number, lap_index - 1);
//            lap_time = lap_time.plus(earlier_lap_result.adjustment_for_finishing_after_next_leg_mass_start);
//        }
//
//        return lap_time;
//    }

//    private LegResult findLegResult(final int bib_number, int lap_index) {
//
//        final List<LegResult> this_lap_results = leg_results.get(lap_index);
//        for (LegResult lap_result : this_lap_results) {
//            if (lap_result.bib_number == bib_number) return lap_result;
//        }
//        return null;
//    }

    // Find the earliest lap that doesn't yet have this bib number recorded.
//    private int findEarliestLegWithoutNumberRecorded(final int bib_number) {
//
//        for (int lap_index = 0; lap_index < number_of_legs; lap_index++) {
//            if (!bibNumberRecorded(bib_number, lap_index)) return lap_index;
//        }
//
//        throw new RuntimeException("surplus result recorded for team: " + bib_number + getDescriptionOfTeamsMissingResults());
//    }

//    private String getDescriptionOfTeamsMissingResults() {
//
//        StringBuilder builder = new StringBuilder();
//
//        Map<Integer, Integer> lap_counts = new HashMap<>();
//        for (RawResult raw_result : raw_results) {
//            lap_counts.putIfAbsent(raw_result.bib_number, 0);
//            lap_counts.put(raw_result.bib_number, lap_counts.get(raw_result.bib_number) + 1);
//        }
//
//        for (Map.Entry<Integer, Team> entry : entries.entrySet()) {
//
//            final int bib_number = entry.getKey();
//            final int laps_completed = lap_counts.get(bib_number);
//
//            if (laps_completed < number_of_legs) {
//                if (!builder.isEmpty()) builder.append(", ");
//                builder.append(bib_number);
//            }
//        }
//
//        if (builder.isEmpty()) {
//            builder.append("; no teams missing results");
//        }
//        else {
//            builder = new StringBuilder("; team(s) missing results: ").append(builder);
//        }
//        return builder.toString();
//    }

//    private boolean bibNumberRecorded(final int bib_number, final int lap_index) {
//
//        for (LegResult lap_result : leg_results.get(lap_index)) {
//            if (lap_result.bib_number == bib_number) return true;
//        }
//        return false;
//    }

//    static class PrizeResult {
//        String position_string;
//        OverallResult result;
//        PrizeResult(String position_string, OverallResult result) {
//            this.position_string = position_string;
//            this.result = result;
//        }
//    }
}

// code for dead heats
//        int number_of_results = overall_results.size();
//
//        for (int i = 0; i < number_of_results; i++) {
//
//            OverallResult result = overall_results.get(i);
//            int j = i;
//
//            while (j + 1 < number_of_results && result.overall_time.equals(overall_results.get(j + 1).overall_time)) j++;
//            if (j > i) {
//                for (int k = i; k <= j; k++)
//                    overall_results.get(k).position_string = i + 1 + "=";
//                i = j;
//            }
//            else
//                result.position_string = String.valueOf(i + 1);
//        }