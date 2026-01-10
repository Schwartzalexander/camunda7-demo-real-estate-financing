package de.aschwartz.camunda7demo.realestatefinancing.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OffersRequest implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Kunde kunde;
	private Immobilie immobilie;
	private Finanzierung finanzierung;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Kunde implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		// im Beispiel: "ANGESTELLTER"
		@JsonProperty("anstellungsVerhaeltnis")
		private String anstellungsVerhaeltnis;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Immobilie implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		// im Beispiel int; wir nehmen BigDecimal, Jackson schreibt dann Zahl
		private BigDecimal kaufPreis;
		private String postleitzahl;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Finanzierung implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		private String finanzierungszweck;
		private BigDecimal tilgungsSatz;
		private Integer zinsBindungInJahren;
		private BigDecimal kreditbetrag;
		private LocalDate auszahlungsTermin;
	}
}
