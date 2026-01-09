package de.aschwartz.camunda7demo.realestatefinancing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewResult {
	private boolean accepted;
	private String contractNumber;
	private String rejectionReason;

	public static ReviewResult accepted(String contractNumber) {
		return new ReviewResult(true, contractNumber, null);
	}

	public static ReviewResult rejected(String reason) {
		return new ReviewResult(false, null, reason);
	}
}
