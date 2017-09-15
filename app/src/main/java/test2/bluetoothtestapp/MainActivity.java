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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivityClass";
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVERABLE_BT = 0;

    private Button mTurnOn;
    private Button mTurnOff;
    private TextView mStatus;
    private TextView mReceiveMessage;
    private EditText mSendMessage;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothServerSocket btServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord();
    private BluetoothSocket btSocket;

    private OutputStream opStream;
    private InputStream ipStream;
    private BluetoothDevice btDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTurnOn = (Button)findViewById(R.id.turn_on);
        mTurnOff = (Button)findViewById(R.id.turn_off);

        mStatus = (TextView) findViewById(R.id.status);
        mReceiveMessage = (TextView)findViewById(R.id.message);
        mSendMessage = (EditText) findViewById(R.id.send_message);

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
                }
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);

                try {
                    Log.i(TAG, "block, waiting for connection request to accept");
                    btSocket = btServerSocket.accept();
                    if(btSocket!=null){
                        Log.i(TAG, "Connection request accepted");
                        btServerSocket.close();
                        ipStream = btSocket.getInputStream();
                        opStream = btSocket.getOutputStream();
                        btDevice = btSocket.getRemoteDevice();
                        mStatus.setText(btDevice.getName());
                    }

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


}
