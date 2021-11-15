/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.reader;

import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 *  接口类 IBinder 继承
 *  外部拿到 IBinder 接口后可以执行任务
 */
public interface IPostMonitor {
	/**
	 * 任务回调
	 * @param callback
	 */
	void setListener(IPostListener callback);

	/**
	 * 判断当前是否运行
	 * @return
	 */
	boolean isRunning();

	/**
	 * 执行队列
	 */
	void executeQueue();

	/**
	 * 添加任务
	 * @param job obd任务
	 */
	void addJobToQueue(ObdCommandJob job);
}