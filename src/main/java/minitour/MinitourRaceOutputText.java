package minitour;

import com.lowagie.text.Document;
import common.Category;
import fife_ac_races.Minitour;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinitourRaceOutputText extends MinitourRaceOutput {

    public MinitourRaceOutputText(final Minitour race) {
        super(race);
    }

    @Override
    public void printOverallResults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void printCombined() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void printPrizes() throws IOException {

        final Path prizes_text_path = output_directory_path.resolve(prizes_filename + ".txt");

        try (final OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(prizes_text_path))) {

            writer.append(race_name_for_results).append(" Results ").append(year).append("\n");
            writer.append("============================").append("\n\n");

            for (final Category category : race.categories.getCategoriesInReportOrder())
                printPrizes(category, writer);
        }
    }

    @Override
    protected void printPrizes(Category category, Document document) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void printPrizes(final Category category, final OutputStreamWriter writer) throws IOException {

        final String header = "Category: " + category.getLongName();

        writer.append(header).append("\n");
        writer.append("-".repeat(header.length())).append("\n\n");

        printResults(getMinitourRacePrizeResults(category), new ResultPrinterText(writer));

        writer.append("\n\n");
    }

    record ResultPrinterText(OutputStreamWriter writer) implements ResultPrinter {

        @Override
        public void printResult(final MinitourRaceResult result) throws IOException {

            writer.append(result.position_string).append(": ").
                    append(result.runner.name).append(" (").
                    append(result.runner.club).append(") ").
                    append(format(result.duration())).append("\n");
        }

        @Override
        public void printNoResults() throws IOException {
            writer.append("No results\n");
        }
    }
}
