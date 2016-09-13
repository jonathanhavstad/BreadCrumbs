package com.mac.training.breadcrumbs;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent mapServiceIntent = new Intent(this, BreadCrumbService.class);
        LocationManager locationManager =
                (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        mapServiceIntent.putExtra(getString(R.string.gps_provider_bundle_key), provider);
        startService(mapServiceIntent);
        finish();
    }
}
