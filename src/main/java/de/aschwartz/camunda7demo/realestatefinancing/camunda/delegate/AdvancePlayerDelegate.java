package de.aschwartz.camunda7demo.realestatefinancing.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class AdvancePlayerDelegate implements JavaDelegate {
	@Override
	public void execute(DelegateExecution execution) {
		execution.setVariable("chosenPieceId", null);
		execution.setVariable("moveWasPossible", null);
		execution.setVariable("kicked", null);
		execution.setVariable("kickedPlayerId", null);
		execution.setVariable("kickedPieceId", null);
	}
}
