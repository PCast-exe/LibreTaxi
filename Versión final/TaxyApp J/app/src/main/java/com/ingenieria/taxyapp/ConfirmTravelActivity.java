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
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.server.converter.StringToIntConverter;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ConfirmTravelActivity extends FragmentActivity implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private SupportMapFragment mapFragment;
    EditText editText;
    private Button mLogout;
    NavigationView navigationView;

    //Cancelar/Aceptar Viaje Variables
    private Button confirm_button, cancel_button;

    //Calcular costo del viaje
    TextView mcostoText, mdistancia_Text;
    private float distancia_val, costo, tarifa_base;
    DecimalFormat currency = new DecimalFormat("###,###.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_travel);

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

        //Muestra y Calcula costo y distancia del viaje
        mcostoText = (TextView) findViewById(R.id.costoText);
        mdistancia_Text = (TextView) findViewById(R.id.distancia_Text);

        //Muestra distancia del viaje
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child("usuarioRequest").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String distancia = dataSnapshot.child("Distancia").getValue().toString();
                    distancia_val = Float.valueOf(distancia);
                    distancia_val = distancia_val/1000;
                    mdistancia_Text.setText(currency.format(distancia_val) + " km");

                    //Se calcula costo determinando la distancia
                    tarifa_base = (float) 6.8;
                    costo = distancia_val * 4;
                    costo = costo + tarifa_base;
                    mcostoText.setText("$" + currency.format(costo) + " MXN");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Aceptar Viaje
        confirm_button = (Button) findViewById(R.id.confirm_button);
        confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Guarda datos de pago
                Calendar calendar = Calendar.getInstance();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("hh:mm:ss");
                String date = simpleDateFormat.format(calendar.getTime());
                String time = simpleTimeFormat.format(calendar.getTime());

                Map<String, Object> pago = new HashMap<>();
                pago.put("Hora_pago", time);
                pago.put("Fechapago", date);
                pago.put("tarifa_pago", 4);
                pago.put("tarifa_total", costo);
                pago.put("tipoPago", "efectivo");

                String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference mDataBase = FirebaseDatabase.getInstance().getReference();
                mDataBase.child("pago").child(user_id).setValue(pago).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task2) {
                        if(task2.isSuccessful()){
                            //se abre pestaña con los datos del conductor, tiempo de llegada
                            Intent intent = new Intent(ConfirmTravelActivity.this, TravelActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }else{
                            //en caso de que el correo ya se haya registrado  se muestra un mensaje en pantalla
                            Toast.makeText(ConfirmTravelActivity.this, "No se pudo realizar su peticion", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        //Cancelar viaje
        cancel_button = (Button) findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //elimina Request de Base de datos
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("usuarioRequest");
                ref.child(userId).setValue(null);

                //Se dirige a la pantalla inicial
                Intent intent = new Intent(ConfirmTravelActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_log:
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ubicacionUsuario");
                ref.child(userId).setValue(null);
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(ConfirmTravelActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return true;
    }

    // Obtiene el mapa y notifica cuando el mapa esta listo para ser usado
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
    public void onLocationChanged(Location location) {
        //actualiza constantemente la ubicacion del usuario (en tiempo real)
        if(getApplicationContext()!=null){
            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ubicacionUsuario");

            GeoFire geoFire = new GeoFire(ref);
            geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //muestra la localizacion del usuario y define el intervalo en el que se actualiza constantemente.
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ConfirmTravelActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
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
}
