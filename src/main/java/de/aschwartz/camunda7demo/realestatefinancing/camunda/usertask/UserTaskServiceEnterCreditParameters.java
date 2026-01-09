package de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask;

import de.aschwartz.camunda7demo.realestatefinancing.model.EnterCreditParametersResponse;
import de.aschwartz.camunda7demo.realestatefinancing.model.Offer;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserTaskServiceEnterCreditParameters extends GenericUserTaskService {

	public UserTaskServiceEnterCreditParameters(TaskService taskService, RuntimeService runtimeService) {
		super(taskService, runtimeService);
	}

	public EnterCreditParametersResponse enterCreditParameters(BigDecimal monthlyNetIncome, BigDecimal propertyValue, BigDecimal equity) {
		var pi = getRuntimeService()
				.createProcessInstanceByKey("RealEstateCreditApplication")
				.executeWithVariablesInReturn();

		String processInstanceId = pi.getProcessInstanceId();

		Optional<Task> taskEnterCreditParametersOpt = super.findTask(processInstanceId, "Task_EnterCreditParameters");
		if (taskEnterCreditParametersOpt.isEmpty()) {
			throw new RuntimeException("[%s] No active Task_EnterCreditParameters was found.".formatted(processInstanceId));

		}
		Task task = taskEnterCreditParametersOpt.get();

		getTaskService().complete(task.getId(),
				Variables.createVariables()
						.putValue("monthlyNetIncome", monthlyNetIncome)
						.putValue("propertyValue", propertyValue)
						.putValue("equity", equity));

		Object offersVar = getRuntimeService().getVariable(processInstanceId, "creditOffers");

		@SuppressWarnings("unchecked")
		List<Offer> offers = (List<Offer>) offersVar;

		return new EnterCreditParametersResponse(processInstanceId, offers);
	}
 
}
