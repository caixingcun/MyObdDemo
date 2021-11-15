/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.engine;

import eu.lighthouselabs.obd.commands.PercentageObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * 油门位置
 * 01 11
 */
public class ThrottlePositionObdCommand extends PercentageObdCommand {

	/**
	 * Default ctor.
	 */
	public ThrottlePositionObdCommand() {
		super("01 11");
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public ThrottlePositionObdCommand(ThrottlePositionObdCommand other) {
		super(other);
	}

	/**
	 * 
	 */
	@Override
	public String getName() {
		return AvailableCommandNames.THROTTLE_POS.getValue();
	}
	
}