package com.example.mapgpsdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker currentMarker;      // Un seul marker qui bouge

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 1) Vérifier permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            // Demander la permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Vérifier si le GPS (ou réseau) est activé
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        // Utiliser NETWORK_PROVIDER (rapide, moins précis) ou GPS_PROVIDER (précis)
        // Ici on utilise NETWORK_PROVIDER pour les tests en intérieur
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000,   // 1 seconde
                10,     // 10 mètres (réduit pour tests)
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        updateMapWithLocation(location);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        if (provider.equals(LocationManager.NETWORK_PROVIDER) ||
                                provider.equals(LocationManager.GPS_PROVIDER)) {
                            buildAlertMessageNoGps();
                        }
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) { }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) { }
                }
        );
    }

    private void updateMapWithLocation(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        LatLng pos = new LatLng(lat, lng);

        // Afficher un Toast pour le debug
        Toast.makeText(this, "Lat: " + lat + ", Lng: " + lng, Toast.LENGTH_SHORT).show();

        // Un seul marker : on le crée ou on le déplace
        if (currentMarker == null) {
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Position actuelle")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            currentMarker.setPosition(pos);
        }

        // Centrer la caméra et zoomer
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
    }

    private void buildAlertMessageNoGps() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Votre GPS est désactivé. Voulez-vous l'activer ?")
                .setCancelable(false)
                .setPositiveButton("Oui", (dialog, id) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Non", (dialog, id) -> dialog.cancel());
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accordée", Toast.LENGTH_SHORT).show();
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_LONG).show();
            }
        }
    }
}