package lap_race;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OutputPDF extends Output {

    private static final Font PDF_FONT = FontFactory.getFont(FontFactory.HELVETICA);
    private static final Font PDF_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
    private static final Font PDF_BOLD_UNDERLINED_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, Font.DEFAULTSIZE, Font.UNDERLINE);
    private static final Font PDF_BOLD_LARGE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
    private static final Font PDF_ITALIC_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE);

    public OutputPDF(final Results results) {
        super(results);
    }

    @Override
    public void printOverallResults() {

        throw new UnsupportedOperationException();
    }

    @Override
    public void printDetailedResults() {

        throw new UnsupportedOperationException();
    }

    @Override
    public void printLegResults(int leg) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void printPrizes() throws IOException {

        final Path prizes_pdf_path = output_directory_path.resolve(prizes_filename + ".pdf");
        final OutputStream pdf_file_output_stream = Files.newOutputStream(prizes_pdf_path);

        final Document document = new Document();
        PdfWriter.getInstance(document, pdf_file_output_stream);

        document.open();
        document.add(new Paragraph(race_name_for_results + " " + year + " Category Prizes", PDF_BOLD_LARGE_FONT));

        for (final Category category : CATEGORY_REPORT_ORDER)
            printPrizes(category, document);

        document.close();
    }

    private void printPrizes(final Category category, final Document document) {

        final Paragraph category_header_paragraph = new Paragraph(48f, "Category: " + category.shortName(), PDF_BOLD_UNDERLINED_FONT);
        category_header_paragraph.setSpacingAfter(12);
        document.add(category_header_paragraph);

        final List<Team> category_prize_winners = results.prize_winners.get(category);

        if (category_prize_winners.isEmpty())
            document.add(new Paragraph("No results", PDF_ITALIC_FONT));

        int position = 1;
        for (final Team team : category_prize_winners) {

            final OverallResult result = results.overall_results[results.findIndexOfTeamWithBibNumber(team.bib_number)];

            final Paragraph paragraph = new Paragraph();
            paragraph.add(new Chunk(position++ + ": ", PDF_FONT));
            paragraph.add(new Chunk(result.team.name, PDF_BOLD_FONT));
            paragraph.add(new Chunk(" (" + result.team.category.shortName() + ") ", PDF_FONT));
            paragraph.add(new Chunk(format(result.duration()), PDF_FONT));
            document.add(paragraph);
        }
    }
}
