package eu.lighthouselabs.obd.reader.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.lighthouselabs.obd.reader.R;

public class BlueBoothActivity extends AppCompatActivity {
    Map<String, BluetoothDevice> devices = new HashMap<>();
    LinearLayout ll;
    Button btn;
    TextView tv;
    EditText et;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_booth);
        btn = findViewById(R.id.btn);
        et = findViewById(R.id.et);
        tv = findViewById(R.id.tv);
        ll = findViewById(R.id.ll);
        ll.setVisibility(View.INVISIBLE);

        btn.setOnClickListener(v -> {
            String text = et.getText().toString();
            handerThread.send(text);
        });

        BluetoothAdapter blueBoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueBoothAdapter == null) {
            toast("无蓝牙设备");
            return;
        }
        if (!blueBoothAdapter.isEnabled()) {
            toast("请打开蓝牙");
            blueBoothAdapter.enable();
            return;
        }

        //注册发现监听
        registerReceiver(blueBoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        //注册状态变化
        registerReceiver(blueBoothReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        //注册 蓝牙设备搜索完成状态
        registerReceiver(blueBoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        //开启蓝牙搜索
        if (blueBoothAdapter.isDiscovering()) {
            blueBoothAdapter.cancelDiscovery();
        }
        blueBoothAdapter.startDiscovery();
    }

    private BroadcastReceiver blueBoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //发现设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                String name = device.getName();
                int status = device.getBondState();
//                BluetoothDevice.BOND_BONDED; //已配对
//                BluetoothDevice.BOND_BONDING; //配对中
//                BluetoothDevice.BOND_NONE; //未配对或者取消
                Log.d("tag", device.getName() + " " + device.getAddress());
                if (device.getName() != null && device.getName().startsWith("DESKTOP-8")) {
                    devices.put(device.getName() + "_" + device.getAddress(), device);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                toast("状态变化");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                toast("蓝牙设备搜索完成");
                Log.d("tag", "蓝牙设备搜索完成");
                showDeviceDialog();
            }
        }
    };

    /**
     * 选择需要匹配的设备
     */
    private void showDeviceDialog() {
        List<String> temps = new ArrayList<>();
        for (Map.Entry<String, BluetoothDevice> entry : devices.entrySet()) {
            temps.add(entry.getKey());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设备");
        builder.setItems(temps.toArray(new String[temps.size()]), (dialog, which) -> {
            BluetoothDevice device = devices.get(temps.get(which));
            connectBlueTooth(device);
        });
        builder.show();
    }

    private BlueToothConnectThread handerThread;

    /**
     * 连接蓝牙设备
     *
     * @param device
     */
    private void connectBlueTooth(BluetoothDevice device) {
        handerThread = new BlueToothConnectThread(device, new BlueToothConnectCallback() {
            @Override
            public void connectSuccess(BluetoothSocket socket) {
                toast("连接成功");
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write("hello world".getBytes());
                    outputStream.flush();
                    runOnUiThread(() -> ll.setVisibility(View.VISIBLE));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void connectFailed(String errorMessage) {
                toast("连接失败");

            }

            @Override
            public void connectCancel() {
                toast("连接取消");

            }
        }, new ReceiveCallback() {
            @Override
            public void receive(String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(msg);
                    }
                });
            }
        });
        handerThread.start();
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    public void toast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(BlueBoothActivity.this, msg, Toast.LENGTH_LONG).show());
    }


    public static class BlueToothConnectThread extends Thread implements SocketHandler {
        private static final UUID BluetoothUUid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket bluetoothSocket;
        BluetoothDevice bluetoothDevice;
        private boolean connected = false;
        private Object lock = new Object();
        private BlueToothConnectCallback connectCallback;
        private ReceiveCallback receiveCallback;

        public BlueToothConnectThread(BluetoothDevice device, BlueToothConnectCallback callback, ReceiveCallback rCallback) {
            try {
                connectCallback = callback;
                bluetoothDevice = device;
                receiveCallback = rCallback;

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BluetoothUUid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            new Thread() {
                @Override
                public void run() {
                    connect();
                }
            }.start();
        }

        public void connect() {
            try {
                bluetoothSocket.connect();
                connected = true;
                if (connectCallback != null) {
                    connectCallback.connectSuccess(bluetoothSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
                cancel();
            }
        }

        public void cancel2() {
            try {
                synchronized (lock) {
                    bluetoothSocket.close();
                    connected = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                synchronized (lock) {
                    bluetoothSocket.close();
                    connected = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void send(String msg) {
            if (bluetoothSocket != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = bluetoothSocket.getOutputStream();
                    outputStream.write(msg.getBytes());
                    outputStream.flush();
                    Log.d("tag", "发送结束");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void release() {
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public interface BlueToothConnectCallback {

        void connectSuccess(BluetoothSocket socket);

        void connectFailed(String errorMessage);

        void connectCancel();
    }

    public interface SocketHandler {
        void send(String msg);

        void release();

    }

    public interface ReceiveCallback {
        void receive(String msg);
    }
}

