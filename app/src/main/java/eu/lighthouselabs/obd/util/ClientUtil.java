package eu.lighthouselabs.obd.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import eu.lighthouselabs.obd.reader.activity.BlueBoothActivity;

/**
 * @author caixingcun
 * @date 2021/11/17
 * Description :
 */
public class ClientUtil {
    private ClientUtil() {
    }

    private static ClientUtil instance;

    public static synchronized ClientUtil getInstance() {
        if (instance == null) {
            instance = new ClientUtil();
        }
        return instance;
    }

    private String serverBlueToothAddress;
    private BluetoothSocket socket = null;
    private BluetoothAdapter bluetoothAdapter;

    public void onCreate(Activity activity) {
        registerBluetoothScanReceiver(activity);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                //弹出系统提示框
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableIntent, Activity.RESULT_FIRST_USER);
                // 设置蓝牙可见性
                Intent displayIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                displayIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                activity.startActivity(displayIntent);
                // 打开蓝牙
                bluetoothAdapter.enable();
                logUtil("打开蓝牙成功");
            } else {
                logUtil("蓝牙已打开");
            }
        } else {
            logUtil("当前设备m没有蓝牙模块");
        }
    }

    /**
     * 扫码设备 onResume中执行，连接页面调用
     *
     * @return
     */
    public Set<BluetoothDevice> scanDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            logUtil("蓝牙状态异常");
            return null;
        }
        Set<BluetoothDevice> bluetoothDevices = new HashSet<>();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        } else {
            bluetoothDevices = bluetoothAdapter.getBondedDevices();
            if (bluetoothDevices.size() > 0) {

            } else {
                logUtil("没有匹配过的设备");
            }
            bluetoothAdapter.startDiscovery();
        }
        return bluetoothDevices;
    }

    private void registerBluetoothScanReceiver(Activity activity) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(receiver, filter);
    }

    private BlueToothConnectCallback blueToothConnectCallback;

    public void connectRemoteDevice(String address, BlueToothConnectCallback callback) {
        this.blueToothConnectCallback = callback;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        new Thread(new ConnectRunnable(device, blueToothConnectCallback)).start();
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if (onFoundDeviceLister != null) {
                        onFoundDeviceLister.foundBondDevice(device);
                    }
                }
            }
        }
    };

    public void logUtil(String msg) {
        Log.d("tag", msg);
    }

    private OnFoundDeviceLister onFoundDeviceLister;

    public void setOnFoundDeviceLister(OnFoundDeviceLister onFoundDeviceLister) {
        this.onFoundDeviceLister = onFoundDeviceLister;
    }

    public interface OnFoundDeviceLister {
        void foundBondDevice(BluetoothDevice device);
    }

    public interface BlueToothConnectCallback {
        void connecting(String address);

        void connectSuccess(String address);

        void connectFail(IOException e);
    }

    public class ConnectRunnable implements Runnable {
        private BluetoothDevice device;
        private BlueToothConnectCallback connectCallback;

        public ConnectRunnable(BluetoothDevice device, BlueToothConnectCallback callback) {
            this.device = device;
            this.connectCallback = callback;
        }

        @Override
        public void run() {
            if (device != null) {
                try {
                    if (socket != null) {
                        closeSocket(socket);
                    }
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    logUtil("正在连接：" + device.getAddress());
                    connectCallback.connecting(device.getAddress());
                    socket.connect();

                } catch (IOException e) {

                }
            }
        }
    }

    private void closeSocket(BluetoothSocket socket) {

    }

}
