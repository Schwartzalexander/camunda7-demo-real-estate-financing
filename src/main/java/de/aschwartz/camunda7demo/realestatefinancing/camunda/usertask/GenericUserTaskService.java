package de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@Getter
public class GenericUserTaskService {

	private final TaskService taskService;
	private final RuntimeService runtimeService;
	private final HistoryService historyService;

	public GenericUserTaskService(TaskService taskService, RuntimeService runtimeService, HistoryService historyService) {
		this.taskService = taskService;
		this.runtimeService = runtimeService;
		this.historyService = historyService;
	}

	Optional<Task> findTask(String processInstanceId, String taskDefinitionKey) {
		return taskService.createTaskQuery()
				.taskDefinitionKey(taskDefinitionKey)
				.active()
				.orderByTaskCreateTime()
				.desc()
				.listPage(0, 1)
				.stream().findFirst();
	}

	String readStringVar(String processInstanceId, String name) {
		Object o = readVar(processInstanceId, name);
		return (o instanceof String s) ? s : null;
	}

	Boolean readBooleanVar(String processInstanceId, String name) {
		Object o = readVar(processInstanceId, name);
		if (o instanceof Boolean b) return b;
		if (o instanceof String s) return Boolean.parseBoolean(s); // fallback
		return null;
	}

	private boolean isProcessInstanceActive(String processInstanceId) {
		ProcessInstance pi = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId)
				.active()
				.singleResult();
		return pi != null;
	}

	private Object readVar(String processInstanceId, String name) {
		// 1) try runtime (if still running)
		if (isProcessInstanceActive(processInstanceId)) {
			Object value = runtimeService.getVariable(processInstanceId, name);
			if (value != null)
				return value;
		}

		// 2) history (works after end)
		HistoricVariableInstance hvi = historyService.createHistoricVariableInstanceQuery()
				.processInstanceId(processInstanceId)
				.variableName(name)
				.singleResult();

		return hvi != null ? hvi.getValue() : null;
	}
}
