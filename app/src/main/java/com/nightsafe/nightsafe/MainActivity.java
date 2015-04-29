package com.nightsafe.nightsafe;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {

    // UUIDs for UAT service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // timer declaration
    CountDownTimer TRTimer;
    CountDownTimer TLTimer;
    CountDownTimer BRTimer;
    CountDownTimer BLTimer;
    CountDownTimer PIRLTimer;
    CountDownTimer PIRCTimer;
    CountDownTimer PIRRTimer;

    // UI elements
    ScrollView scrollView;
    private TextView messages;
    Button btnON, btnOFF;

    View TRview;
    View TLview;
    View BRview;
    View BLview;
    View PIRLview;
    View PIRCview;
    View PIRRview;

    GradientDrawable TR;
    GradientDrawable TL;
    GradientDrawable PIRL;
    GradientDrawable PIRC;
    GradientDrawable PIRR;
    LayerDrawable BR;
    LayerDrawable BL;
    GradientDrawable BRcircleLeft;
    GradientDrawable BRcircleRight;
    GradientDrawable BLcircleLeft;
    GradientDrawable BLcircleRight;

    // Vibrate element
    Vibrator v;

    // BTLE state
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;

    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                writeLine("Connected!");
                // Discover services.
                if (!gatt.discoverServices()) {
                    writeLine("Failed to start discovering services!");
                }
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                writeLine("Disconnected!");
            }
            else {
                writeLine("Connection state changed.  New state: " + newState);
            }
        }

        // Called when services have been discovered on the remote device.
        // It seems to be necessary to wait for this discovery to occur before
        // manipulating any services or characteristics.
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeLine("Service discovery completed!");
            }
            else {
                writeLine("Service discovery failed with status: " + status);
            }
            // Save reference to each characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                writeLine("Couldn't set notifications for RX characteristic!");
            }
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    writeLine("Couldn't write RX client descriptor value!");
                }
            }
            else {
                writeLine("Couldn't get RX client descriptor!");
            }
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String tmp = characteristic.getStringValue(0);
            writeLine("Received: " + tmp);

            if(tmp.equals("5")){
                Intent i = new Intent(MainActivity.this, AggressionAlert.class);
                startActivity(i);
            }else if(tmp.equals("6")){
                // Alert PIR
                writeLine("You are being approached from behind: PIR Right");

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        PIRR.setColor(Color.RED);
                    }
                });

                // Set timer and when done set color bad
                PIRLTimer.start();
            }else if(tmp.equals("7")){
                // Alert PIR
                writeLine("You are being approached from behind: PIR Center");

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        PIRC.setColor(Color.RED);
                    }
                });

                // Set timer and when done set color bad
                PIRCTimer.start();
            }else if(tmp.equals("8")){
                // Alert PIR
                writeLine("You are being approached from behind: PIR Left");

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        PIRL.setColor(Color.RED);
                    }
                });

                // Set timer and when done set color bad
                PIRRTimer.start();
            }else if(tmp.equals("2")){
                writeLine("You are being approached from behind");

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        TL.setColor(Color.RED);
                    }
                });

                // Set timer and when done set color bad
                TLTimer.start();
            }else if(tmp.equals("1")){
                writeLine("You are being approached from behind");

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        TR.setColor(Color.RED);
                    }
                });

                // Set timer and when done set color bad
                TRTimer.start();
            }else if(tmp.equals("4")){
                writeLine("You are being approached from behind");

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        BLcircleRight.setColor(Color.RED);
                    }
                });

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        BLcircleLeft.setColor(Color.RED);
                    }
                });

                // Set timer and when done set color bad
                BLTimer.start();
            }else if(tmp.equals("3")){
                writeLine("You are being approached from behind");

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        BRcircleRight.setColor(Color.RED);
                    }
                });

                // Change color of dot
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        BRcircleLeft.setColor(Color.RED);
                    }
                });

                // Set timer and when done set color bad
                BRTimer.start();
            }else if(!tmp.equals("6")){
                writeLine("Did not work...");
                writeLine("Received: _" + tmp + "_");
            }
        }
    };

    // BTLE device scanning callback.
    private LeScanCallback scanCallback = new LeScanCallback() {
        // Called when a device is found.
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            writeLine("Found device: " + bluetoothDevice.getAddress());
            // Check if the device has the UART service.
            if (parseUUIDs(bytes).contains(UART_UUID)) {
                // Found a device, stop the scan.
                adapter.stopLeScan(scanCallback);
                writeLine("Found UART service!");
                // Connect to the device.
                // Control flow will now go to the callback functions when BTLE events occur.
                gatt = bluetoothDevice.connectGatt(getApplicationContext(), false, callback);
            }
        }
    };

    // OnCreate, called once to initialize the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab references to UI elements.
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        TRview = findViewById(R.id.sensorTopRight);
        TLview = findViewById(R.id.sensorTopLeft);
        BRview = findViewById(R.id.sensorBottomRight);
        BLview = findViewById(R.id.sensorBottomLeft);
        PIRLview = findViewById(R.id.sensorPIRLeft);
        PIRCview = findViewById(R.id.sensorPIRCenter);
        PIRRview = findViewById(R.id.sensorPIRRight);

        TR = (GradientDrawable)TRview.getBackground();
        TL = (GradientDrawable)TLview.getBackground();
        PIRL = (GradientDrawable)PIRLview.getBackground();
        PIRC = (GradientDrawable)PIRCview.getBackground();
        PIRR = (GradientDrawable)PIRRview.getBackground();

        BR = (LayerDrawable)BRview.getBackground();
        BRcircleLeft = (GradientDrawable) BR.findDrawableByLayerId(R.id.leftCircle);
        BRcircleRight = (GradientDrawable) BR.findDrawableByLayerId(R.id.rightCircle);

        BL = (LayerDrawable)BLview.getBackground();
        BLcircleLeft = (GradientDrawable) BL.findDrawableByLayerId(R.id.leftCircle);
        BLcircleRight = (GradientDrawable) BL.findDrawableByLayerId(R.id.rightCircle);

        messages = (TextView) findViewById(R.id.messages);
        btnON = (Button)findViewById(R.id.buttonON);
        btnOFF = (Button)findViewById(R.id.buttonOFF);
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        adapter = BluetoothAdapter.getDefaultAdapter();

        btnON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // turn NightSafe device on
                sendData(v, "1");
            }
        });

        btnOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TEST THE EMERGENCY FUNCTION
                Intent intent = new Intent(MainActivity.this, AggressionAlert.class);
                startActivity(intent);

                // turn NightSafe device off
                sendData(v, "0");

            }
        });
    }

    // OnResume, called right before UI is displayed.  Start the BTLE connection.
    @Override
    protected void onResume() {
        super.onResume();
        // Scan for all BTLE devices.
        // The first one with the UART service will be chosen--see the code in the scanCallback.
        writeLine("Scanning for devices...");
        adapter.startLeScan(scanCallback);
        //adapter.startDiscovery();
    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        super.onStop();
        if (gatt != null) {
            // For better reliability be careful to disconnect and close the connection.
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
        }
    }

    // send Data over Bluetooth
    public void sendData(View view, String message){
        if (tx == null || message == null || message.isEmpty()) {
            // Do nothing if there is no device or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            writeLine("Sent: " + message);
        }
        else {
            writeLine("Couldn't write TX characteristic!");
        }
    }

    // Write some text to the messages text view.
    // Care is taken to do this on the main UI thread so writeLine can be called
    // from any thread (like the BTLE callback).
    private void writeLine(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messages.append(text);
                scrollView.fullScroll(View.FOCUS_DOWN);
                messages.append("\n");
            }
        });
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Top Left three second timer
        TLTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // do NOTHING!!!!!
            }

            @Override
            public void onFinish() {
                // set color back to green
                TL.setColor(Color.GREEN);
            }
        };

        // Top Right three second timer
        TRTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // do NOTHING!!!!!
            }

            @Override
            public void onFinish() {
                // set color back to green
                TR.setColor(Color.GREEN);
            }
        };

        // Bottom Left three second timer
        BLTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // do NOTHING!!!!!
            }

            @Override
            public void onFinish() {
                // set color back to green
                BLcircleLeft.setColor(Color.GREEN);
                BLcircleRight.setColor(Color.GREEN);
            }
        };

        // Bottom Right three second timer
        BRTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // do NOTHING!!!!!
            }

            @Override
            public void onFinish() {
                // set color back to green
                BRcircleLeft.setColor(Color.GREEN);
                BRcircleRight.setColor(Color.GREEN);
            }
        };

        // Left PIR three second timer
        PIRLTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // do NOTHING!!!!!
            }

            @Override
            public void onFinish() {
                // set color back to green
                PIRL.setColor(Color.GREEN);
            }
        };

        // center PIR three second timer
        PIRCTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // do NOTHING!!!!!
            }

            @Override
            public void onFinish() {
                // set color back to green
                PIRC.setColor(Color.GREEN);
            }
        };

        // Left PIR three second timer
        PIRRTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // do NOTHING!!!!!
            }

            @Override
            public void onFinish() {
                // set color back to green
                PIRR.setColor(Color.GREEN);
            }
        };
    }
}