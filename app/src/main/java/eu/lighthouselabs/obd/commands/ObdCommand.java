/*
 * TODO put header
 */

package eu.lighthouselabs.obd.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * obd命令抽象类
 */
public abstract class ObdCommand {
	/**
	 * 缓冲区
	 */
	protected ArrayList<Integer> buffer = null;
	/**
	 * 命令
	 */
	protected String cmd = null;
	protected boolean useImperialUnits = false;
	/**
	 * 返回数据
	 */
	protected String rawData = null;

	/**
	 * Default ctor to use
	 * 继承类 super父类构造 传入 命令，建立缓冲区
	 * @param command
	 *            the command to send
	 */
	public ObdCommand(String command) {
		this.cmd = command;
		this.buffer = new ArrayList<Integer>();
	}

	/**
	 * Prevent empty instantiation
	 */
	private ObdCommand() {
	}

	/**
	 * Copy ctor.
	 * 支持传入 ObdCmd 类型
	 * @param other
	 *            the ObdCommand to copy.
	 */
	public ObdCommand(ObdCommand other) {
		this(other.cmd);
	}

	/**
	 * Sends the OBD-II request and deals with the response.
	 * 
	 * This method CAN be overriden in fake commands.
	 */
	public void run(InputStream in, OutputStream out) throws IOException,
			InterruptedException {
		sendCommand(out);
		readResult(in);
	}

	/**
	 * Sends the OBD-II request.
	 * 
	 * This method may be overriden in subclasses, such as ObMultiCommand or
	 * TroubleCodesObdCommand.
	 * 
	 * @param out 输出流
	 *  The command to send.
	 */
	protected void sendCommand(OutputStream out) throws IOException,
			InterruptedException {
		// add the carriage return char
		cmd += "\r";

		// write to OutputStream, or in this case a BluetoothSocket
		out.write(cmd.getBytes());
		out.flush();

		/*
		 * HACK GOLDEN HAMMER ahead!!
		 * 
		 * TODO clean
		 * 
		 * Due to the time that some systems may take to respond, let's give it
		 * 500ms.
		 */
		Thread.sleep(200);
	}

	/**
	 * Resends this command.
	 * 重新发送
	 * 
	 */
	protected void resendCommand(OutputStream out) throws IOException,
			InterruptedException {
		out.write("\r".getBytes());
		out.flush();
		/*
		 * HACK GOLDEN HAMMER ahead!!
		 * 
		 * TODO clean this
		 * 
		 * Due to the time that some systems may take to respond, let's give it
		 * 500ms.
		 */
		// Thread.sleep(250);
	}

	/**
	 * Reads the OBD-II response.
	 * This method may be overriden in subclasses, such as ObdMultiCommand.
	 * 从输入流读取数据 到 rowData
	 */
	protected void readResult(InputStream in) throws IOException {
		byte b = 0;
		StringBuilder res = new StringBuilder();

		// read until '>' arrives
		while ((char) (b = (byte) in.read()) != '>')
			if ((char) b != ' ')
				res.append((char) b);

		/*
		 * Imagine the following response 41 0c 00 0d.
		 * 
		 * ELM sends strings!! So, ELM puts spaces between each "byte". And pay
		 * attention to the fact that I've put the word byte in quotes, because
		 * 41 is actually TWO bytes (two chars) in the socket. So, we must do
		 * some more processing..
		 */
		//
		rawData = res.toString().trim();

		// clear buffer
		buffer.clear();

		// read string each two chars
		// 双指针 每次读取两个字符作为一个字节 添加 0x 作为16进制数 添加到缓冲区
		int begin = 0;
		int end = 2;
		while (end <= rawData.length()) {
			String temp = "0x" + rawData.substring(begin, end);
			buffer.add(Integer.decode(temp));
			begin = end;
			end += 2;
		}
	}

	/**
	 * @return the raw command response in string representation.
	 * 包含搜索中或者DATA 文字的标识无数据，其他的正常返回 rawData
	 */
	public String getResult() {
		if (rawData.contains("SEARCHING") || rawData.contains("DATA")) {
			rawData = "NODATA";
		}

		return rawData;
	}

	/**
	 * @return a formatted command response in string representation.
	 */
	public abstract String getFormattedResult();

	/******************************************************************
	 * Getters & Setters
	 */

	/**
	 * @return a list of integers
	 */
	public ArrayList<Integer> getBuffer() {
		return buffer;
	}

	/**
	 * Returns this command in string representation.
	 * 
	 * @return the command
	 */
	public String getCommand() {
		return cmd;
	}

	/**
	 * @return true if imperial units are used, or false otherwise
	 */
	public boolean useImperialUnits() {
		return useImperialUnits;
	}

	/**
	 * Set to 'true' if you want to use imperial units, false otherwise. By
	 * default this value is set to 'false'.
	 * 
	 * @param isImperial
	 */
	public void useImperialUnits(boolean isImperial) {
		this.useImperialUnits = isImperial;
	}

	/**
	 * @return the OBD command name.
	 */
	public abstract String getName();

}