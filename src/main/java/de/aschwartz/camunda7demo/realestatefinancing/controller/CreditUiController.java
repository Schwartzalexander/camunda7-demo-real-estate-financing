package de.aschwartz.camunda7demo.realestatefinancing.controller;

import de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask.UserTaskSelectBank;
import de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask.UserTaskServiceEnterCreditParameters;
import de.aschwartz.camunda7demo.realestatefinancing.camunda.usertask.UserTaskSubmitApplication;
import de.aschwartz.camunda7demo.realestatefinancing.model.EnterCreditParametersResponse;
import de.aschwartz.camunda7demo.realestatefinancing.model.Offer;
import de.aschwartz.camunda7demo.realestatefinancing.model.SelectBankResponse;
import de.aschwartz.camunda7demo.realestatefinancing.model.SubmitApplicationResponse;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequestMapping("/credit")
@Slf4j
public class CreditUiController {

	// Task definition keys in your main process (must match BPMN ids)
	private static final String TASK_SIGN = "Task_SignContract";

	private final TaskService taskService;
	private final UserTaskServiceEnterCreditParameters userTaskServiceEnterCreditParameters;
	private final UserTaskSelectBank userTaskSelectBank;
	private final UserTaskSubmitApplication userTaskSubmitApplication;

	public CreditUiController(
			TaskService taskService,
			UserTaskServiceEnterCreditParameters userTaskServiceEnterCreditParameters,
			UserTaskSelectBank userTaskSelectBank,
			UserTaskSubmitApplication userTaskSubmitApplication) {
		this.taskService = taskService;
		this.userTaskServiceEnterCreditParameters = userTaskServiceEnterCreditParameters;
		this.userTaskSelectBank = userTaskSelectBank;
		this.userTaskSubmitApplication = userTaskSubmitApplication;
	}

	@GetMapping
	public String page(Model model) {
		// Defaults (optional)
		Object monthlyNetIncome = model.getAttribute("monthlyNetIncome");
		model.addAttribute("monthlyNetIncome", monthlyNetIncome != null ? monthlyNetIncome : "2500");
		Object propertyValue = model.getAttribute("propertyValue");
		model.addAttribute("propertyValue", propertyValue != null ? propertyValue : "100000");
		Object equity = model.getAttribute("equity");
		model.addAttribute("equity", equity != null ? equity : "10000");
		return "credit";
	}

	@PostMapping("/compare")
	public String compare(
			@RequestParam BigDecimal monthlyNetIncome,
			@RequestParam BigDecimal propertyValue,
			@RequestParam BigDecimal equity,
			Model model
	) {
		model.addAttribute("monthlyNetIncome", monthlyNetIncome);
		model.addAttribute("propertyValue", propertyValue);
		model.addAttribute("equity", equity);
		List<Offer> offers;

		try {
			EnterCreditParametersResponse response = userTaskServiceEnterCreditParameters.enterCreditParameters(monthlyNetIncome, propertyValue, equity);
			offers = response.getOffers();
			model.addAttribute("processInstanceId", response.getProcessInstanceId());
		} catch (Exception e) {
			// Fallback demo offers if the BPMN doesn't provide creditOffers yet.
			offers = demoOffers(monthlyNetIncome, propertyValue, equity);
			model.addAttribute("statusType", "danger");
			model.addAttribute("statusTitle", "Info");
			model.addAttribute("statusMessage",
					"Could not read creditOffers from process. Showing demo offers. (" + e.getMessage() + ")");
			log.error(e.getMessage(), e);
		}

		model.addAttribute("offers", offers);
		return "credit";
	}

	@PostMapping("/select")
	public String selectBankAndSubmit(
			@RequestParam String bankName,
			@RequestParam String processInstanceId,
			Model model
	) {
		SelectBankResponse selectBankResponse = userTaskSelectBank.selectBank(bankName, processInstanceId);
		SubmitApplicationResponse submitApplicationResponse = userTaskSubmitApplication.submitApplication(processInstanceId);

		model.addAttribute("processInstanceId", processInstanceId);
		model.addAttribute("monthlyNetIncome", selectBankResponse.getMonthlyNetIncome());
		model.addAttribute("propertyValue", selectBankResponse.getPropertyValue());
		model.addAttribute("equity", selectBankResponse.getEquity());
		model.addAttribute("applicationAccepted", submitApplicationResponse.getAccepted());
		model.addAttribute("contractNumber", submitApplicationResponse.getContractNumber());
		model.addAttribute("rejectionReason", submitApplicationResponse.getRejectionReason());

		return "credit";
	}

	@PostMapping("/sign")
	public String sign(@RequestParam String processInstanceId, Model model) {
		// Find sign task and complete
		Task signTask = taskService.createTaskQuery()
				.processInstanceId(processInstanceId)
				.taskDefinitionKey(TASK_SIGN)
				.singleResult();

		if (signTask == null) {
			model.addAttribute("statusType", "danger");
			model.addAttribute("statusTitle", "Not found");
			model.addAttribute("statusMessage", "No sign task found (process may already be finished).");
			model.addAttribute("showSign", false);
			return "credit";
		}

		taskService.complete(signTask.getId());

		model.addAttribute("statusType", "success");
		model.addAttribute("statusTitle", "Done");
		model.addAttribute("statusMessage", "Contract signed. Credit contract concluded.");
		model.addAttribute("showSign", false);

		return "credit";
	}

	private List<Offer> demoOffers(BigDecimal income, BigDecimal value, BigDecimal equity) {
		// Simple deterministic demo calculation
		BigDecimal base = new BigDecimal("3.20");
		BigDecimal equityRatio = equity.compareTo(BigDecimal.ZERO) > 0 && value.compareTo(BigDecimal.ZERO) > 0
				? equity.divide(value, 6, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		BigDecimal discount = equityRatio.multiply(new BigDecimal("1.20")); // up to ~1.2% discount if equityRatio=1.0
		BigDecimal incomeBonus = income.compareTo(new BigDecimal("4000")) >= 0 ? new BigDecimal("0.20") : BigDecimal.ZERO;

		BigDecimal a = base.subtract(discount).subtract(incomeBonus).max(new BigDecimal("1.10"));
		BigDecimal b = a.add(new BigDecimal("0.35"));
		BigDecimal c = a.add(new BigDecimal("0.60"));

		return List.of(
				new Offer("Bank A", a.setScale(2, RoundingMode.HALF_UP)),
				new Offer("Bank B", b.setScale(2, RoundingMode.HALF_UP)),
				new Offer("Bank C", c.setScale(2, RoundingMode.HALF_UP))
		);
	}

}
