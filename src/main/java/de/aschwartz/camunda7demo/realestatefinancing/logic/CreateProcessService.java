package de.aschwartz.camunda7demo.realestatefinancing.logic;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class CreateProcessService {
	private final RuntimeService runtimeService;

	public CreateProcessService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	public String createProcess(String processId) {
		var pi = runtimeService
				.createProcessInstanceByKey(processId)
				.executeWithVariablesInReturn();

		return pi.getProcessInstanceId();
	}
}
