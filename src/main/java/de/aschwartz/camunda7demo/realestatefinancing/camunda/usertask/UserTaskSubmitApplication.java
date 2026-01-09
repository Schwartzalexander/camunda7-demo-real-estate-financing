package de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask;

import de.aschwartz.camunda7demo.realestatefinancing.model.SubmitApplicationResponse;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserTaskSubmitApplication extends GenericUserTaskService {

	public UserTaskSubmitApplication(TaskService taskService, RuntimeService runtimeService, HistoryService historyService) {
		super(taskService, runtimeService, historyService);
	}

	public SubmitApplicationResponse submitApplication(String processInstanceId) {
		Optional<Task> taskOpt = super.findTask(processInstanceId, "Task_SubmitApplication");
		if (taskOpt.isEmpty()) {
			throw new RuntimeException("[%s] No active Task_SubmitApplication was found.".formatted(processInstanceId));

		}
		Task task = taskOpt.get();

		getTaskService().complete(task.getId());

		Boolean accepted = readBooleanVar(processInstanceId, "applicationAccepted");
		String contractNumber = readStringVar(processInstanceId, "contractNumber");
		String rejectionReason = readStringVar(processInstanceId, "rejectionReason");

		return new SubmitApplicationResponse(accepted, contractNumber, rejectionReason);
	}

}
