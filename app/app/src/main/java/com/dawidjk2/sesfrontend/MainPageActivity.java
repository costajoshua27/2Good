package com.dawidjk2.sesfrontend;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dawidjk2.sesfrontend.Adapters.CardAdapter;
import com.dawidjk2.sesfrontend.Models.Card;
import com.dawidjk2.sesfrontend.Models.Geofence;
import com.dawidjk2.sesfrontend.Models.Transaction;
import com.dawidjk2.sesfrontend.Services.GeofenceBroadcastReceiver;
import com.dawidjk2.sesfrontend.Services.GeofenceService;
import com.dawidjk2.sesfrontend.Singletons.ApiSingleton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainPageActivity extends AppCompatActivity implements CardAdapter.OnItemListener, View.OnClickListener {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<Card> myDataset;

    private TextView hello;
    private TextView balance;

    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private GeofenceService geofenceService;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;
    public static String lastKnownLocation = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_drawer_layout);

        createNotificationChannel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            lastKnownLocation = location.getLatitude() + "," + location.getLongitude();
                        }
                    }
                });

        geofencingClient = LocationServices.getGeofencingClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        geofenceService = new GeofenceService(geofencingClient);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, getString(R.string.backend_url) + "v0/geofence/getPlaces", null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject object) {
                        try {
                            JSONArray locationArray = object.getJSONArray("locations");
                            ArrayList<Geofence> geofenceArrayList = new ArrayList<>();
                            Log.d("locationArray length", String.valueOf(locationArray.length()));

                            for(int i = 0; i < locationArray.length(); ++i) {
                                JSONObject location = locationArray.getJSONObject(i);
                                Geofence geofence = new Geofence();
                                geofence.latitude = location.getDouble("lat");
                                geofence.longitude = location.getDouble("lng");
                                geofence.key = location.getString("key");
                                geofence.exp = 999999999999999999L;

                                geofenceArrayList.add(geofence);
                            }

                            geofenceService.addFences(geofenceArrayList);
                            intializeGeofence();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        /* TODO: Handle error */
                        Log.e("Volley Places", error.toString());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  params = new HashMap<>();
                @SuppressLint("MissingPermission")
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location != null) {
                    lastKnownLocation = location.getLatitude() + "," + location.getLongitude();
                }
                Log.d("Local", lastKnownLocation);
                params.put("location", lastKnownLocation);
                params.put("type", "cafe");

                return params;
            }
        };

        // Access the RequestQueue through your singleton class.
        ApiSingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

        // Dummy data
        String name = "Sami";
        String accountBalance = "100.00";

        hello = findViewById(R.id.mainPageHello);
        balance = findViewById(R.id.mainPageBalance);
        findViewById(R.id.nav_bar_button).setOnClickListener(this);

        hello.setText("Welcome back, " + name + "!");
        balance.setText("$" + accountBalance);


        // More dummy data
        myDataset = new ArrayList<>();
        myDataset.add(new Card("Card 1", "123456789", "100.00"));
        myDataset.add(new Card("Card 2", "24567890", "200.00"));
        myDataset.add(new Card("Card 3", "24567890", "200.00"));
        myDataset.add(new Card("Card 4", "24567890", "200.00"));
        myDataset.add(new Card("Card 5", "24567890", "200.00"));

        // Handle the recycle view for all cards
        recyclerView = findViewById(R.id.mainPageCards);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new CardAdapter(myDataset, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {

        // Get the chosen card
        Card card = myDataset.get(position);

        // These are dummy transactions
        ArrayList<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(card, "Chipotle", 10.00));
        transactions.add(new Transaction(card, "Macy's", 1000.00));

        // This is where you actually switch activites
        Intent intent = new Intent(MainPageActivity.this, ExpandedCardActivity.class);
        intent.putExtra("card", card);
        intent.putExtra("transactions", transactions);
        startActivity(intent);
    }

    public void onClick(View v) {
        DrawerLayout navDrawer = findViewById(R.id.drawer_layout);
        if (!navDrawer.isDrawerOpen(GravityCompat.START)) {
            navDrawer.openDrawer(GravityCompat.START);
        } else {
            navDrawer.closeDrawer(GravityCompat.END);
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private void intializeGeofence() {
        geofencingClient.addGeofences(geofenceService.getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added
                        // ...
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences
                        // ...
                    }
                });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getString(R.string.app_name), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
