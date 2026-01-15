package de.aschwartz.camunda7demo.realestatefinancing.camunda.executionlistener;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

@Slf4j
public class LogDecline implements ExecutionListener {
	@Override
	public void notify(DelegateExecution delegateExecution) throws Exception {
		log.info("[{}] DMN credit application was declined.", delegateExecution.getProcessInstance().getProcessInstanceId());
	}
}
