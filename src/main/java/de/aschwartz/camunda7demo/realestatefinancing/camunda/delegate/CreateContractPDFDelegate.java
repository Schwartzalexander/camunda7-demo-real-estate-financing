package de.aschwartz.camunda7demo.realestatefinancing.camunda.delegate;

import de.aschwartz.camunda7demo.realestatefinancing.model.OffersResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component("createContractPdfDelegate")
@Slf4j
public class CreateContractPDFDelegate implements JavaDelegate {

	private final Path pdfDir;

	public CreateContractPDFDelegate(@Value("${camunda7demo.pdf-path}") String pdfPath) {
		this.pdfDir = Path.of(pdfPath);
	}

	@Override
	public void execute(DelegateExecution execution) {

		OffersResponse.Angebot cheapestOffer =
				(OffersResponse.Angebot) execution.getVariable("cheapestOffer");

		if (cheapestOffer == null) {
			throw new IllegalStateException("Process variable 'cheapestOffer' is null");
		}

		try {
			Files.createDirectories(pdfDir);
		} catch (IOException e) {
			throw new RuntimeException("Could not create pdf directory: " + pdfDir, e);
		}

		String processInstanceId = execution.getProcessInstanceId();
		String fileName = "credit-contract-" + processInstanceId + "-" + LocalDate.now() + ".pdf";
		Path target = pdfDir.resolve(fileName);

		try (PDDocument doc = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			doc.addPage(page);

			try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
				float margin = 48f;
				float y = page.getMediaBox().getHeight() - margin;

				// Helpers
				DecimalFormat df2 = new DecimalFormat("0.00");
				DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

				// Extract fields safely
				var anbieter = cheapestOffer.getAnbieter();
				var anschrift = (anbieter != null) ? anbieter.getAnschrift() : null;
				var kond = cheapestOffer.getKondition();

				String vermittler = nvl(cheapestOffer.getVermittler(), "—");
				String bankName = nvl(anbieter != null ? anbieter.getName() : null, "—");
				String bankKurz = nvl(anbieter != null ? anbieter.getKurzbezeichnung() : null, "");
				String bankOrt = nvl(anschrift != null ? anschrift.getOrt() : null, "—");
				String bankPlz = nvl(anschrift != null ? anschrift.getPlz() : null, "—");
				String bankStr = nvl(anschrift != null ? anschrift.getStrasseUndHausnummer() : null, "—");

				// Header
				y = drawTitle(cs, margin, y, "Kreditvertrag (Demo)", "Erstellt am " + LocalDate.now().format(dateFmt));

				// Offer summary box
				y -= 16;
				y = drawSectionHeader(cs, margin, y, "Anbieter & Vermittler");
				y -= 10;

				y = drawKeyValue(cs, margin, y, "Vermittler", vermittler);
				y = drawKeyValue(cs, margin, y, "Anbieter / Bank", bankName);
				if (!bankKurz.isBlank()) {
					y = drawKeyValue(cs, margin, y, "Kurzbezeichnung", bankKurz);
				}
				y = drawKeyValue(cs, margin, y, "Adresse", bankStr + ", " + bankPlz.trim() + " " + bankOrt);

				// Konditionen
				y -= 14;
				y = drawSectionHeader(cs, margin, y, "Konditionen");
				y -= 10;

				// draw as a neat 2-column list
				y = drawKeyValue(cs, margin, y, "Sollzins (%)", fmt(kond != null ? kond.getSollZins() : null, df2));
				y = drawKeyValue(cs, margin, y, "Effektivzins (%)", fmt(kond != null ? kond.getEffektivZins() : null, df2));
				y = drawKeyValue(cs, margin, y, "Monatliche Rate (€)", fmt(kond != null ? kond.getMonatlicheRate() : null, df2));
				y = drawKeyValue(cs, margin, y, "Anfängliche Tilgung (%)", fmt(kond != null ? kond.getAnfaenglicheTilgung() : null, df2));
				y = drawKeyValue(cs, margin, y, "Zinsbindung (Jahre)", nvl(kond != null ? kond.getZinsbindungInJahren() : null, "—"));
				y = drawKeyValue(cs, margin, y, "Darlehensbetrag (€)", fmt(kond != null ? kond.getDarlehensbetrag() : null, df2));
				y = drawKeyValue(cs, margin, y, "Kaufpreis (€)", fmt(kond != null ? kond.getKaufpreis() : null, df2));
				y = drawKeyValue(cs, margin, y, "Restschuld Ende Zinsbindung (€)", fmt(kond != null ? kond.getRestschuldAmEndeDerZinsbindung() : null, df2));
				y = drawKeyValue(cs, margin, y, "Zinskosten Ende Zinsbindung (€)", fmt(kond != null ? kond.getZinskostenAmEndeDerZinsbindung() : null, df2));
				y = drawKeyValue(cs, margin, y, "Gesamtlaufzeit (Monate)", nvl(kond != null ? kond.getGesamtlaufzeitInMonaten() : null, "—"));
				y = drawKeyValue(cs, margin, y, "Beleihungsauslauf (%)", fmt(kond != null ? kond.getBeleihungsauslauf() : null, df2));
				y = drawKeyValue(cs, margin, y, "Gesamtkosten (€)", fmt(kond != null ? kond.getGesamtkosten() : null, df2));

				// Small disclaimer
				y -= 16;
				cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
				writeLine(cs, margin, y, "Hinweis: Dieses Dokument ist eine Demo und stellt keinen rechtsverbindlichen Vertrag dar.");
				y -= 28;

				// Signature section
				y = drawSectionHeader(cs, margin, y, "Unterschrift");
				y -= 18;

				cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
				writeLine(cs, margin, y, "Ort / Datum:");
				drawLine(cs, margin + 70, y - 3, margin + 260, y - 3);
				y -= 26;

				writeLine(cs, margin, y, "Unterschrift Kunde:");
				drawLine(cs, margin + 110, y - 3, margin + 420, y - 3);
			}

			doc.save(target.toFile());
			log.info("Contract PDF created at: {}", target);

			// optional: Pfad als Prozessvariable ablegen
			execution.setVariable("contractPdfPath", target.toString());

		} catch (IOException e) {
			throw new RuntimeException("Failed to generate PDF at " + target, e);
		}
	}

	// --------- Drawing helpers (simple + robust) ---------

	private static float drawTitle(PDPageContentStream cs, float x, float y, String title, String subtitle) throws IOException {
		cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
		writeLine(cs, x, y, title);
		y -= 20;
		cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
		writeLine(cs, x, y, subtitle);
		return y;
	}

	private static float drawSectionHeader(PDPageContentStream cs, float x, float y, String text) throws IOException {
		cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
		writeLine(cs, x, y, text);
		// underline
		drawLine(cs, x, y - 4, x + 520, y - 4);
		return y - 2;
	}

	private static float drawKeyValue(PDPageContentStream cs, float x, float y, String key, String value) throws IOException {
		float keyWidth = 170f;
		cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10.5f);
		writeLine(cs, x, y, key + ":");
		cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10.5f);
		writeLine(cs, x + keyWidth, y, value);
		return y - 14;
	}

	private static void writeLine(PDPageContentStream cs, float x, float y, String text) throws IOException {
		cs.beginText();
		cs.newLineAtOffset(x, y);
		cs.showText(text);
		cs.endText();
	}

	private static void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
		cs.moveTo(x1, y1);
		cs.lineTo(x2, y2);
		cs.stroke();
	}

	private static String nvl(String s, String fallback) {
		return (s == null || s.isBlank()) ? fallback : s;
	}

	private static String nvl(Integer i, String fallback) {
		return (i == null) ? fallback : String.valueOf(i);
	}

	private static String fmt(BigDecimal v, DecimalFormat df) {
		return (v == null) ? "—" : df.format(v);
	}
}
