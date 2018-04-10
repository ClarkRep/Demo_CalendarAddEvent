package com.example.clark.addcalendarevent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int eventId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_insert).setOnClickListener(this);
        findViewById(R.id.btn_update).setOnClickListener(this);
        findViewById(R.id.btn_delete).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_insert:
                if (hasPermission()) {
                    eventId = CalendarEventUtils.addCalendarEvent(this, -1, "haha标题", "haha描述", System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(11), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(21));
                } else {
                    requestPermission();
                }
                break;
            case R.id.btn_update:
                CalendarEventUtils.addCalendarEvent(this, -1, "haha标题", "haha描述更新了", System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(31), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(61));
                break;
            case R.id.btn_delete:
                CalendarEventUtils.deleteEventRemind(this, eventId);
                break;
            default:
                break;
        }
    }

    private boolean hasPermission() {
        int readCalendarPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);
        int writeCalendarPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR);
        if (readCalendarPermission == PackageManager.PERMISSION_GRANTED && writeCalendarPermission == writeCalendarPermission) {
            return true;
        }
        return false;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, 111);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 111) {
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
            }
        }
    }
}
