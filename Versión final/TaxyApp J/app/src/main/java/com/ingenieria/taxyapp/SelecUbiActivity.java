package com.ingenieria.taxyapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelecUbiActivity extends FragmentActivity implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private SupportMapFragment mapFragment;

    private Button mLogout;

    NavigationView navigationView;


    //Seleccionar destino/ubicacion variables
    PlacesClient placesClient;
    EditText mselec_ubi, mselec_destino;

    //Guardar datos ubicacion partida/destino variables
    String destino_nombre, destino_dir;
    String partida_nombre, partida_dir;
    LatLng partida_latlang, destino_latlang;

    //Buscar Conductor Variables
    private Button searchDriver_button;
    private DatabaseReference mDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selec_ubi);

        // Obtiene el mapa y notifica cuando el mapa esta listo para ser usado
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Despliega el menu
        final DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        findViewById(R.id.imageMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        //Seleccionar Ubicacion
        mselec_destino = findViewById(R.id.selec_destino);
        Places.initialize(this,getString(R.string.google_maps_key));
        mselec_destino.setFocusable(false);
        mselec_destino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(SelecUbiActivity.this);
                startActivityForResult(intent,200);
            }
        });

        //Seleccionar destino
        mselec_ubi = findViewById(R.id.selec_ubi);
        Places.initialize(this,getString(R.string.google_maps_key));
        mselec_ubi.setFocusable(false);
        mselec_ubi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(SelecUbiActivity.this);
                startActivityForResult(intent,100);
            }
        });

        //Buscar Conductor
        mDataBase = FirebaseDatabase.getInstance().getReference();

        searchDriver_button = (Button) findViewById(R.id.searchDriver_button);
        searchDriver_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String destino = mselec_destino.getText().toString();
                String ubicacion = mselec_ubi.getText().toString();
                if (!destino.isEmpty()){
                    if (ubicacion.isEmpty()){
                        partida_nombre="Ubicacion Actual";
                    }

                    Map<String, Object> origen = new HashMap<>();
                    origen.put("nombre", partida_nombre);
                    origen.put("direccion", partida_dir);
                    origen.put("ubicacion", partida_latlang);

                    Map<String, Object> dest = new HashMap<>();
                    dest.put("nombre", destino_nombre);
                    dest.put("direccion", destino_dir);
                    dest.put("ubicacion", destino_latlang);

                    //Determinar distancia entre los dos puntos
                    Location locpartida = new Location("");  //Se obtiene ubicacion de partida (latitud y longitud)
                    locpartida.setLatitude(partida_latlang.latitude);
                    locpartida.setLongitude(partida_latlang.longitude);

                    Location locdestino = new Location(""); //Se obtiene ubicacion de destino (latitud y longitud)
                    locdestino.setLatitude(destino_latlang.latitude);
                    locdestino.setLongitude(destino_latlang.longitude);

                    float distance = locpartida.distanceTo(locdestino); //se realiza calculo de distancia (en metros)

                    String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    mDataBase.child("usuarioRequest").child(user_id).child("Distancia").setValue(distance);
                    mDataBase.child("usuarioRequest").child(user_id).child("Origen").setValue(origen);
                    mDataBase.child("usuarioRequest").child(user_id).child("Destino").setValue(dest).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task2) {
                            if(task2.isSuccessful()){
                                //se abre pestaña con los datos del conductor, tiempo de llegada
                                Intent intent = new Intent(SelecUbiActivity.this, ConfirmTravelActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }else{
                                //en caso de que el correo ya se haya registrado  se muestra un mensaje en pantalla
                                Toast.makeText(SelecUbiActivity.this, "No se pudo realizar su peticion", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Toast.makeText(SelecUbiActivity.this, "Debe seleccionar una ubicación de destino", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    //Actividad Ubicacion & Destino
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK){
            Place place = Autocomplete.getPlaceFromIntent(data);
            mselec_ubi.setText(place.getName());

            partida_latlang = place.getLatLng();
            partida_nombre = place.getName();
            partida_dir = place.getAddress();

        }else if (requestCode == 200 && resultCode == RESULT_OK){
            Place place = Autocomplete.getPlaceFromIntent(data);
            mselec_destino.setText(place.getName());

            destino_latlang = place.getLatLng();
            destino_nombre = place.getName();
            destino_dir = place.getAddress();
        }else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(getApplicationContext(), status.getStatusMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //muestra la localizacion del usuario y define el intervalo en el que se actualiza constantemente.
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SelecUbiActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //actualiza constantemente la ubicacion del usuario (en tiempo real)
        if(getApplicationContext()!=null){
            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

            partida_latlang = latLng;
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ubicacionUsuario");

            GeoFire geoFire = new GeoFire(ref);
            geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
        }
    }

    final int LOCATION_REQUEST_CODE = 1;
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {

                }
                break;
            }
        }
    }

    //Menu desplegable
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_log:
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ubicacionUsuario");
                ref.child(userId).setValue(null);
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(SelecUbiActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return true;
    }
}
