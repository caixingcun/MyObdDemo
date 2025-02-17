/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.control;

import eu.lighthouselabs.obd.commands.PercentageObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * 时间提前
 * 01 0E
 * Timing Advance
 */
public class TimingAdvanceObdCommand extends PercentageObdCommand {

	public TimingAdvanceObdCommand() {
		super("01 0E");
	}

	public TimingAdvanceObdCommand(TimingAdvanceObdCommand other) {
		super(other);
	}

	@Override
	public String getName() {
		return AvailableCommandNames.TIMING_ADVANCE.getValue();
	}
}