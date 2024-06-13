package common;

import common.categories.Categories;
import common.categories.Category;
import common.output.RaceOutput;
import individual_race.IndividualRace;
import individual_race.IndividualRaceResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public abstract class Race {

    protected static final String DUMMY_DURATION_STRING = "23:59:59";
    protected static final String ZERO_TIME_STRING = "0:0:0";
    public static final Duration DUMMY_DURATION = parseTime(DUMMY_DURATION_STRING);

    protected final Properties properties;
    private final Path working_directory_path;

    // String read from configuration file specifying all the runners who did have a finish
    // time recorded but were declared DNF.
    protected String dnf_string;

    protected List<RawResult> raw_results;
    public Map<Category, List<RaceResult>> prize_winners = new HashMap<>();
    protected List<RaceResult> overall_results;

    public Categories categories;
    protected RacePrizes prizes;

    public RaceInput input;
    public RaceOutput output_CSV, output_HTML, output_text, output_PDF;

    public Race(final Path config_file_path) throws IOException {

        working_directory_path = config_file_path.getParent().getParent();
        properties = readProperties(config_file_path);

        configure();
    }

    protected abstract void configure() throws IOException;

    public abstract void processResults() throws IOException;

    public Properties getProperties() {
        return properties;
    }

    private static Properties readProperties(final Path config_file_path) throws IOException {

        try (final FileInputStream in = new FileInputStream(config_file_path.toString())) {

            final Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    public List<RaceResult> getOverallResults() {
        return overall_results;
    }

    public Category lookupCategory(final String short_name) {

        for (final Category category : categories.getCategoriesInDecreasingGeneralityOrder())
            if (category.getShortName().equals(short_name)) return category;

        throw new RuntimeException("Category not found: " + short_name);
    }

    protected void readProperties() {

        dnf_string = properties.getProperty("DNF_LEGS");
    }

    public Path getWorkingDirectoryPath() {
        return working_directory_path;
    }

    public List<RawResult> getRawResults() {
        return raw_results;
    }

    public String getPropertyWithDefault(final String property_key, final String default_value) {

        final String value = properties.getProperty(property_key);
        return value == null || value.isBlank() ? default_value : value;
    }

    public static List<Runner> getCombinedRunners(final List<IndividualRace> individual_races) {

        final List<Runner> runners = new ArrayList<>();

        for (final IndividualRace individual_race : individual_races)
            if (individual_race != null)
                for (final RaceResult result : individual_race.getOverallResults()) {
                    final Runner runner = ((IndividualRaceResult)result).entry.runner;
                    if (!runners.contains(runner))
                        runners.add(runner);
                }

        return runners;
    }

    public List<RaceResult> getResultsByCategory(List<Category> ignore) {

        return overall_results;
    }

    public record CategoryGroup(String combined_categories_title, List<String> category_names){};

    public List<CategoryGroup> getResultCategoryGroups() {

        return List.of(new CategoryGroup("Everything", List.of()));
    }

    public static Duration parseTime(final String element) {

        try {
            final String[] parts = element.strip().split(":");
            final String time_as_ISO = "PT" + hours(parts) + minutes(parts) + seconds(parts);

            return Duration.parse(time_as_ISO);
        }
        catch (Exception e) {
            throw new RuntimeException("illegal time: " + element);
        }
    }

    public static String getFirstName(final String name) {
        return name.split(" ")[0];
    }

    public static String getLastName(final String name) {
        final String[] names = name.split(" ");
        return names[names.length - 1];
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
}
