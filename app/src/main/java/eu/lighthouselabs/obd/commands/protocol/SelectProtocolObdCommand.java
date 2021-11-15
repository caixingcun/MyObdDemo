/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.protocol;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.ObdProtocols;

/**
 * Select the protocol to use.
 * 选择OBD协议命令
 */
public class SelectProtocolObdCommand extends ObdCommand {
	
	private final ObdProtocols _protocol;

	/**
	 * @param protocol 协议
	 *  比如标准的 AT SP 0
	 *  0 为协议类型
	 */
	public SelectProtocolObdCommand(ObdProtocols protocol) {
		super("AT SP " + protocol.getValue());
		_protocol = protocol;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.lighthouselabs.MyCommand.commands.ObdCommand#getFormattedResult()
	 */
	@Override
	public String getFormattedResult() {
		return getResult();
	}

	@Override
	public String getName() {
		return "Select Protocol " + _protocol.name();
	}

}