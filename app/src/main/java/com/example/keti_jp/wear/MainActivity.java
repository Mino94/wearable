package com.example.keti_jp.wear;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.util.Log.e;

public class MainActivity extends WearableActivity {

    private TextView textView = null;
    //private EditText editText = null;
    private Button send =null;
    private BluetoothAdapter    mBTAdapter = null;
    public BluetoothService    mBTService = null;

    public boolean DEBUG = false;
    public String TAG = "WearableActivity";

    // sensor
    SensorManager sm;
    SensorEventListener multipleListener;
    SensorEventListener accL;
    SensorEventListener oriL;
    Sensor gyroSensor;
    Sensor accSensor;
    TextView ax, ay, az;
    TextView gx, gy, gz;

    public static boolean isStart = false;

    //float[] floatarr = new float[72000];
    //float[] floatarr = new float[6];
    float[] accValue = new float[3];
    float[] gyrValue = new float[3];

    byte[] accHeader = new byte[] { (byte)0x61, (byte)0x63 };
    byte[] gyrHeader = new byte[] { (byte)0x67, (byte)0x72 };



    Thread thread;
    int i = 0;
    boolean mlond=true;
    private final Handler mHandler = new Handler() {
        //todo check bluetooth state
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.STATE_NONE:
                    if(DEBUG) Log.d(TAG, "BT State: STATE_NONE");
                    break;
                case Constants.STATE_LISTEN:
                    if(DEBUG) Log.d(TAG, "BT State: STATE_LISTEN");
                    break;
                case Constants.STATE_CONNECTING:
                    if(DEBUG) Log.d(TAG, "BT State: STATE_CONNECTING");
                    break;
                case Constants.STATE_CONNECTION_FAILED:
                    if(DEBUG) Log.d(TAG, "BT State: STATE_CONNECTION_FAILED");
                    break;
                case Constants.STATE_CONNECTED:
                    if(DEBUG) Log.d(TAG, "BT State: STATE_CONNECTED");
                    updateUI(Constants.UI_STATE_CONNECTED, null);
                    break;
                case Constants.STATE_DISCONNECTED:
                    if(DEBUG) Log.d(TAG, "BT State: STATE_DISCONNECTED");
                    startBTServer();
                    break;
                case Constants.STATE_CONNECTION_LOST:
                    if(DEBUG) Log.d(TAG, "BT State: STATE_CONNECTION_LOST");
                    startBTServer();
                    break;
                case Constants.STATE_MESSAGE_READ:
                    //String messg = String.format("%02X", msg.getData().getByteArray(Constants.READ_MESSAGE));
                    //if(DEBUG) Log.d(TAG, "received : " + messg);
                    updateUI(Constants.UI_STATE_SENSOR_LIST, msg.getData().getByteArray(Constants.READ_MESSAGE));
                    break;
                case Constants.STATE_MESSAGE_WRITE:
                    Log.e(TAG,"버튼 이벤트로 상태 변환 성공");
                    //thread = new BufferThread();
                    isStart = true;
                    //thread.start();
                    Log.e(TAG, "thread start");
                    stopThread();
                    Log.e(TAG, "thread stop method");
                    break;
                case Constants.STATE_DEVICE_LIST_VIEW:
                    // todo : do something
                    break;
                default:
                    break;
            }
        }
    };
    Timer timer = new Timer();
    public void stopThread(){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isStart = false;

            }
        },1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text);
        //editText = findViewById(R.id.editText);
        send = findViewById(R.id.send);

        textView.setText("not connected");
        //mSensorsManager = new SensorsManager(this, mHandler);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
        setAmbientEnabled();

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);    // SensorManager 인스턴스를 가져옴


        multipleListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                // 문제
                if(MainActivity.isStart){ // send actual sensor data
                    if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        //TODO: get values
                        ax.setText(Float.toString(event.values[0]));
                        ay.setText(Float.toString(event.values[1]));
                        az.setText(Float.toString(event.values[2]));

                        accValue[0] = event.values[0]; // 가속도 X축 데이터
                        accValue[1] = event.values[1]; // 가속도 Y축 데이터
                        accValue[2] = event.values[2]; // 가속도 Z축 데이터


                    }else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        //TODO: get values
                        gyrValue[0] = event.values[0];  // 자이로 X축
                        gyrValue[1] = event.values[1];  // 자이로 Y축
                        gyrValue[2] = event.values[2];  // 자이로 Z축

                        gyrValue[0] = event.values[0]; // 자이로 X축 데이터
                        gyrValue[1] = event.values[1]; // 자이로 X축 데이터
                        gyrValue[2] = event.values[2]; // 자이로 X축 데이터
                        Log.e(TAG, "gyrValue[0]" + gyrValue[0] +"gyrValue[1]" + gyrValue[1]+"gyrValue[2]" + gyrValue[2]);
                    }else{
                        Log.e(TAG, "Sensor Not Supported!!!");
                    }

                    ByteBuffer accBuffer= ByteBuffer.allocate(2+accValue.length*4);
                    Log.e(TAG, "accBuffer:"+accBuffer.toString());

                    ByteBuffer gyrBuffer= ByteBuffer.allocate(2+gyrValue.length*4);
                    //Log.e(TAG,"변환:"+"성공" +accValue[0]);
                    //Log.e(TAG,"acc길이"+accValue.length);
                    accBuffer.put(accHeader);
                    //for(int i = 0; i<accBuffer.; i++)
                    Log.e(TAG, "accBuffer(header):"+accBuffer.toString());

                    for (float value : accValue){
                        accBuffer.putFloat(value); // float(4B)*6 =24
                    }
                    Log.e(TAG, "accBuffer(after header):"+accBuffer.toString());

                    gyrBuffer.put(gyrHeader);
                    Log.e(TAG, "gyrBuffer( header):"+gyrBuffer.toString());
                    for (float value : gyrValue){
                        gyrBuffer.putFloat(value); // float(4B)*6 =24
                    }
                    Log.e(TAG, "gyrBuffer(after header):"+gyrBuffer.toString());

                    ByteBuffer data = ByteBuffer.allocate(28);
                    data.put(accBuffer.array());
                    data.put(gyrBuffer.array());
                    Log.e(TAG, "data(after header):"+data.toString());

                    byte[] array = data.array();
                    for(int i = 0; i<array.length; i++)
                        Log.e(TAG, "array:"+ array[i]);

                    Log.e(TAG, "array end");
                    mBTService.write(array);
                }
                else{
                    Log.e(TAG,"Not");

                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        //gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);    //  자이로 센서
        //accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);    // 가속도 센서
        //soriL = new oriListener();        // 자이로 센서 리스너 인스턴스
        //accL = new accListener();       // 가속도 센서 리스너 인스턴스
        ax = (TextView)findViewById(R.id.acc_x);
        ay = (TextView)findViewById(R.id.acc_y);
        az = (TextView)findViewById(R.id.acc_z);
        gx = (TextView)findViewById(R.id.ori_x);
        gy = (TextView)findViewById(R.id.ori_y);
        gz = (TextView)findViewById(R.id.ori_z);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = mHandler.obtainMessage(Constants.STATE_MESSAGE_WRITE);
                mHandler.sendMessage(msg);

            }
        });
        if (mBTAdapter == null) {
            finish();
        }
        else {
            Log.e(TAG, "startBTServer");
            mBTService = new BluetoothService(this, mHandler);
            startBTServer();
        }
        // Enables Always-on
    }

    @Override
    public void onResume() {
        super.onResume();
        sm.registerListener(multipleListener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(multipleListener, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
        //sm.registerListener(accL, accSensor, SensorManager.SENSOR_DELAY_FASTEST);    // 가속도 센서 리스너 오브젝트를 등록
        //sm.registerListener(oriL, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);    // 방향 센서 리스너 오브젝트를 등록
    }
    @Override
    public void onPause() {
        super.onPause();

        sm.unregisterListener(oriL);    // unregister acceleration listener
        sm.unregisterListener(accL);    // unregister orientation listener
    }
/*

    private class accListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {  // 가속도 바뀔때마다 호출됨센서 값이
            ax.setText(Float.toString(event.values[0]));
            ay.setText(Float.toString(event.values[1]));
            az.setText(Float.toString(event.values[2]));

            if(isStart){
                accValue[0] = event.values[0]; // 가속도 X축 데이터
                accValue[1] = event.values[1]; // 가속도 Y축 데이터
                accValue[2] = event.values[2]; // 가속도 Z축 데이터

                ByteBuffer byteBuffer= ByteBuffer.allocate(accValue.length*4);
                //Log.e(TAG,"변환:"+"성공" +accValue[0]);
                //Log.e(TAG,"acc길이"+accValue.length);
                for (float value : accValue){
                    byteBuffer.putFloat(value);
                }

                byte[] data= byteBuffer.array();
                byte[] array = new byte[2+data.length];
                array[0] = (byte)'a';
                array[1] = (byte)'c';
                for(int i = 2; i<array.length; i++){
                    array[i] = data[i-2];
                }
                Log.e(TAG, "acc len:"+array.length);
                mBTService.write(array);
            }
            //Log.i("SENSOR", "Acceleration changed.");
            Log.i("SENSOR", "  Acceleration X: " + event.values[0]
                    + ", Acceleration Y: " + event.values[1]
                    + ", Acceleration Z: " + event.values[2]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private class oriListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {  // 방향 센서 값이 바뀔때마다 호출됨
            gx.setText(Float.toString(event.values[0]));
            gy.setText(Float.toString(event.values[1]));
            gz.setText(Float.toString(event.values[2]));

            if(isStart){
                gyrValue[0] = event.values[0];  // 자이로 X축
                gyrValue[1] = event.values[1];  // 자이로 Y축
                gyrValue[2] = event.values[2];  // 자이로 Z축
                //Log.e(TAG,"gyro성공2" );

                ByteBuffer byteBuffer= ByteBuffer.allocate(gyrValue.length*4);
                //Log.e(TAG,"gyro 변환:"+"성공" +gyrValue[2]);
                //Log.e(TAG,"길이"+gyrValue.length);
                for (float value : gyrValue){
                    byteBuffer.putFloat(value);
                }

                //byte[] header = new byte[2];
                //header[0] = 'g';
                //header[1] = 'r';
                byte[] data= byteBuffer.array();
                byte[] array = new byte[2+data.length];
                array[0] = (byte)'g';
                array[1] = (byte)'r';
                for(int i = 2; i<array.length; i++){
                    array[i] = data[i-2];
                }
                Log.e(TAG, "gyr len:"+array.length);
                mBTService.write(array);
            }
          */
/*  //Log.i("SENSOR", "Orientation changed.");
            Log.i("SENSOR", "  Orientation X: " + event.values[0]
                    + ", Orientation Y: " + event.values[1]
                    + ", Orientation Z: " + event.values[2]);*//*

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
*/

    public void startBTServer(){

        if(mBTService != null){
            //mIsBTConnected = false;
            mBTService.start();

            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            this.registerReceiver(mReceiver, filter);
            ensureDiscoverable();

            updateUI(Constants.UI_STATE_WAITING_CONNECT_DEVICE, null);
        }

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int preScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                Log.d("INFO","preScanMode="+preScanMode+",scanMode="+scanMode);
            }
        }
    };


    private void ensureDiscoverable() {
        if (mBTAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Log.e(TAG, "discoverable");
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
            startActivity(discoverableIntent);
        }
    }

    public void updateUI(int state, byte[] arg) {
        switch (state) {
            case Constants.UI_STATE_CONNECTED:
                if (DEBUG) Log.d(TAG, "UI_STATE_CONNECTED");  //2
                textView.setText("connected");
                break;
            case Constants.UI_STATE_SENSOR_LIST:
                String msg = new String(arg);
                Log.e(TAG, "msg"+msg);
                textView.setText(msg);
                break;
            default:
                break;
        }
    }

    @Override
    public void onStop()
    {
        //sensorRcv = false;
        //mSensorsManager.StopSensorReceiver();
        if(mBTService != null)
        {
            mBTService.stop();
            mBTService = null;
            //mIsBTConnected = false;
        }

        try {
            this.unregisterReceiver(mReceiver);
        }catch (IllegalArgumentException e){
            //noting to do
        }

        //if(mAdapter != null){
            //mAdapter.removeAllItem();
            //mAdapter = null;
        //}
        super.onStop();
    }

}
