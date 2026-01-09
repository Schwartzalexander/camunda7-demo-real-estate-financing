package de.aschwartz.camunda7demo.realestatefinancing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmitApplicationResponse {
	Boolean accepted;
	String contractNumber;
	String rejectionReason;
}
