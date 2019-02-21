package com.example.keti_jp.wear;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";
    private static boolean D = true;

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE     = UUID.fromString(Constants.STRING_UUID_SECURE);
    private static final UUID MY_UUID_INSECURE   = UUID.fromString(Constants.STRING_UUID_INSECURE);
    private static final UUID MY_UUID_SPP_SECURE = UUID.fromString(Constants.STRING_UUID_SPP_SECURE);

    // Member fields
    private final BluetoothAdapter mBTAdapter_service;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private findSupportingDevice mFindSupportDevice;
    private int mState;
    public static final int STATE_CONNECTED = 3;


    public BluetoothService(Context context, Handler handler){
        mBTAdapter_service = BluetoothAdapter.getDefaultAdapter();
        mState = Constants.STATE_NONE;
        mHandler = handler;
    }

    public synchronized void setBTState(int state){
        if(D) Log.d(TAG, "setState()" + mState + "->" + state);
        mState=state;
        // Give the new state to the Handler so the UI Activity can upate
        // mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getBTState(){ return mState; }

    public synchronized void start(){
        if(D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}

        Message msg = mHandler.obtainMessage(Constants.STATE_LISTEN);
        mHandler.sendMessage(msg);
        setBTState(Constants.STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if(mSecureAcceptThread == null){
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }

//        if(mInsecureAcceptThread == null){
//            mInsecureAcceptThread = new AcceptThread(false);
//            mInsecureAcceptThread.start();
//            Log.d(TAG, "---> Insecure");
//        }
    }

    public synchronized void connect(BluetoothDevice device, boolean secure){

        if(D) Log.d(TAG, "connected to " + device);

        //Cancel any thread attempting to make a connection
        if(mState == Constants.STATE_CONNECTING){
            if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        }

        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}


        // If we're having uuid supporting bluetooth services
        mFindSupportDevice = new findSupportingDevice(device);

        if(mFindSupportDevice.isSupport()) {
            mConnectThread = new ConnectThread(device, secure, mFindSupportDevice.getUuid());
            mConnectThread.start();

            Message msg = mHandler.obtainMessage(Constants.STATE_CONNECTING);
            mHandler.sendMessage(msg);

            setBTState(Constants.STATE_CONNECTING);
        }
        else{
            connectionFailed();
        }


    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device ,
                                       final String socketType){
        if(D) Log.d(TAG, "connected, Socket Type: " + socketType);

        //Cancel the thread that completed the connection
        if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}

        //Cancel the accept thread becauset we only want to connect to one device
        if(mSecureAcceptThread != null){ mSecureAcceptThread.cancel(); mSecureAcceptThread = null;}
        if(mInsecureAcceptThread != null){ mInsecureAcceptThread.cancel(); mInsecureAcceptThread = null;}

        //Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        //Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.STATE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_MAC_ADDR, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setBTState(Constants.STATE_CONNECTED);
    }

    public synchronized void stop(){
        if(D) Log.d(TAG, "stop");

        //Cancel the thread that completed the connection
        if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
        if(mConnectedThread != null){mConnectedThread.cancel(); mConnectedThread = null;}

        //Cancel the accept thread because we only want to connect to one device
        if(mSecureAcceptThread != null){ mSecureAcceptThread.cancel(); mSecureAcceptThread = null;}
        if(mInsecureAcceptThread != null){ mInsecureAcceptThread.cancel(); mInsecureAcceptThread = null;}

        Message msg = mHandler.obtainMessage(Constants.STATE_DISCONNECTED);
        mHandler.sendMessage(msg);

        setBTState(Constants.STATE_DISCONNECTED);
    }

    /**
            * Write to the ConnectedThread in an unsynchronized manner
     *
             * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out){
        //Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectThread
        synchronized(this){
            if(mState != Constants.STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        Log.d(TAG, "outside write");
        r.write(out);
    }

    private void connectionFailed(){
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.STATE_CONNECTION_FAILED);
        mHandler.sendMessage(msg);

        setBTState(Constants.STATE_CONNECTION_FAILED);
        //Start the service over to restart listening mode
        //todo check
        BluetoothService.this.start();
    }

    private void connectionLost(){
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.STATE_CONNECTION_LOST);
        mHandler.sendMessage(msg);

        setBTState(Constants.STATE_CONNECTION_LOST);
        // Start the service over to restart listening mode
        //todo check
        BluetoothService.this.start();
    }

    private class AcceptThread extends Thread {
        //The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure){
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            //Create a new listening server socket
            try{
                if(secure){
                    tmp = mBTAdapter_service.listenUsingRfcommWithServiceRecord(Constants.NAME_SECURE,
                            MY_UUID_SECURE);
                }
                else{
                    tmp = mBTAdapter_service.listenUsingRfcommWithServiceRecord(Constants.NAME_INSECURE,
                            MY_UUID_INSECURE);
                }
            }catch(IOException e){
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed ", e);
            }
            mmServerSocket = tmp;
        }

        public void run(){
            if(D) Log.d(TAG, "SocketType:" + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            //Listen to the server socket if we're not connected
            while(mState != Constants.STATE_CONNECTED){
                try{
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                }catch (IOException e){
                    Log.e(TAG, "Socket Type:" + mSocketType + "accept() failed", e);
                    break;
                }

                //If a connection was accepted
                if(socket != null){
                    synchronized (BluetoothService.this){
                        switch(mState){
                            case Constants.STATE_LISTEN:
                            case Constants.STATE_CONNECTING:
                                //Situation normal. Start the connected thread
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case Constants.STATE_NONE:
                            case Constants.STATE_CONNECTED:
                                try{
                                    socket.close();
                                }catch(IOException e){
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if(D) Log.i(TAG, "End mAcceptThread , SocketTpye :"+ mSocketType);
        }

        public void cancel(){
            if(D) Log.d(TAG, "Socket Type" + mSocketType + "cancel" + this);
            try{
                mmServerSocket.close();
            }catch(IOException e){
                Log.e(TAG, "Socket Type"+ mSocketType + "close() of server failed", e);
            }
        }
    }

    private class findSupportingDevice {

        // device
        private BluetoothDevice bDevice;
        // active uuid service
        private UUID activeUuid;
        // confirm the permission using bluetooth module
        private boolean isAcess;

        public findSupportingDevice(BluetoothDevice device){

            init(device);

        }

        public void init(BluetoothDevice device){

            bDevice = device;
            // allocate the uuid service
            isAcess = findService();

        }

        public UUID getUuid(){
            return activeUuid;
        }

        public boolean isSupport(){
            return isAcess;
        }

        private boolean findService(){
            ParcelUuid[] deviceUuids = bDevice.getUuids();

            if (deviceUuids == null){
                // allocate default uuid Service
                activeUuid = MY_UUID_SECURE;
                return true;
            }

            for(ParcelUuid uuid : deviceUuids){

                if((uuid.getUuid()).equals(MY_UUID_SPP_SECURE)) {
                    Log.d(TAG, "SUPPORT UUID:" + uuid.getUuid());
                    activeUuid = uuid.getUuid();
                    return true;
                }
                else if((uuid.getUuid()).equals(MY_UUID_SECURE)) {
                    Log.d(TAG, "SUPPORT UUID:" + uuid.getUuid());
                    activeUuid = uuid.getUuid();
                    return true;

                }
                else{
                    activeUuid = MY_UUID_SECURE;
                    return true;
                }
            }
            return false;
        }

    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mmSokcetType;

        public ConnectThread(BluetoothDevice device , boolean secure, UUID activeUuid){
            mmDevice = device;
            BluetoothSocket tmp = null;
            mmSokcetType = secure? "Secure":"Insecure";

            // Get a BluetoothSocket for a connection with
            // given BluetoothDevice
            try{
                if(secure){
                    tmp = device.createRfcommSocketToServiceRecord(activeUuid);
                }
                else{
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            }catch(IOException e){
                Log.e(TAG, "SokcetType:" + mmSokcetType +"create() failed", e);
            }

            mmSocket = tmp;
        }

        public void run(){
            Log.i(TAG, "BEGIN mConnectThread socketType:" + mmSokcetType);
            setName("ConnectThread" + mmSokcetType);

            //Always cancel discovery because it will slow down a connection
            mBTAdapter_service.cancelDiscovery();

            //Make a connection to the BluetoothSocket
            try{
                // This is a blocking call and will only return on a
                // sucessful connection or an exception
                mmSocket.connect();
            }catch(IOException e){
                //close the socket
                try{
                    mmSocket.close();
                }catch(IOException cloE){
                    Log.e(TAG, "unable to close()" + mmSokcetType +
                            " socket during connection failure", cloE);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized(BluetoothService.this){
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mmSokcetType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mmSokcetType + " socket failed", e);
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType){
            Log.d(TAG, "create ConnectedThread" + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

            }catch(IOException e){
                Log.e(TAG, "temp socket not created",e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run(){
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            byte ch;
            int bytes;
            // Keep listening to the InputStream while connected
            //while(mState != Constants.STATE_CONNECTED){
            while(true){
                try{
                    bytes = 0;
                    //Read from the InputStream
                    //mmInStream.reset();

                    while(bytes < 10) {
                        ch = (byte)mmInStream.read();
                        buffer[bytes++] = ch;
                        Log.d(TAG, "wear got character(" + bytes+ ") : "+ String.format("%02X", buffer[bytes]));
                        //bytes++;
                    }

                    Log.d(TAG, "wear got buf size:"+bytes);
                    Log.d(TAG, "wear got character--------------");
                    //buffer[bytes] = '\0';

                    Message msg = mHandler.obtainMessage(Constants.STATE_MESSAGE_READ);
                    Bundle bundle = new Bundle();
                    bundle.putByteArray(Constants.READ_MESSAGE, buffer);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);

                    //setState(Constants.STATE_MESSAGE_READ);
                }catch(IOException e){
                    //Log.e(TAG, "disconnected", e);
                    if(getBTState() == Constants.STATE_CONNECTED){
                        Log.d(TAG, "disconnected", e);
                        connectionLost();
                    }


                    //Start the service over to restart listening mode
                    //todo check
                    //BluetoothService.this.start();
                    break;
                }
            }
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer){
            try{
                Log.d(TAG, "inside write");
                for(int i =0; i<buffer.length; i++){
                    Log.d(TAG, "inside buffer["+i+"]" + buffer[i]);
                }

                mmOutStream.write(buffer);

                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();

                //setState(Constants.STATE_MESSAGE_WRITE);

            }catch(IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException e){
                Log.e(TAG, "close() of connection socket failed", e);
            }
        }
    }
}

