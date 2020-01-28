package it.uniupo.noteyournote;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import it.uniupo.noteyournote.database.DatabaseManager;
import it.uniupo.noteyournote.model.Note;
import it.uniupo.noteyournote.util.LocationTracker;
import it.uniupo.noteyournote.util.Util;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private LocationTracker mLocationTracker;
    private static final float DEFAULT_ZOOM = 15.0f;
    private static final int REQUEST_LOCATION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mMap != null) {
            if (Util.checkPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) && Util.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                mMap.setMyLocationEnabled(true);

                mLocationTracker = new LocationTracker(this);
                if (mLocationTracker.canGetLocaion()) {
                    if (!String.valueOf(mLocationTracker.getLatitude()).equals("0.0") && !String.valueOf(mLocationTracker.getLongitude()).equals("0.0")) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLocationTracker.getLatitude(), mLocationTracker.getLongitude()), DEFAULT_ZOOM));
                    }
                } else {
                    mLocationTracker.showSettingsAlertDialog();
                }

                retrieveNotes();

                mMap.setOnMarkerClickListener(this);
                mMap.setOnInfoWindowClickListener(this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), DEFAULT_ZOOM));
        marker.showInfoWindow();
        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Intent intent = new Intent(this, NoteActivity.class);
        Note note = (Note) marker.getTag();
        intent.putExtra("id", note.getId());
        startActivity(intent);
    }

    private void retrieveNotes() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            DatabaseManager databaseManager = new DatabaseManager(this);

            for (Note note : databaseManager.fetch()) {
                addItemsToMaps(note);
            }
            databaseManager.close();
        } else {
            if (Util.isNetworkAvailable(this)) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore
                        .getInstance()
                        .collection(uid)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot q : task.getResult()) {
                                        addItemsToMaps(q.toObject(Note.class));
                                    }
                                }
                            }
                        });
            }
        }
    }

    private void addItemsToMaps(Note note) {
        if (!String.valueOf(note.getLatitude()).equals("0.0") && !String.valueOf(note.getLongitude()).equals("0.0")) {
            mMap.addMarker(new MarkerOptions()
                    .title(note.getTitle())
                    .snippet(note.getDescription())
                    .position(new LatLng(note.getLatitude(), note.getLongitude())))
                    .setTag(note);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLocationTracker != null) {
            mLocationTracker.stopListener();
        }
    }
}
