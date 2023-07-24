package com.example.stockpasswordrecovery;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    private static final String SERVER_URL = "https://mar.masetawosha.com/requestPasswordReset";
    private static final int SMS_PERMISSION_REQUEST_CODE = 1;
    private TextView responseTextView;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            new NetworkRequestTask().execute(SERVER_URL);
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        responseTextView = findViewById(R.id.response_textview);
        handler.post(runnable);
        // Request permission to send SMS if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        }
    }

    private class NetworkRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                Scanner scanner = new Scanner(inputStream);
                scanner.useDelimiter("\\A");

                if (scanner.hasNext()) {
                    response = scanner.next();
                }
                scanner.close();
            } catch (IOException e) {
                Log.e("MainActivity", "Error making network request", e);
            }

            try {
                JSONObject jsonObject = new JSONObject(response);

                String phoneNumber = jsonObject.getString("phoneNumber");
                String pinCode = jsonObject.getString("pinCode");
                if (!phoneNumber.equals("notFound")) {
                    sendSMS(MainActivity.this, phoneNumber, "Your password reset code is: " + pinCode);
                }

            } catch (JSONException e) {
                Log.e("MainActivity", "Error parsing JSON response", e);
                response = "";
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null && !response.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.has("message")) { // Check if the "message" key exists
                        responseTextView.setText(jsonObject.getString("message"));
                    } else {
                        responseTextView.setText("No message received from server");
                    }
                } catch (JSONException e) {
                    Log.e("MainActivity", "Error parsing JSON response", e);
                    responseTextView.setText("Error parsing response from server");
                }
            } else {
                responseTextView.setText("No response received from server");
            }
        }
    }

    private void sendSMS(Context context, String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.i("MainActivity", "SMS sent to " + phoneNumber);
        } else {
            Log.e("MainActivity", "Cannot send SMS: permission not granted");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("MainActivity", "SMS permission granted");
        } else {
            Log.e("MainActivity", "SMS permission not granted");
        }
    }
}