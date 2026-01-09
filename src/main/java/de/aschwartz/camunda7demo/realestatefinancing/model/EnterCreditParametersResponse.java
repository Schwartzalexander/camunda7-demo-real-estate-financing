package de.aschwartz.camunda7demo.realestatefinancing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EnterCreditParametersResponse {
	String processInstanceId;
	List<Offer> offers;
}
