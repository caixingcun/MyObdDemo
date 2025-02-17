/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.engine;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.PercentageObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * Calculated Engine Load value.
 * 01 04
 * 发动机负荷
 */
public class EngineLoadObdCommand extends PercentageObdCommand {

	/**
	 * @param command
	 */
	public EngineLoadObdCommand() {
		super("01 04");
	}

	/**
	 * @param other
	 */
	public EngineLoadObdCommand(ObdCommand other) {
		super(other);
	}

	/* (non-Javadoc)
	 * @see eu.lighthouselabs.MyCommand.commands.ObdCommand#getName()
	 */
	@Override
	public String getName() {
		return AvailableCommandNames.ENGINE_LOAD.getValue();
	}

}