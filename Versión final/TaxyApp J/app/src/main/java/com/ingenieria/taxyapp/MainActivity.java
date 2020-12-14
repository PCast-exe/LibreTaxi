package com.ingenieria.taxyapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //referencia de variables creadas a objetos
        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);

        mLogin = (Button) findViewById(R.id.logInButton);
        mRegistration = (Button) findViewById(R.id.signUpButton);

        mAuth = FirebaseAuth.getInstance();

        //al presionar boton registrar lleva a pantalla de registro
        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        //al presionar boton ingresar
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //se toman los datos ingresados en los campos mostrados en pantalla
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                //se autentifica que el correo y contraseña ingresados sean correctos y que ya esten registrados
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            //en caso de que el usuario este registrado ingresa a la pantalla de inicio donde se muestra el mapa con su ubicacion actual
                            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }else{
                            //en caso de que el usuario no este registrado o sus datos estan mal escritos se muestra un mensaje en pantalla
                            Toast.makeText(MainActivity.this, "No se pudo ingresar con este usuario", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
