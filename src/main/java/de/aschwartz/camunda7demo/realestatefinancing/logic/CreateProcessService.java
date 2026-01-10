package de.aschwartz.camunda7demo.realestatefinancing.logic;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class CreateProcessService {
	private final RuntimeService runtimeService;

	public CreateProcessService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	public String createProcess() {
		var pi = runtimeService
				.createProcessInstanceByKey("RealEstateCreditApplication")
				.executeWithVariablesInReturn();

		return pi.getProcessInstanceId();
	}
}
