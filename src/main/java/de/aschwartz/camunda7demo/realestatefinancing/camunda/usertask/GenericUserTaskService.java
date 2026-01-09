package de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@Getter
public class GenericUserTaskService {

	private final TaskService taskService;
	private final RuntimeService runtimeService;

	public GenericUserTaskService(TaskService taskService, RuntimeService runtimeService) {
		this.taskService = taskService;
		this.runtimeService = runtimeService;
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
}
