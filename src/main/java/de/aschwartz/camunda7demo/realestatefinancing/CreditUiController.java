package de.aschwartz.camunda7demo.realestatefinancing;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/credit")
public class CreditUiController {

	/**
	 * BPMN process keys (must match your BPMN process ids/keys).
	 * - Main process: RealEstateCreditApplication
	 * - Comparison process: CreditComparisonProcess (please set this in your comparison BPMN)
	 */
	private static final String PROC_MAIN = "RealEstateCreditApplication";
	private static final String PROC_COMPARE = "CreditComparisonProcess";

	// Task definition keys in your main process (must match BPMN ids)
	private static final String TASK_ENTER_PARAMS = "Task_EnterCreditParameters";
	private static final String TASK_BANK_SELECTION = "Task_BankSelection";
	private static final String TASK_SUBMIT = "Task_SubmitApplication";
	private static final String TASK_SIGN = "Task_SignContract";

	private final RuntimeService runtimeService;
	private final TaskService taskService;

	public CreditUiController(RuntimeService runtimeService, TaskService taskService) {
		this.runtimeService = runtimeService;
		this.taskService = taskService;
	}

	@GetMapping
	public String page(Model model) {
		// Defaults (optional)
		model.addAttribute("monthlyNetIncome", model.getAttribute("monthlyNetIncome"));
		model.addAttribute("propertyValue", model.getAttribute("propertyValue"));
		model.addAttribute("equity", model.getAttribute("equity"));
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

		// Start comparison synchronously (works if the process has no wait states).
		// If your comparison process includes wait states, executeWithVariablesInReturn will return earlier.
		List<Offer> offers;
		try {
			ProcessInstanceWithVariables pi = runtimeService
					.createProcessInstanceByKey(PROC_COMPARE)
					.setVariable("monthlyNetIncome", monthlyNetIncome)
					.setVariable("propertyValue", propertyValue)
					.setVariable("equity", equity)
					.executeWithVariablesInReturn();

			// Expecting a variable "creditOffers" (per your call activity out mapping).
			// If your process sets it differently, adjust here.
			TypedValue tv = pi.getVariables().getValueTyped("creditOffers");
			offers = toOffers(tv);
		} catch (Exception e) {
			// Fallback demo offers if the BPMN doesn't provide creditOffers yet.
			offers = demoOffers(monthlyNetIncome, propertyValue, equity);
			model.addAttribute("statusType", "danger");
			model.addAttribute("statusTitle", "Info");
			model.addAttribute("statusMessage",
					"Could not read creditOffers from process. Showing demo offers. (" + e.getClass().getSimpleName() + ")");
		}

		model.addAttribute("offers", offers);
		return "credit";
	}

	@PostMapping("/select")
	public String selectBankAndSubmit(
			@RequestParam String bankName,
			@RequestParam BigDecimal monthlyNetIncome,
			@RequestParam BigDecimal propertyValue,
			@RequestParam BigDecimal equity,
			Model model
	) {
		model.addAttribute("monthlyNetIncome", monthlyNetIncome);
		model.addAttribute("propertyValue", propertyValue);
		model.addAttribute("equity", equity);

		// Start main process (will likely stop at first user task)
		var vars = Variables.createVariables()
				.putValue("monthlyNetIncome", monthlyNetIncome)
				.putValue("propertyValue", propertyValue)
				.putValue("equity", equity)
				.putValue("selectedBank", bankName);

		String processInstanceId = runtimeService.startProcessInstanceByKey(PROC_MAIN, vars).getProcessInstanceId();

		// Drive the process forward until it either:
		// - ends (declined path)
		// - reaches Task_SignContract (accepted path)
		boolean accepted = driveMainProcessToSignOrEnd(processInstanceId, bankName, vars);

		model.addAttribute("processInstanceId", processInstanceId);

		if (!accepted) {
			model.addAttribute("statusType", "danger");
			model.addAttribute("statusTitle", "Declined");
			model.addAttribute("statusMessage", "The bank declined your credit application.");
			model.addAttribute("showSign", false);
		} else {
			model.addAttribute("statusType", "success");
			model.addAttribute("statusTitle", "Accepted");
			model.addAttribute("statusMessage", "The bank accepted your credit application.");
			model.addAttribute("showSign", true);
		}

		// Keep showing offers area? Optional:
		model.addAttribute("offers", demoOffers(monthlyNetIncome, propertyValue, equity));

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

	/**
	 * Completes the user tasks automatically up to either SignContract (wait state) or process end.
	 * In a real system, you'd model more tasks as service tasks / delegates, not in the controller.
	 */
	private boolean driveMainProcessToSignOrEnd(String processInstanceId, String bankName, org.camunda.bpm.engine.variable.VariableMap vars) {
		// 1) Complete Task_EnterCreditParameters
		completeIfPresent(processInstanceId, TASK_ENTER_PARAMS, vars);

		// 2) Complete Task_BankSelection with chosen bank
		var bankVars = Variables.createVariables().putValue("selectedBank", bankName);
		completeIfPresent(processInstanceId, TASK_BANK_SELECTION, bankVars);

		// 3) Complete Task_SubmitApplication
		completeIfPresent(processInstanceId, TASK_SUBMIT, Variables.createVariables());

		// At this point service task "Bank reviews loan application" runs.
		// If your BPMN service task does NOT set application_accepted, we set it here as a demo fallback.
		// Adjust or remove this once your JavaDelegate does it.
		boolean accepted = decideAcceptance(bankName);
		runtimeService.setVariable(processInstanceId, "application_accepted", accepted);

		// Now either:
		// - process ends (declined), no tasks
		// - reaches Task_SignContract (accepted), one task waiting
		Task signTask = taskService.createTaskQuery()
				.processInstanceId(processInstanceId)
				.taskDefinitionKey(TASK_SIGN)
				.singleResult();

		// If sign exists => accepted
		if (signTask != null) return true;

		// else maybe process already ended or is stuck somewhere unexpected
		return false;
	}

	private void completeIfPresent(String processInstanceId, String taskDefinitionKey, org.camunda.bpm.engine.variable.VariableMap variables) {
		Task task = taskService.createTaskQuery()
				.processInstanceId(processInstanceId)
				.taskDefinitionKey(taskDefinitionKey)
				.singleResult();
		if (task != null) {
			taskService.complete(task.getId(), variables);
		}
	}

	private boolean decideAcceptance(String bankName) {
		// Demo rule: Bank A accepts, others decline. Replace with your real bank logic.
		return "Bank A".equalsIgnoreCase(bankName) || "A".equalsIgnoreCase(bankName);
	}

	/**
	 * Converts the process variable creditOffers into Offer list.
	 * You can switch this depending on how you store offers (Spin JSON, List<Map>, etc.).
	 */
	@SuppressWarnings("unchecked")
	private List<Offer> toOffers(TypedValue typedValue) {
		if (typedValue == null || typedValue.getValue() == null) return List.of();

		Object v = typedValue.getValue();

		// Common simple case: List<Map<String,Object>>
		if (v instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
			return ((List<Map<String, Object>>) v).stream()
					.map(m -> new Offer(
							Objects.toString(m.getOrDefault("bankName", "Unknown")),
							toBigDecimal(m.get("interestRate"))
					))
					.collect(Collectors.toList());
		}

		// If you store another format, adapt here.
		return List.of();
	}

	private BigDecimal toBigDecimal(Object o) {
		if (o == null) return BigDecimal.ZERO;
		if (o instanceof BigDecimal bd) return bd;
		if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
		try {
			return new BigDecimal(o.toString());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
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

	public record Offer(String bankName, BigDecimal interestRate) {
	}
}
