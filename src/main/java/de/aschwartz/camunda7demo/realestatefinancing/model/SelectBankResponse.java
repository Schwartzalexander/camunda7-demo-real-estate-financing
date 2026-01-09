package de.aschwartz.camunda7demo.realestatefinancing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SelectBankResponse {
	BigDecimal monthlyNetIncome;
	BigDecimal propertyValue;
	BigDecimal equity;
}
