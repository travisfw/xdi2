package xdi2.messaging.target.interceptor;

import xdi2.messaging.MessageResult;
import xdi2.messaging.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.ExecutionContext;

/**
 * Interceptor that is executed on a message result after a message envelope has been executed.
 * 
 * @author markus
 */
public interface ResultInterceptor extends Interceptor {

	/**
	 * Run on the message result after it is complete for final adjustments.
	 * @param messageResult The message result.
	 * @param executionContext The current execution context.
	 */
	public void finish(MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException;
}
