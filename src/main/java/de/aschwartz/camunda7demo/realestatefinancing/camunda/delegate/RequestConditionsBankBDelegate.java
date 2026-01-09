package de.aschwartz.camunda7demo.realestatefinancing.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Component("requestConditionsBankBDelegate")
@Slf4j
public class RequestConditionsBankBDelegate implements JavaDelegate {

	Random random = new Random();

	@Override
	public void execute(DelegateExecution execution) {
		BigDecimal monthlyNetIncome = (BigDecimal) execution.getVariable("monthlyNetIncome");
		BigDecimal propertyValue = (BigDecimal) execution.getVariable("propertyValue");
		BigDecimal equity = (BigDecimal) execution.getVariable("equity");

		BigDecimal interestRate = calculateInterestRate(
				monthlyNetIncome, propertyValue, equity
		);

		execution.setVariable("interestRateB", interestRate);
	}

	private BigDecimal calculateInterestRate(
			BigDecimal income,
			BigDecimal propertyValue,
			BigDecimal equity
	) {
		double randomAddend = -1 + random.nextDouble(1.0);
		BigDecimal baseRate = new BigDecimal("3.10").add(new BigDecimal(randomAddend));

		BigDecimal equityRatio =
				(propertyValue != null && propertyValue.signum() > 0 && equity != null)
						? equity.divide(propertyValue, 6, RoundingMode.HALF_UP)
						: BigDecimal.ZERO;

		BigDecimal equityDiscount = equityRatio.multiply(new BigDecimal("1.20"));
		BigDecimal incomeBonus =
				(income != null && income.compareTo(new BigDecimal("4000")) >= 0)
						? new BigDecimal("0.20")
						: BigDecimal.ZERO;

		return baseRate
				.subtract(equityDiscount)
				.subtract(incomeBonus)
				.max(new BigDecimal("1.10"))
				.setScale(2, RoundingMode.HALF_UP);
	}
}
