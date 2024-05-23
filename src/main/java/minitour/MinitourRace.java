package minitour;

import common.Category;
import common.JuniorRaceCategories;
import common.Race;
import individual_race.IndividualRace;
import individual_race.IndividualRaceResult;
import individual_race.Runner;
import lap_race.LapRaceOutputPDF;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MinitourRace extends Race {

    public static final int DEFAULT_CATEGORY_PRIZES = 3;

    ////////////////////////////////////////////  SET UP  ////////////////////////////////////////////
    //                                                                                              //
    //  See README.md at the project root for details of how to configure and run this software.    //
    //                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////

    MinitourRaceInput input;
    MinitourRaceOutput output_CSV, output_HTML, output_text, output_PDF;
    MinitourRacePrizes prizes;

    IndividualRace[] races;
    Runner[] combined_runners;
    MinitourRaceResult[] overall_results;
    Map<Category, List<Runner>> prize_winners = new HashMap<>();

    public int category_prizes;

    //////////////////////////////////////////////////////////////////////////////////////////////////

    public MinitourRace(final Path config_file_path) throws IOException {
        super(config_file_path);
    }

    public static void main(final String[] args) throws IOException {

        // Path to configuration file should be first argument.

        if (args.length < 1)
            System.out.println("usage: java MinitourRace <config file path>");
        else
            new MinitourRace(Paths.get(args[0])).processResults();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void configure() throws IOException {

        readProperties();

        configureHelpers();
        configureCategories();
        configureInputData();
    }

     @Override
    public void processResults() throws IOException {

        initialiseResults();

        calculateResults();
        allocatePrizes();

        printOverallResults();
        printPrizes();
        printCombined();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

    protected void readProperties() {

        super.readProperties();
        category_prizes = Integer.parseInt(getPropertyWithDefault("CATEGORY_PRIZES", String.valueOf(DEFAULT_CATEGORY_PRIZES)));
    }

    private void configureHelpers() {

        input = new MinitourRaceInput(this);

        output_CSV = new MinitourRaceOutputCSV(this);
        output_HTML = new MinitourRaceOutputHTML(this);
        output_text = new MinitourRaceOutputText(this);
        output_PDF = new MinitourRaceOutputPDF(this);

        prizes = new MinitourRacePrizes(this);
    }

    protected void configureCategories() {

        categories = new JuniorRaceCategories(category_prizes);
    }

    private void configureInputData() throws IOException {

        races = input.loadMinitourRaces();
    }

    private void initialiseResults() {

        combined_runners = getCombinedRunners(races);
        overall_results = new MinitourRaceResult[combined_runners.length];
    }

    private void calculateResults() {

        for (int i = 0; i < overall_results.length; i++)
            overall_results[i] = getOverallResult(combined_runners[i]);

        // Ordering defined in MinitourRaceResult: sort by time then by runner name.
        Arrays.sort(overall_results);
    }

    private MinitourRaceResult getOverallResult(final Runner runner) {

        final MinitourRaceResult result = new MinitourRaceResult(runner, this);

        for (int i = 0; i < races.length; i++) {

            final IndividualRace individual_race = races[i];

            if (individual_race != null)
                result.times[i] = getRaceTime(individual_race, runner);
        }

        return result;
    }

    private Duration getRaceTime(final IndividualRace individual_race, final Runner runner) {

        for (IndividualRaceResult result : individual_race.getOverallResults())
            if (result.entry.runner.equals(runner)) return result.duration();

        return null;
    }

    private void allocatePrizes() {

        prizes.allocatePrizes();
    }

    private void printOverallResults() throws IOException {

        output_CSV.printOverallResults();
        output_HTML.printOverallResults();
    }

    private void printPrizes() throws IOException {

        output_PDF.printPrizes();
        output_HTML.printPrizes();
        output_text.printPrizes();
    }

    private void printCombined() throws IOException {

        output_HTML.printCombined();
    }

    public MinitourRaceResult[] getOverallResults() {

        return overall_results;
    }

    public MinitourRaceResult[] getCompletedResultsByCategory(List<Category> categories_required) {

        final Predicate<MinitourRaceResult> category_filter = minitourRaceResult -> minitourRaceResult.completedAllRacesSoFar() && categories_required.contains(minitourRaceResult.runner.category);

        return Stream.of(overall_results).filter(category_filter).toArray(MinitourRaceResult[]:: new);
    }

    public int findIndexOfRunner(Runner runner) {

        for (int i = 0; i < overall_results.length; i++)
            if (runner.equals(overall_results[i].runner)) return i;

        throw new RuntimeException("Runner not found: " + runner.name);
    }
}
