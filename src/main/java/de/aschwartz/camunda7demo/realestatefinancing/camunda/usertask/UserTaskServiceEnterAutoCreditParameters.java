package de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class UserTaskServiceEnterAutoCreditParameters extends GenericUserTaskService {

	public UserTaskServiceEnterAutoCreditParameters(TaskService taskService, RuntimeService runtimeService, HistoryService historyService) {
		super(taskService, runtimeService, historyService);
	}

	public void enterCreditParameters(BigDecimal monthlyNetIncome, BigDecimal propertyValue, BigDecimal equity, String processInstanceId) {

		Optional<Task> taskOpt = super.findTask(processInstanceId, "Task_EnterAutoCreditParameters");
		if (taskOpt.isEmpty()) {
			throw new RuntimeException("[%s] No active Task_EnterAutoCreditParameters was found.".formatted(processInstanceId));

		}
		Task task = taskOpt.get();

		getTaskService().complete(task.getId(),
				Variables.createVariables()
						.putValue("monthlyNetIncome", monthlyNetIncome)
						.putValue("propertyValue", propertyValue)
						.putValue("equity", equity));
	}

}
