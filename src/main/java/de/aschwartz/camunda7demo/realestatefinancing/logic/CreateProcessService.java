package de.aschwartz.camunda7demo.realestatefinancing.logic;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CreateProcessService {
	private final RuntimeService runtimeService;

	public CreateProcessService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	public String createProcess(String processId) {
		try {
			var pi = runtimeService
					.createProcessInstanceByKey(processId)
					.executeWithVariablesInReturn();
			String processInstanceId = pi.getProcessInstanceId();
			log.info("[{}] Process {} was started.", processInstanceId, processId);
			return processInstanceId;
		} catch (Exception e) {
			log.error("Process {} could not be started.", processId, e);
			throw e;
		}
	}
}
