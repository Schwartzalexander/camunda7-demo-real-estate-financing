package de.aschwartz.camunda7demo.realestatefinancing.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OffersResponse implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private List<Angebot> angebote;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Angebot implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private String vermittler;
		private Kondition kondition;
		private Anbieter anbieter;
		private Produktinformation produktinformation;

		private String actionUrl;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Kondition implements Serializable {
			@Serial
			private static final long serialVersionUID = 1L;

			private BigDecimal sollZins;
			private BigDecimal effektivZins;
			private BigDecimal monatlicheRate;
			private BigDecimal anfaenglicheTilgung;

			private BigDecimal zinskostenAmEndeDerZinsbindung;
			private BigDecimal restschuldAmEndeDerZinsbindung;

			private Integer zinsbindungInJahren;
			private Integer gesamtlaufzeitInMonaten;

			private BigDecimal beleihungsauslauf;
			private BigDecimal kaufpreis;
			private BigDecimal darlehensbetrag;
			private BigDecimal gesamtkosten;

			private BigDecimal grundbuchkosten;
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Anbieter implements Serializable {
			@Serial
			private static final long serialVersionUID = 1L;

			private String id;
			private String name;
			private String kurzbezeichnung;
			private String ksId;
			private Anschrift anschrift;

			private String gegruendet;
			private Boolean beratungVorOrt;
			private Boolean unterstuetztKfw;
			private String unterstuetztSondertilgung;
			private String anbietertyp;
			private String informationstext;

			private Integer anzahlMitarbeiter;
			private Integer anzahlNiederlassungen;

			private String urlLogo;
			private Boolean regional;
			private String urlKlickout;

			private String leadId;
			private String leadmanagementId;

			@Data
			@NoArgsConstructor
			@AllArgsConstructor
			@Builder
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public static class Anschrift implements Serializable {
				@Serial
				private static final long serialVersionUID = 1L;

				private String ort;
				private String plz;
				private String strasseUndHausnummer;
			}
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Produktinformation implements Serializable {
			@Serial
			private static final long serialVersionUID = 1L;

			private BigDecimal minimalerDarlehensbetrag;
			private BigDecimal maximalerDarlehensbetrag;
			private BigDecimal bearbeitungsgebuehr;

			private String bereitstellungsfreieZeitText;
			private String sondertilgung;
			private String tilgungsaussetzungText;
			private String kfwDarlehenText;

			private Integer produktId;
		}

		// --------- Optional: JSON serialize/deserialize helper ----------
		public String toJson(ObjectMapper mapper) {
			try {
				return mapper.writeValueAsString(this);
			} catch (Exception e) {
				throw new RuntimeException("Failed to serialize Angebot to JSON", e);
			}
		}

		public static Angebot fromJson(ObjectMapper mapper, String json) {
			try {
				return mapper.readValue(json, Angebot.class);
			} catch (Exception e) {
				throw new RuntimeException("Failed to deserialize Angebot from JSON", e);
			}
		}
	}

	// JSON helpers für die ganze Response
	public String toJson(ObjectMapper mapper) {
		try {
			return mapper.writeValueAsString(this);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize OffersResponse to JSON", e);
		}
	}

	public static OffersResponse fromJson(ObjectMapper mapper, String json) {
		try {
			return mapper.readValue(json, OffersResponse.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize OffersResponse from JSON", e);
		}
	}
}
