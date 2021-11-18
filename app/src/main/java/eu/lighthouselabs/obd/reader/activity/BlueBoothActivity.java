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
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import eu.lighthouselabs.obd.reader.R;

public class BlueBoothActivity extends AppCompatActivity {
    Map<String, BluetoothDevice> devices = new HashMap<>();
    LinearLayout ll;
    Button btn;
    TextView tv;
    EditText et;
    EditText etNameRegex;
    Button btnSearch;
    RecyclerView rv;
    private BluetoothAdapter blueBoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private String deviceNameRegex = "";
    private SimpleAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_booth);
        btn = findViewById(R.id.btn);
        et = findViewById(R.id.et);
        tv = findViewById(R.id.tv);
        ll = findViewById(R.id.ll);
        rv = findViewById(R.id.rv);
        initRv();
        etNameRegex = findViewById(R.id.et_name);
        btnSearch = findViewById(R.id.btn_search);
        ll.setVisibility(View.INVISIBLE);

        etNameRegex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                deviceNameRegex = s.toString().trim();
            }
        });
        btn.setOnClickListener(v -> {
            String text = et.getText().toString();
            handerThread.send(text);
        });


        btnSearch.setOnClickListener(v -> {
            startDiscoverBlueBoothDevices();
        });

        Set<BluetoothDevice> bondedDevices = blueBoothAdapter.getBondedDevices();

        for (BluetoothDevice bondedDevice : bondedDevices) {
            if (bondedDevice.getAddress().equals("00:1A:7D:DA:71:13")) {
                connectBlueTooth(bondedDevice);
                return;
            }
        }
    }

    List<String> mList = new ArrayList<>();

    private void initRv() {
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new SimpleAdapter(this, mList);
        rv.setAdapter(mAdapter);
        mAdapter.setRvListener(pos -> {
            BluetoothDevice device = this.devices.get(mList.get(pos));
            connectBlueTooth(device);
        });
    }

    private void registerBlueDiscoveryReceive() {
        IntentFilter intentFilter = new IntentFilter();
        //注册发现监听
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        //注册状态变化
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //注册 蓝牙设备搜索完成状态
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(blueBoothReceiver, intentFilter);
    }

    private void startDiscoverBlueBoothDevices() {
        if (blueBoothAdapter == null) {
            toast("无蓝牙设备");
            return;
        }
        if (!blueBoothAdapter.isEnabled()) {
            toast("请打开蓝牙");
            blueBoothAdapter.enable();
            return;
        }

        registerBlueDiscoveryReceive();

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
                String name = device.getName();
                devices.put(name + ":" + device.getAddress(), device);
                Log.d("tag", name + ":" + device.getAddress());
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                //状态变化
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                devices.put(name + ":" + device.getAddress(), device);
                Log.d("tag", name + ":" + device.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                toast("蓝牙设备搜索完成");
                Log.d("tag", "蓝牙设备搜索完成");
            }
        }
    };


    private BlueToothConnectThread handerThread;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == BlueToothConnectThread.WHAT_READ) {
                toast((String) msg.obj);
            }
        }
    };



    /**
     * 连接蓝牙设备
     *
     * @param device
     */
    private void connectBlueTooth(BluetoothDevice device) {
        handerThread = new BlueToothConnectThread(handler, device, new BlueToothConnectCallback() {
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
        }, msg -> runOnUiThread(() -> tv.setText(msg)));
        handerThread.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueBoothReceiver);
        if (blueBoothAdapter != null) {
            if (blueBoothAdapter.isDiscovering()) {
                blueBoothAdapter.cancelDiscovery();
            }
        }
    }

    public void toast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(BlueBoothActivity.this, msg, Toast.LENGTH_LONG).show());
    }

    public void setBtn(Button btn) {
        this.btn = btn;
    }


    public static class BlueToothConnectThread extends Thread implements SocketHandler {
        private static final UUID BluetoothUUid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket bluetoothSocket;
        BluetoothDevice bluetoothDevice;
        private boolean connected = false;
        private Object lock = new Object();
        private BlueToothConnectCallback connectCallback;
        private ReceiveCallback receiveCallback;
        private byte[] buffer = new byte[1024];
        private Handler handler;
        public static final int WHAT_READ = 1;

        public BlueToothConnectThread(Handler handler, BluetoothDevice device, BlueToothConnectCallback callback, ReceiveCallback rCallback) {
            try {
                this.handler = handler;
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

                while (connected) {
                    try {
                        InputStream inputStream = bluetoothSocket.getInputStream();
                        int read = inputStream.read(buffer);


                        Message msg = Message.obtain(handler, WHAT_READ, read, -1, new String(buffer));
                        msg.sendToTarget();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


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

    public class SimpleAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private Context mContext;
        private List<String> mList;
        private MyRvListener rvListener;

        public SimpleAdapter(Context context, List<String> list) {
            this.mContext = context;
            this.mList = list;
        }

        public void setRvListener(MyRvListener rvListener) {
            this.rvListener = rvListener;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_simple, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.tv.setText(mList.get(position));
            holder.tv.setOnClickListener(v -> {
                if (rvListener != null) {
                    rvListener.click(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv);
        }
    }

    public interface MyRvListener {
        void click(int pos);
    }

}

