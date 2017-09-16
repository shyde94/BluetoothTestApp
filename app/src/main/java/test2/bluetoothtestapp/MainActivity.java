package test2.bluetoothtestapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.util.Output;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivityClass";
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVERABLE_BT = 0;
    private final String NAME = "SD";
    private Thread AcceptThread, ConnectedThread;


    /*
    Only objects running on the UI thread have access to other objects on that thread.
    Because tasks that you run on a thread from a thread pool aren't running on your UI thread,
    they don't have access to UI objects.
    To move data from a background thread to the UI thread, use a Handler that's running on the UI thread.
    Define a Handler on the UI Thread. When you connect a Handler to your UI thread,
    the code that handles messages runs on the UI thread.
    */
    Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            //Get string from incoming text
            Log.i(TAG, "Handle Message");
            String message = "";
            switch(msg.what){
                case 0:
                    Log.i(TAG, "Handler case 0");
                    message = (String) msg.obj;
                    mReceiveMessage.setText(message);
                    super.handleMessage(msg);
                    break;
                case 3:
                    Log.i(TAG, "Case 3");
                    message = (String) msg.obj;
                    mStatus.setText("Connected to: " + message);
                    break;
            }

        }
    };

    private Button mTurnOn;
    private Button mTurnOff;
    private Button mMakeDiscoverable;
    private TextView mStatus;
    private TextView mReceiveMessage;
    private EditText mSendMessage;
    private Button mSendButton;


    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothServerSocket btServerSocket;
    private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;

    private InputStream ipStream;
    private OutputStream opStream;
    private byte[] mmBuffer; // mmBuffer store for the stream

    //private ConnectedThread cDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTurnOn = (Button)findViewById(R.id.turn_on);
        mTurnOff = (Button)findViewById(R.id.turn_off);
        mMakeDiscoverable = (Button) findViewById(R.id.make_discoverable);
        mStatus = (TextView) findViewById(R.id.status);
        mReceiveMessage = (TextView)findViewById(R.id.message);
        mSendMessage = (EditText)findViewById(R.id.send_message);
        mSendButton = (Button) findViewById(R.id.send_button);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);




        if (mBluetoothAdapter == null) {
            mStatus.setText("This device does not support bluetooth");
        }

        mTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    mStatus.setText("Bluetooth turned on");
                }
            }
        });

        mMakeDiscoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBluetoothAdapter.isEnabled()){
                    Intent discoverableIntent =
                            new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                    Log.i(TAG, "block, waiting for connection request to accept");

                    /*AcceptThread accept = new AcceptThread();
                    accept.run();
                    if(btSocket!=null){
                        /*Log.i(TAG, "Device Connected");
                        mStatus.setText("Connected");
                        btDevice = btSocket.getRemoteDevice();
                        cDevice = new ConnectedThread(btSocket);
                        cDevice.run();

                    }*/
                    manageConnectionRequests();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Please turn on bluetooth first", Toast.LENGTH_LONG).show();
                }

            }
        });



        mTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothAdapter.disable();
                mStatus.setText("Bluetooth Turned Off");
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = mSendMessage.getText().toString();
                Log.i(TAG, "Message: " + message);
                try{
                    Log.i(TAG,"Sending message");
                    write(message);
                }catch(NullPointerException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    public void manageConnectionRequests(){
        AcceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"In acceptThread");
                UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
                try {
                    // MY_UUID is the app's UUID string, also used by the client code.
                    btServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);

                } catch (IOException e) {
                    Log.i(TAG, "Socket's listen() method failed", e);
                }
                try {
                    Log.i(TAG,"In acceptThread");
                    listenForConnectionRequests(btServerSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listenForIncomingMessages();
            }
        });
        AcceptThread.start();
    }

    private void listenForConnectionRequests(BluetoothServerSocket btServerSocket) throws IOException {
        while(true){
            btSocket = btServerSocket.accept();
            if (btSocket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                Log.i(TAG, "Found connection. ");

                //Call handler here? perhaps.
                ipStream = btSocket.getInputStream();
                opStream = btSocket.getOutputStream();
                btDevice = btSocket.getRemoteDevice();
                String DeviceName = btDevice.getName();
                Message readMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_CONNECTED, DeviceName);
                readMsg.sendToTarget();
                try {
                    Log.i(TAG, "Closing bluetoothServerSocket");
                    btServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

        }
    Log.i(TAG,"Exciting listenForConnectionRequests");
    }

    private void listenForIncomingMessages(){
        ConnectedThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()
                while (true) {
                    Log.i(TAG, "Listening for data");
                    try {
                        // Read from the InputStream.
                        numBytes = ipStream.read(mmBuffer);
                        // Send the obtained bytes to the UI activity.
                        final String readMessage = new String(mmBuffer, 0, numBytes);
                        Message readMsg = mHandler.obtainMessage(
                                MessageConstants.MESSAGE_READ, numBytes, -1,
                                readMessage);
                        readMsg.sendToTarget();
                        Log.i(TAG, "Message readMsg: " + readMsg.toString());

                        //This probably doesnt work because its from a different thread.
                        Log.i(TAG, "message: " + readMessage);
                    } catch (IOException e) {
                        //Should include code to automatically reconnect to device!
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }
        });
        ConnectedThread.start();
    }

    public void write(String s) throws IOException {
        Log.d(TAG, "Last send: "+s);
        if(opStream != null){
            opStream.write((s+"|").getBytes());
        }
        else{
            Log.i(TAG, "Bluetooth not connected");
        }

    }

    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;
        int MESSAGE_CONNECTED = 3;

        // ... (Add other message types here as needed.)
    }

    /*private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            Log.i(TAG,"In acceptThread");
            BluetoothServerSocket tmp = null;
            UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
            } catch (IOException e) {
                Log.i(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            Log.i(TAG,"AcceptThread run");
            while (true) {
                Log.i(TAG,"In while loop");
                try {
                    //Accept incoming connections
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.i(TAG, "Socket's accept() method failed", e);
                    break;
                }
                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.i(TAG, "Found connection. ");
                    btSocket = socket;
                    //Call handler here? perhaps.
                    try {
                        Log.i(TAG, "Closing bluetoothServerSocket");
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            Log.i(TAG,"Exciting acceptThread thread");
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }*/






    /*private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream


        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG,"Connection thread run");
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                Log.i(TAG, "Listening for data");
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                    Log.i(TAG, "Message readMsg: " + readMsg.toString());
                    final String readMessage = new String(mmBuffer, 0, numBytes);
                    //This probably doesnt work because its from a different thread.
                    Log.i(TAG, "message: " + readMessage);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                Log.i(TAG,"sending data");
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                /*Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }*/




}
