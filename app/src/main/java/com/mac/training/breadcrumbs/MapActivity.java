package com.mac.training.breadcrumbs;

import android.*;
import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.List;

/**
 * Created by jonathanhavstad on 9/12/16.
 */

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapActivity";

    private static final int GREEN_PATH_WIDTH = 8;
    private static final int YELLOW_PATH_WIDTH = 10;
    private static final int RED_PATH_WIDTH = 12;

    private static final int PERMISSION_REQUEST_POSITION = 1000;

    private GoogleMap map;
    private Marker marker;
    private LatLongDataDao latLongDataDao;
    private ServiceConnection serviceConnection;
    private long lastTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initDatabase();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fabClearPath = (FloatingActionButton)
                findViewById(R.id.fab_clearpath);
        fabClearPath.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Clear the current path", Snackbar.LENGTH_LONG)
                        .setAction("Clear", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                unbindService(serviceConnection);
                                dropTable();
                                createTable();
                                clearMap();
                                initService();
                                initDatabase();
                            }
                        })
                .show();
            }
        });

        Toast.makeText(this, "Starting BreadCrumbs", Toast.LENGTH_SHORT).show();
    }

    private void initDatabase() {
        SQLiteDatabase db = new DaoMaster.DevOpenHelper(
                this,
                getCacheDir() +
                        File.pathSeparator +
                        getString(R.string.breadcrumbs_dbname)).getReadableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        latLongDataDao = daoSession.getLatLongDataDao();
    }

    private void dropTable() {
        LatLongDataDao.dropTable(latLongDataDao.getDatabase(), true);
    }

    private void createTable() {
        LatLongDataDao.createTable(latLongDataDao.getDatabase(), true);
    }

    private void clearMap() {
        map.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initService();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        latLongDataDao.getDatabase().close();
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        drawBreadCrumbs(lastTime);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putLong(getString(R.string.last_time_bundle_key), lastTime);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        lastTime = savedInstanceState.getLong(getString(R.string.last_time_bundle_key));
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    private void drawBreadCrumbs(Long lastTime) {
        new DrawBreadCrumbsTask().execute(lastTime);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_POSITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initServiceWithPermissions();
            }
        }
    }

    private class DrawBreadCrumbsTask extends AsyncTask<Long, PolylineOptions, Void> {
        private List<LatLongData> latLongDataList;
        @Override
        protected Void doInBackground(Long... params) {
            latLongDataList = queryDatabase(params[0]);
            PolylineOptions polylineOptions = new PolylineOptions();
            LatLng gPrevPt = null;
            for (LatLongData latLongData : latLongDataList) {
                LatLng gDataPt = new LatLng(latLongData.getLat(), latLongData.getLon());
                polylineOptions.add(gDataPt);
                if (gPrevPt != null) {
                    if (latLongData.getSpeed() <= 1.0f) {
                        publishProgress(createRedPath(polylineOptions));
                    } else if (latLongData.getSpeed() <= 10.0f) {
                        publishProgress(createYellowPath(polylineOptions));
                    } else {
                        publishProgress(createGreenPath(polylineOptions));
                    }
                }
                gPrevPt = gDataPt;
                polylineOptions = new PolylineOptions();
                polylineOptions.add(gPrevPt);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(PolylineOptions... values) {
            map.addPolyline(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (latLongDataList.size() > 0) {
                LatLongData lastPoint = latLongDataList.get(latLongDataList.size() - 1);
                LatLng gMapPoint = new LatLng(lastPoint.getLat(), lastPoint.getLon());

                CameraPosition lastPos =
                        new CameraPosition.Builder().target(gMapPoint).zoom(20f).build();

                if (marker == null) {
                    marker = map.addMarker(new MarkerOptions().position(gMapPoint));
                } else {
                    marker.setPosition(gMapPoint);
                }

                map.animateCamera(CameraUpdateFactory.newCameraPosition(lastPos));
                lastTime = lastPoint.getTime();
            }
        }

        private PolylineOptions createGreenPath(PolylineOptions polylineOptions) {
            return polylineOptions.width(GREEN_PATH_WIDTH).color(Color.GREEN);
        }

        private PolylineOptions createRedPath(PolylineOptions polylineOptions) {
            return polylineOptions.width(RED_PATH_WIDTH).color(Color.RED);
        }

        private PolylineOptions createYellowPath(PolylineOptions polylineOptions) {
            return polylineOptions.width(YELLOW_PATH_WIDTH).color(Color.YELLOW);
        }
    }

    private void initService() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_POSITION);
            } else {
                initServiceWithPermissions();
            }
        } else {
            initServiceWithPermissions();
        }
    }

    private void initServiceWithPermissions() {
        Intent breadcrumbsIntent = new Intent(this, BreadCrumbService.class);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                BreadCrumbService.Binder breadCrumbBinder =
                        (BreadCrumbService.Binder) service;
                breadCrumbBinder.registerUpdate(
                        new BreadCrumbService.Binder.LocationUpdater() {

                            @Override
                            public void send(Location location) {
                                drawBreadCrumbs(lastTime);
                            }
                        });

                initService(breadCrumbBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        bindService(
                breadcrumbsIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void initService(BreadCrumbService.Binder breadCrumbBinder) {
        LocationManager locationManager =
                (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        breadCrumbBinder.getService().init(provider);
    }

    private List<LatLongData> queryDatabase(long time) {

        return latLongDataDao
                .queryBuilder()
                .where(LatLongDataDao.Properties.Time.ge(time))
                .orderAsc(LatLongDataDao.Properties.Time)
                .list();
    }
}
