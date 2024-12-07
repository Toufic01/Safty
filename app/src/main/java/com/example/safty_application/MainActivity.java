package com.example.safty_application;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity {
    private SensorManager mSensorManager;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private String phoneNo1 = "";  // First phone number
    private String phoneNo2 = "";  // Second phone number
    private boolean isShakeDetected = false;
    private FusedLocationProviderClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the accelerometer
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        mAccel = 10f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        // Initialize FusedLocationProviderClient
        client = LocationServices.getFusedLocationProviderClient(this);

        // Request permissions at runtime
        requestPermissions();
    }

    // Accelerometer sensor listener
    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (mAccel > 12 && !isShakeDetected) {
                Toast.makeText(getApplicationContext(), "Shake event detected", Toast.LENGTH_SHORT).show();
                sendSMSMessage();
                isShakeDetected = true;

                // Reset the flag after 2 seconds
                new Handler().postDelayed(() -> isShakeDetected = false, 2000);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onResume() {
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    private void sendSMSMessage() {
        // Check if phone numbers are empty or invalid
        if (phoneNo1.isEmpty() || phoneNo2.isEmpty()) {
            Toast.makeText(this, "Please set valid phone numbers first.", Toast.LENGTH_SHORT).show();
            return;  // Exit if phone numbers are not set
        }

        // Validate phone numbers
        if (!isValidPhoneNumber(phoneNo1) || !isValidPhoneNumber(phoneNo2)) {
            Toast.makeText(this, "Invalid phone number format.", Toast.LENGTH_SHORT).show();
            return;  // Exit if phone numbers are invalid
        }

        // Proceed to send SMS after validation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            client.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    String message = "Shake detected! Location: http://maps.google.com/?q="
                            + location.getLatitude() + "," + location.getLongitude();
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo1, null, message, null, null);
                    smsManager.sendTextMessage(phoneNo2, null, message, null, null);
                    Toast.makeText(MainActivity.this, "SMS sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Unable to fetch location.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Location permission is not granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhoneNumberDialog() {
        // Create the dialog box for entering phone numbers
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Phone Numbers");

        // Set up the input fields
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText editTextPhone1 = new EditText(this);
        editTextPhone1.setHint("Phone Number 1");
        layout.addView(editTextPhone1);

        EditText editTextPhone2 = new EditText(this);
        editTextPhone2.setHint("Phone Number 2");
        layout.addView(editTextPhone2);

        builder.setView(layout);

        // Set the positive button (Save)
        builder.setPositiveButton("Save", (dialog, which) -> {
            // Save the phone numbers entered by the user
            phoneNo1 = editTextPhone1.getText().toString().trim();
            phoneNo2 = editTextPhone2.getText().toString().trim();

            // Check if phone numbers are valid before proceeding
            if (isValidPhoneNumber(phoneNo1) && isValidPhoneNumber(phoneNo2)) {
                Toast.makeText(MainActivity.this, "Phone numbers saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Invalid phone numbers. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set the negative button (Cancel)
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Add a delete button to clear phone numbers
        builder.setNeutralButton("Delete Numbers", (dialog, which) -> {
            phoneNo1 = "";
            phoneNo2 = "";
            Toast.makeText(MainActivity.this, "Phone numbers deleted!", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Basic validation: Ensure the phone number contains only digits and may have a country code (+)
        return phoneNumber != null && phoneNumber.matches("^[+]?[0-9]{10,13}$");
    }

    private void requestPermissions() {
        // Request SMS and location permissions
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_SEND_SMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSMSMessage();
            } else {
                Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSMSMessage();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // For showing the menu with the option to set phone numbers
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);  // Inflate your menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_set_numbers) {
            showPhoneNumberDialog(); // Show the dialog to enter phone numbers
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
