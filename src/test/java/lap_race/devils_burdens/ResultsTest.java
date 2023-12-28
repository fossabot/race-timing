package lap_race.devils_burdens;

import lap_race.Results;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.ac.standrews.cs.utilities.FileManipulation;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ResultsTest {

    // illegal config - dnf lap times, mass start times / mass start times wrong order
    // illegal category
    // raw results not in order
    // annotate detailed results with mass starts
    // generate HTML
    // generate xls
    // test different number of legs

    Properties properties;
    Path resources_expected_outputs;
    Path temp_directory;
    Path temp_output_sub_directory;

    @AfterEach
    public void tearDown() throws IOException {
        //FileManipulation.deleteDirectory(temp_directory);
    }

    @Test
    public void simple() throws Exception {

        processingCompletes("simple");
    }

    @Test
    public void topTwoResultsWomen() throws Exception {

        processingCompletes("top_two_results_women");
    }

    @Test
    public void deadHeats() throws Exception {

        processingCompletes("dead_heats");
    }

    @Test
    public void htmlOutput() throws Exception {

        processingCompletes("html_output");
    }

    @Test
    public void full() throws Exception {

        processingCompletes("full");
    }

    @Test
    public void lastFewResultsNotRecorded() throws Exception {

        processingCompletes("last_few_results_not_recorded");
    }

    @Test
    public void massStartNoneDNFLeg1() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_1");
    }

    @Test
    public void massStartNoneDNFLeg1And2And3() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_1_2_3");
    }

    @Test
    public void massStartNoneDNFButCompleted() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_1_2_3_4a");
    }

    @Test
    public void massStartNoneDNFNotCompleted() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_1_2_3_4b");
    }

    @Test
    public void massStartNoneDNFLeg2() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_2");
    }

    @Test
    public void massStartNoneDNFLeg3() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_3");
    }

    @Test
    public void massStartNoneDNFLeg4() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_4");
    }

    @Test
    public void massStartNoneDNFLeg3And4NotCompleted() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_3_4a");
    }

    @Test
    public void massStartNoneDNFLeg3And4ButCompleted() throws Exception {

        processingCompletes("mass_start_none/dnf_leg_3_4b");
    }

    @Test
    public void massStart_3_4_DNFLeg1() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_1");
    }

    @Test
    public void massStart_3_4_DNFLeg1And2And3() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_1_2_3");
    }

    @Test
    public void massStart_3_4_DNFButCompleted() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_1_2_3_4a");
    }

    @Test
    public void massStart_3_4_DNFNotCompleted() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_1_2_3_4b");
    }

    @Test
    public void massStart_3_4_DNFLeg2() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_2");
    }

    @Test
    public void massStart_3_4_DNFLeg3() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_3");
    }

    @Test
    public void massStart_3_4_DNFLeg3And4NoFinishes() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_3_4a");
    }

    @Test
    public void massStart_3_4_DNFLeg3And4ButCompleted() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_3_4b");
    }

    @Test
    public void massStart_3_4_DNFLeg4() throws Exception {

        processingCompletes("mass_start_3_4/dnf_leg_4");
    }

    @Test
    public void massStart_3_4_FirstLegFinishAfterMassStart3() throws Exception {

        processingCompletes("mass_start_3_4/first_leg_finish_after_mass_start_3");
    }

    @Test
    public void massStart_3_4_FirstLegFinishAfterMassStart4() throws Exception {

        processingCompletes("mass_start_3_4/first_leg_finish_after_mass_start_4");
    }

    @Test
    public void massStart_4_DNFLeg1() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_1");
    }

    @Test
    public void massStart_4_DNFLeg1And2And3() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_1_2_3");
    }

    @Test
    public void massStart_4_DNFButCompleted() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_1_2_3_4a");
    }

    @Test
    public void massStart_4_DNFNotCompleted() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_1_2_3_4b");
    }

    @Test
    public void massStart_4_DNFLeg2() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_2");
    }

    @Test
    public void massStart_4_DNFLeg3() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_3");
    }

    @Test
    public void massStart_4_DNFLeg3And4NoFinishes() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_3_4a");
    }

    @Test
    public void massStart_4_DNFLeg3And4ButCompleted() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_3_4b");
    }

    @Test
    public void massStart_4_DNFLeg4() throws Exception {

        processingCompletes("mass_start_4/dnf_leg_4");
    }

    @Test
    public void unregisteredTeam() throws Exception {

        configureTest("unregistered_team");

        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> new Results(properties).processResults()
        );

        assertEquals("unregistered team: 4", thrown.getMessage());
    }

    @Test
    public void extraResult() throws Exception {

        configureTest("extra_result");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> new Results(properties).processResults()
        );

        assertEquals("surplus result recorded for team: 2", thrown.getMessage());
    }

    @Test
    public void switchedResult() throws Exception {

        configureTest("switched_result");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> new Results(properties).processResults()
        );

        assertEquals("surplus result recorded for team: 2", thrown.getMessage());
    }

    private void processingCompletes(String configuration_name) throws Exception {

        configureTest(configuration_name);
        new Results(properties).processResults();
        assertThatDirectoryContainsAllExpectedContent(resources_expected_outputs, temp_output_sub_directory);
    }

    private void configureTest(String test_resource_root) throws IOException {

        //temp_directory = Files.createTempDirectory(null);
        temp_directory = Paths.get("/Users/gnck/Desktop/temp");

        Path temp_input_sub_directory = Files.createDirectories(temp_directory.resolve("input"));
        temp_output_sub_directory = Files.createDirectories(temp_directory.resolve("output"));

        Path resources_root = Paths.get("src/test/resources/devils_burdens/" + test_resource_root);
        Path resources_config = resources_root.resolve("race.config");
        Path resources_inputs = resources_root.resolve("input");
        resources_expected_outputs = resources_root.resolve("expected");

        copyFilesBetweenDirectories(resources_inputs, temp_input_sub_directory);

        properties = getProperties(resources_config);
        properties.setProperty("WORKING_DIRECTORY", temp_directory.toString());
    }

    private static void copyFilesBetweenDirectories(Path source, Path destination) throws IOException {

        try (Stream<Path> list = Files.list(source)) {
            for (Iterator<Path> iterator = list.iterator(); iterator.hasNext(); ) {
                Path file = iterator.next();
                Files.copy(file, destination.resolve(file.getFileName()));
            }
        }
    }

    private static Properties getProperties(Path path) throws IOException {

        try (FileInputStream in = new FileInputStream(path.toFile())) {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    private static void assertThatDirectoryContainsAllExpectedContent(final Path expected, final Path actual) throws IOException {

        final Set<String> directory_listing_expected = getDirectoryEntries(expected);

        for (final String file_name : directory_listing_expected) {

            if (!file_name.equals(".DS_Store")) {

                final Path path_expected = expected.resolve(file_name);
                final Path path_actual = actual.resolve(file_name);

                if (Files.isDirectory(path_expected)) {
                    assertTrue(Files.isDirectory(path_actual));
                    assertThatDirectoryContainsAllExpectedContent(path_expected, path_actual);
                } else {
                    assertFalse(Files.isDirectory(path_actual));
                    assertThatFilesHaveSameContent(path_expected, path_actual);
                }
            }
        }
    }

    private static Set<String> getDirectoryEntries(final Path directory) throws IOException {

        final Set<String> directory_listing = new HashSet<>();

        try (Stream<Path> entries = Files.list(directory)) {
            for (Iterator<Path> iterator = entries.iterator(); iterator.hasNext(); ) {
                final Path file = iterator.next();
                directory_listing.add(file.getFileName().toString());
            }
        }

        return directory_listing;
    }

    private static void assertThatFilesHaveSameContent(final Path path1, final Path path2) throws IOException {

        byte[] expected = Files.readAllBytes(path1);
        byte[] actual = Files.readAllBytes(path2);

        if (!Arrays.equals(expected, actual) && !filesHaveSameContentIgnoringWhitespace(path1, path2))
            fail("Files differ: " + path1 + ", " + path2);
    }
}
