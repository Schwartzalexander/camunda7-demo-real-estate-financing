package de.aschwartz.camunda7demo.realestatefinancing.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Component("requestConditionsBankCDelegate")
@Slf4j
public class RequestConditionsBankCDelegate implements JavaDelegate {

	Random random = new Random();

	@Override
	public void execute(DelegateExecution execution) {
		BigDecimal monthlyNetIncome = (BigDecimal) execution.getVariable("monthlyNetIncome");
		BigDecimal propertyValue = (BigDecimal) execution.getVariable("propertyValue");
		BigDecimal equity = (BigDecimal) execution.getVariable("equity");

		BigDecimal interestRate = calculateInterestRate(
				monthlyNetIncome, propertyValue, equity
		);

		execution.setVariable("interestRateC", interestRate);
	}

	private BigDecimal calculateInterestRate(
			BigDecimal income,
			BigDecimal propertyValue,
			BigDecimal equity
	) {
		if (propertyValue == null || propertyValue.signum() <= 0) {
			// Ohne Objektwert kann man kein LTV bilden -> sehr teuer
			return new BigDecimal("12.00");
		}

		if (equity == null) equity = BigDecimal.ZERO;
		if (income == null) income = BigDecimal.ZERO;

		// loan = propertyValue - equity, min 0
		BigDecimal loan = propertyValue.subtract(equity);
		if (loan.signum() < 0) loan = BigDecimal.ZERO;

		// LTV in [0..]  (1.0 = 100%)
		BigDecimal ltv = loan.divide(propertyValue, 8, RoundingMode.HALF_UP);

		// Kernidee:
		// - Bei kleinem LTV soll der Zins sehr klein werden.
		// - Ab ca. 80% steigt es deutlich,
		// - bei >=100% wird es extrem teuer.
		//
		// Wir bauen eine glatte Kurve:
		// rate = nearZero + A * (ltv^p) / (1 - ltv + eps)   (bläst sich gegen 1 auf)
		// plus Zuschlag, wenn ltv >= 1
		BigDecimal nearZero = new BigDecimal("0.15");     // "fast 0"
		BigDecimal A = new BigDecimal("1.40");      // Grundskalierung
		BigDecimal p = new BigDecimal("2.30");      // stärkere Gewichtung hoher LTV
		BigDecimal eps = new BigDecimal("0.03");      // verhindert Division durch 0

		// ltv^p (mit double, weil BigDecimal-Potenz mit non-integer Aufwand ist)
		double ltvD = ltv.doubleValue();
		double curve = Math.pow(ltvD, p.doubleValue()) / (Math.max(1e-9, (1.0 - ltvD + eps.doubleValue())));

		BigDecimal rate = nearZero.add(A.multiply(BigDecimal.valueOf(curve)));

		// Bonus/Malus: Einkommen beeinflusst leicht (Bank B schaut primär auf LTV)
		if (income.compareTo(new BigDecimal("5000")) >= 0) {
			rate = rate.subtract(new BigDecimal("0.10"));
		} else if (income.compareTo(new BigDecimal("2500")) < 0) {
			rate = rate.add(new BigDecimal("0.25"));
		}

		// Harte Risikozuschläge bei sehr hohem Beleihungsauslauf
		if (ltv.compareTo(new BigDecimal("0.90")) >= 0) {
			rate = rate.add(new BigDecimal("1.50")); // ab 90% deutlich teurer
		}

		// Wenn >=100%: richtig hoch
		if (ltv.compareTo(BigDecimal.ONE) >= 0) {
			BigDecimal over = ltv.subtract(BigDecimal.ONE); // z.B. 1.10 -> 0.10
			// schneller Anstieg: bis +10% LTV gibt's nochmal ordentlich drauf
			rate = rate.add(new BigDecimal("6.00"))
					.add(over.multiply(new BigDecimal("20.0"))); // 10% over -> +2.0
		}

		// Deckelung in einem plausiblen Rahmen
		BigDecimal min = new BigDecimal("0.05");  // fast 0
		BigDecimal max = new BigDecimal("18.00"); // sehr hoch
		if (rate.compareTo(min) < 0) rate = min;
		if (rate.compareTo(max) > 0) rate = max;

		return rate.setScale(2, RoundingMode.HALF_UP);
	}

}
