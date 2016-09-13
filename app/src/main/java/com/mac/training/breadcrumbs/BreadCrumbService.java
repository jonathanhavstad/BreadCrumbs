package com.mac.training.breadcrumbs;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

/**
 * Created by jonathanhavstad on 9/12/16.
 */

public class BreadCrumbService extends Service implements LocationListener {
    private static final int MAP_NOTIFICATION_ID = 1000;
    private static final String TAG = "BreadCrumbService";

    private LocationManager locationManager;
    private String provider;

    private LatLongDataDao latLongDataDao;
    private SQLiteDatabase db;

    private Binder binder;

    private boolean started;

    public static class Binder extends android.os.Binder {
        private LocationUpdater updater;
        private BreadCrumbService service;

        public interface LocationUpdater {
            void send(Location location);
        }

        public Binder(BreadCrumbService service) {
            this.service = service;
        }

        public BreadCrumbService getService() {
            return service;
        }

        public void registerUpdate(LocationUpdater updater) {
            this.updater = updater;
        }

        public void send(Location location) {
            if (updater != null) {
                updater.send(location);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        binder = new Binder(this);
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init(intent.getStringExtra(getString(R.string.gps_provider_bundle_key)));

        started = true;

        Log.d(TAG, "Starting BreadCrumbService");

        return Service.START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLongData latLongData = new LatLongData();
        latLongData.setLat(location.getLatitude());
        latLongData.setLon(location.getLongitude());
        latLongData.setTime(location.getTime());
        latLongData.setSpeed(location.getSpeed());

        latLongDataDao.insert(latLongData);

        if (binder != null) {
            binder.send(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void init(String provider) {
        if (!started) {
            this.provider = provider;

            locationManager = (LocationManager)
                    getSystemService(Service.LOCATION_SERVICE);

            startGps();

            Intent activityIntent = new Intent(this, MapActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Loading GPS Data")
                    .setSmallIcon(android.R.drawable.ic_menu_mapmode)
                    .addAction(android.R.drawable.ic_menu_mapmode,
                            getString(R.string.app_name),
                            pendingIntent)
                    .build();

            startForeground(MAP_NOTIFICATION_ID, notification);

            db = new DaoMaster.DevOpenHelper(
                    this,
                    getCacheDir() + File.pathSeparator + getString(R.string.breadcrumbs_dbname))
                    .getWritableDatabase();
            DaoMaster daoMaster = new DaoMaster(db);
            DaoSession daoSession = daoMaster.newSession();
            latLongDataDao = daoSession.getLatLongDataDao();
        }
    }

    public void startGps() throws SecurityException {
        locationManager.requestLocationUpdates(provider, 10000L, 0f, this);
    }

    public void stopGps() throws SecurityException {
        locationManager.removeUpdates(this);
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopGps();
        db.close();
        started = false;
    }
}
