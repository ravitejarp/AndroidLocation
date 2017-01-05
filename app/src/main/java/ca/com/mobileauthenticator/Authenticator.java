package ca.com.mobileauthenticator;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

public class Authenticator extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    String username;
    String password;
    double longitude;
    double latitude;
    TrackGPS gps;
    SessionManager session;
    Button btnLogout;
    boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);
        session = new SessionManager(getApplicationContext());
        session.checkLogin();

        HashMap<String, String> user = session.getUserDetails();

        // name
        String name = user.get(SessionManager.KEY_NAME);

        // email
        String email = user.get(SessionManager.KEY_PASSWORD);

        btnLogout = (Button) findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Clear the session data
                // This will clear all session data and
                // redirect user to LoginActivity
                session.logoutUser();
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.remove("username");
                editor.apply();
            }
        });
        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        password = intent.getStringExtra("password");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        getLocationInt(null, null, null);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.authenticator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.location) {
            Toast.makeText(Authenticator.this, "Camera check", Toast.LENGTH_SHORT);
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void navigateToLocation(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("Lang", longitude);
        intent.putExtra("Lat", latitude);
        startActivity(intent);
    }

    public void getLocationInt(Location location, TextView viewById, TextView byId) {
        if (this.gps == null) {
            this.gps = new TrackGPS(Authenticator.this, viewById == null ? (TextView) findViewById(R.id.id_location) : viewById, byId == null ? (TextView) findViewById(R.id.id_nearby) : byId);
        }
        if (gps.getLoc() == null && location == null) {
            if (gps.canGetLocation()) {
                longitude = gps.getLongitude();
                latitude = gps.getLatitude();

//            Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();
                if (viewById == null)
                    viewById = (TextView) findViewById(R.id.id_location);
                viewById.setText("Longitude:" + Double.toString(longitude) + " Latitude:" + Double.toString(latitude));
            } else {
                gps.showSettingsAlert();
            }
        } else {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            } else if (gps.getLoc() != null) {
                longitude = gps.getLongitude();
                latitude = gps.getLatitude();
            } else {
                longitude = 0.0;
                latitude = 0.0;
            }
        }
        if (byId == null)
            byId = (TextView) findViewById(R.id.id_nearby);
//        if ((longitude >= 78.3369627 && longitude <= 78.3369827) && (latitude >= 17.4253097 && latitude <= 17.4253297)) {
        if ((longitude >= 78.5410528 && longitude <= 78.5410728) && (latitude >= 17.3716593 && latitude <= 17.3716793)) {
            new Authenticator.HttpRequestTask().execute();
            byId.setText(String.format("%s", "You are near by"));
        } else {
            byId.setText(String.format("%s", "You are not near by"));
        }

    }

    private class HttpRequestTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                final String url = "http://authserver-rakeshbalguri.rhcloud.com/authserver/auth/" + username + "/" + password;
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
                String greeting = restTemplate.getForObject(url, String.class);
                return greeting;
            } catch (Exception e) {
                Log.e("Authentication", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String greeting) {
            TextView greetingIdText = (TextView) findViewById(R.id.id_value);
            greetingIdText.setText(greeting);
        }
    }


    private class TrackGPS extends Service implements LocationListener {

        private final Context mContext;
        boolean checkGPS = false;
        boolean checkNetwork = false;
        boolean canGetLocation = false;
        Location loc;
        double latitude;
        double longitude;

        private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
        private static final long MIN_TIME_BW_UPDATES = 1000;
        protected LocationManager locationManager;
        TextView viewById;
        TextView byId;

        public TrackGPS(Context mContext, TextView viewById, TextView byId) {
            this.mContext = mContext;
            this.viewById = viewById;
            this.byId = byId;
            getLocation();
        }

        private Location getLocation() {

            try {
                locationManager = (LocationManager) mContext
                        .getSystemService(LOCATION_SERVICE);

                // getting GPS status
                checkGPS = locationManager
                        .isProviderEnabled(LocationManager.GPS_PROVIDER);

                // getting network status
                checkNetwork = locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!checkGPS && !checkNetwork) {
                    Toast.makeText(mContext, "No Service Provider Available", Toast.LENGTH_SHORT).show();
                } else {
                    this.canGetLocation = true;
                    // First get location from Network Provider
                    if (checkNetwork) {
                        Toast.makeText(mContext, "Network", Toast.LENGTH_SHORT).show();

                        try {
                            locationManager.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//                            Log.d("Network", "Network");
                            if (locationManager != null) {
                                loc = locationManager
                                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                            }

                            if (loc != null) {
                                latitude = loc.getLatitude();
                                longitude = loc.getLongitude();
                            }
                        } catch (SecurityException e) {

                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (checkGPS) {
                    Toast.makeText(mContext, "GPS", Toast.LENGTH_SHORT).show();
                    if (loc == null) {
                        try {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//                            Log.d("GPS Enabled", "GPS Enabled");
                            if (locationManager != null) {
                                loc = locationManager
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (loc != null) {
                                    latitude = loc.getLatitude();
                                    longitude = loc.getLongitude();
                                }
                            }
                        } catch (SecurityException e) {

                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return loc;
        }

        public double getLongitude() {
            if (loc != null) {
                longitude = loc.getLongitude();
            }
            return longitude;
        }

        public double getLatitude() {
            if (loc != null) {
                latitude = loc.getLatitude();
            }
            return latitude;
        }

        public boolean canGetLocation() {
            return this.canGetLocation;
        }

        public void showSettingsAlert() {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);


            alertDialog.setTitle("GPS Not Enabled");

            alertDialog.setMessage("Do you wants to turn On GPS");


            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    mContext.startActivity(intent);
                }
            });


            alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });


            alertDialog.show();
        }


        public void stopUsingGPS() {
            if (locationManager != null) {
                locationManager.removeUpdates((LocationListener) Authenticator.this);
            }
        }

        public Location getLoc() {
            return loc;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onLocationChanged(Location location) {
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
//            new Authenticator().getLocationInt(location, viewById, byId);
            byId.setText("Lat:" + latitude + " Long:" + longitude);
//            if ((longitude >= 78.5410528 && longitude <= 78.5410728) && (latitude >= 17.3716593 && latitude <= 17.3716793)) {
            if ((longitude >= 78.3374073 && longitude <= 78.3374273) && (latitude >= 17.4251440 && latitude <= 17.4251640)) {
                if (!flag) {
                    new Authenticator.HttpRequestTask().execute();
                }
                byId.setText(String.format("%s", "You are near by"));
                flag = true;
                viewById.setText(String.format("Lat:%s\nLon:%s", longitude, latitude));
            } else {
                byId.setText(String.format("%s", "You are not near by"));
                viewById.setText(String.format("Lat:%s\nLon:%s", longitude, latitude));
                flag = false;
            }

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

        @Override
        public void onDestroy() {
            stopUsingGPS();
        }
    }


}
