package de.aschwartz.camunda7demo.realestatefinancing.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("requestConditionsBankADelegate")
@Slf4j
public class RequestConditionsBankADelegate implements JavaDelegate {
	@Override
	public void execute(DelegateExecution execution) {
		BigDecimal monthlyNetIncome = (BigDecimal) execution.getVariable("monthlyNetIncome");
		BigDecimal propertyValue = (BigDecimal) execution.getVariable("propertyValue");
		BigDecimal equity = (BigDecimal) execution.getVariable("equity");

		BigDecimal interestRate = calculateInterestRate(
				monthlyNetIncome, propertyValue, equity
		);

		execution.setVariable("interestRateA", interestRate);
	}

	private BigDecimal calculateInterestRate(
			BigDecimal income,
			BigDecimal propertyValue,
			BigDecimal equity
	) {

		BigDecimal baseRate = new BigDecimal("3.10");

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
