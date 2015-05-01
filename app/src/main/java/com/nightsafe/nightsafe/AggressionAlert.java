package com.nightsafe.nightsafe;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class AggressionAlert extends Activity {

    public TextView remaining;
    Button test, disregard;
    SmsManager smsManager;
    LocationManager locationManager;
    CountDownTimer currentTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aggression_alert);

        test = (Button)findViewById(R.id.test);
        disregard = (Button)findViewById(R.id.disregard);
        smsManager = SmsManager.getDefault();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        remaining = (TextView)findViewById(R.id.timeLeft);

        disregard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // cancel timer and return to parent activity
                currentTimer.cancel();
                finish();
            }
        });

        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get location
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

                // send location
                String message = "I need HELP!!!\nLocation: " + lastKnownLocation.toString();
                String address = getApplicationContext().getResources().getString(R.string.test_number);
                smsManager.sendTextMessage(address, null, message, null, null);
                Toast.makeText(getApplicationContext(), "The authorities have been notified.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remaining.setText("" + (int) Math.ceil(millisUntilFinished / 1000.0));
            }

            @Override
            public void onFinish() {
                // get location
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

                // send location
                // TODO: Fairly sure this is not going to work because a view is needed
                String message = "I need HELP!!!\nLocation: " + lastKnownLocation.toString();
                String address = getApplicationContext().getResources().getString(R.string.test_number);
                smsManager.sendTextMessage(address, null, message, null, null);
                Toast.makeText(getApplicationContext(), "The authorities have been notified.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        };

        currentTimer.start();
    }
}
