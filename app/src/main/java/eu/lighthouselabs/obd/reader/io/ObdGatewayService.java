/*
 * TODO put header
 */
package eu.lighthouselabs.obd.reader.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import eu.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import eu.lighthouselabs.obd.commands.protocol.ObdResetCommand;
import eu.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import eu.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.ObdProtocols;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.IPostMonitor;
import eu.lighthouselabs.obd.reader.R;
import eu.lighthouselabs.obd.reader.activity.ConfigActivity;
import eu.lighthouselabs.obd.reader.activity.ObdActivity;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob.ObdCommandJobState;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * 
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */
public class ObdGatewayService extends Service {

	private static final String TAG = "ObdGatewayService";

	private NotificationManager _notifManager;
	/**
	 * 对外回调，返回当前执行的任务 出去
	 */
	private IPostListener _callback = null;
	/**
	 * 外部可调用接口
	 */
	private final Binder _binder = new LocalBinder();
	/**
	 * 服务是否运行 flag
	 */
	private AtomicBoolean _isRunning = new AtomicBoolean(false);
	/**
	 * 任务队列
	 */
	private BlockingQueue<ObdCommandJob> _queue = new LinkedBlockingQueue<ObdCommandJob>();
	/**
	 * 当前队列是否执行中 flag
	 */
	private AtomicBoolean _isQueueRunning = new AtomicBoolean(false);
	/**
	 * 队列计数器
	 */
	private Long _queueCounter = 0L;
	/**
	 * 蓝牙设备
	 */
	private BluetoothDevice _dev = null;
	/**
	 * 蓝牙 socket
	 */
	private BluetoothSocket _sock = null;
	/*
	 * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
	 * #createRfcommSocketToServiceRecord(java.util.UUID)
	 * 
	 * "Hint: If you are connecting to a Bluetooth serial board then try using
	 * the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if
	 * you are connecting to an Android peer then please generate your own
	 * unique UUID."
	 * 连接蓝牙串口版 使用下面的uuid
	 * 如果连接 Android设备 需要自己生成uuid
	 */
	private static final UUID MY_UUID = UUID
	        .fromString("00001101-0000-1000-8000-00805F9B34FB");

	/**
	 * As long as the service is bound to another component, say an Activity, it
	 * will remain alive.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	@Override
	public void onCreate() {
		_notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
	}

	@Override
	public void onDestroy() {
		stopService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Received start id " + startId + ": " + intent);

		/*
		 * Register listener Start OBD connection
		 */
		startService();

		/*
		 * We want this service to continue running until it is explicitly
		 * stopped, so return sticky.
		 */
		return START_STICKY;
	}

	private void startService() {
		Log.d(TAG, "启动服务..");

		/*
		 * Retrieve preferences
		 */
		SharedPreferences prefs = PreferenceManager
		        .getDefaultSharedPreferences(this);

		/*
		 * Let's get the remote Bluetooth device
		 * 从sp获取 蓝牙设备id
		 */
		String remoteDevice = prefs.getString(
		        ConfigActivity.BLUETOOTH_LIST_KEY, null);
		if (remoteDevice == null || "".equals(remoteDevice)) {
			Toast.makeText(this, "No Bluetooth device selected",
			        Toast.LENGTH_LONG).show();

			// log error
			Log.e(TAG, "未找到蓝牙设备");

			// TODO kill this service gracefully
			stopService();
		}
		//获取蓝牙适配器
		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		//根据设备Id获取蓝牙设备
		_dev = btAdapter.getRemoteDevice(remoteDevice);

		/*
		 * TODO put this as deprecated Determine if upload is enabled
		 */
		// boolean uploadEnabled = prefs.getBoolean(
		// ConfigActivity.UPLOAD_DATA_KEY, false);
		// String uploadUrl = null;
		// if (uploadEnabled) {
		// uploadUrl = prefs.getString(ConfigActivity.UPLOAD_URL_KEY,
		// null);
		// }

		/*
		 * Get GPS
		 */
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		boolean gps = prefs.getBoolean(ConfigActivity.ENABLE_GPS_KEY, false);

		/*
		 * TODO clean
		 * 
		 * Get more preferences
		 */
		int period = ConfigActivity.getUpdatePeriod(prefs);
		double ve = ConfigActivity.getVolumetricEfficieny(prefs);
		double ed = ConfigActivity.getEngineDisplacement(prefs);
		boolean imperialUnits = prefs.getBoolean(
		        ConfigActivity.IMPERIAL_UNITS_KEY, false);
		ArrayList<ObdCommand> cmds = ConfigActivity.getObdCommands(prefs);

		/*
		 * Establish Bluetooth connection
		 * 
		 * Because discovery is a heavyweight procedure for the Bluetooth
		 * adapter, this method should always be called before attempting to
		 * connect to a remote device with connect(). Discovery is not managed
		 * by the Activity, but is run as a system service, so an application
		 * should always call cancel discovery even if it did not directly
		 * request a discovery, just to be sure. If Bluetooth state is not
		 * STATE_ON, this API will return false.
		 * 
		 * see
		 * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
		 * .html#cancelDiscovery()
		 */
		Log.d(TAG, "停止蓝牙设备搜索");
		btAdapter.cancelDiscovery();

		Toast.makeText(this, "Starting OBD connection..", Toast.LENGTH_SHORT);

		try {
			//开启OBD蓝牙连接
			startObdConnection();
		} catch (Exception e) {
			Log.e(TAG, "建立连接时出错 -> "
			        + e.getMessage());

			// in case of failure, stop this service.
			stopService();
		}
	}

	/**
	 * Start and configure the connection to the OBD interface.
	 * 开启OBD连接 ，主要是蓝牙连接 及 部分OBD命令初始化
	 * @throws java.io.IOException
	 */
	private void startObdConnection() throws IOException {
		Log.d(TAG, "开始OBD连接..");

		// Instantiate a BluetoothSocket for the remote device and connect it.
		// 开启蓝牙通信连接 获取输出流
		_sock = _dev.createRfcommSocketToServiceRecord(MY_UUID);
		_sock.connect();

		// Let's configure the connection.
		Log.d(TAG, "配置连接任务排队..");
		//执行命令
		// AT Z : 复位
		queueJob(new ObdCommandJob(new ObdResetCommand()));
		// AT E0 : 关闭回传
		queueJob(new ObdCommandJob(new EchoOffObdCommand()));

		/*
		 * Will send second-time based on tests.
		 * 
		 * TODO this can be done w/o having to queue jobs by just issuing
		 * command.run(), command.getResult() and validate the result.
		 */
		// AT E0 : 关闭回传
		queueJob(new ObdCommandJob(new EchoOffObdCommand()));
		// AT L0 : 关闭信息后自动加 0x0A
		queueJob(new ObdCommandJob(new LineFeedOffObdCommand()));
		// AT ST 0x62 : 设置ECU返回超时时间62
		queueJob(new ObdCommandJob(new TimeoutObdCommand(62)));

		// AT SP 0 : 设置当前协议，自动搜索并保存
		queueJob(new ObdCommandJob(new SelectProtocolObdCommand(
		        ObdProtocols.AUTO)));
		
		// 01 46 : 环境空气温度任务执行
		queueJob(new ObdCommandJob(new AmbientAirTemperatureObdCommand()));

		Log.d(TAG, "初始化任务队列.");

		// Service is running..
		_isRunning.set(true);

		// Set queue execution counter
		_queueCounter = 0L;
	}

	/**
	 * Runs the queue until the service is stopped
	 */
	private void _executeQueue() {
		Log.d(TAG, "执行队列..");
		//设置队列标志位
		_isQueueRunning.set(true);

		while (!_queue.isEmpty()) {
			ObdCommandJob job = null;
			try {
				job = _queue.take();

				// log job
				Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

				if (job.getState().equals(ObdCommandJobState.NEW)) {
					Log.d(TAG, "Job state is NEW. Run it..");

					job.setState(ObdCommandJobState.RUNNING);
					job.getCommand().run(_sock.getInputStream(),
					        _sock.getOutputStream());
				} else {
					// log not new job
					Log.e(TAG,
					        "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
				}
			} catch (Exception e) {
				job.setState(ObdCommandJobState.EXECUTION_ERROR);
				Log.e(TAG, "Failed to run command. -> " + e.getMessage());
			}

			if (job != null) {
				Log.d(TAG, "Job is finished.");
				job.setState(ObdCommandJobState.FINISHED);
				_callback.stateUpdate(job);
			}
		}

		_isQueueRunning.set(false);
	}

	/**
	 * This method will add a job to the queue while setting its ID to the
	 * internal queue counter.
	 * 将任务加进队列  任务id 位队列计数
	 * @param job
	 * @return
	 */
	public Long queueJob(ObdCommandJob job) {
		_queueCounter++;
		Log.d(TAG, "Adding job[" + _queueCounter + "] to queue..");

		job.setId(_queueCounter);
		try {
			_queue.put(job);
		} catch (InterruptedException e) {
			job.setState(ObdCommandJobState.QUEUE_ERROR);
			// log error
			Log.e(TAG, "Failed to queue job.");
		}

		Log.d(TAG, "Job queued successfully.");
		return _queueCounter;
	}

	/**
	 * Stop OBD connection and queue processing.
	 */
	public void stopService() {
		Log.d(TAG, "Stopping service..");

		clearNotification();
		_queue.removeAll(_queue); // TODO is this safe?
		_isQueueRunning.set(false);
		_callback = null;
		_isRunning.set(false);

		// close socket
		try {
			_sock.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		// kill service
		stopSelf();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// Set the icon, scrolling text and timestamp
		Notification.Builder notification = new Notification.Builder(this);
		notification.setSmallIcon(R.drawable.icon);
		notification.setTicker(getText(R.string.service_started));
		notification.setWhen(System.currentTimeMillis());

		// Launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        new Intent(this, ObdActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setContentTitle(getText(R.string.notification_label));
		notification.setContentText(getText(R.string.service_started));
		notification.setContentIntent(contentIntent);
		// Send the notification.
		_notifManager.notify(124, notification.build());
	}

	/**
	 * Clear notification.
	 */
	private void clearNotification() {
		_notifManager.cancel(R.string.service_started);
	}

	/**
	 * IBinder 对外接口
	 */
	public class LocalBinder extends Binder implements IPostMonitor {
		/**
		 * 设置任务回调
		 * @param callback
		 */
		public void setListener(IPostListener callback) {
			_callback = callback;
		}

		/**
		 * 查询当前运行状态
		 * @return
		 */
		public boolean isRunning() {
			return _isRunning.get();
		}

		/**
		 * 开启队列任务处理
		 */
		public void executeQueue() {
			_executeQueue();
		}

		/**
		 * 添加新任务到队列
		 * @param job obd任务
		 */
		public void addJobToQueue(ObdCommandJob job) {
			Log.d(TAG, "Adding job [" + job.getCommand().getName() + "] to queue.");
			_queue.add(job);

			if (!_isQueueRunning.get())
				_executeQueue();
		}
	}

}