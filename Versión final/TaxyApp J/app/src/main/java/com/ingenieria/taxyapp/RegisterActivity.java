package com.ingenieria.taxyapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText mName, mLastName, mEmail, mPassword, mPhone;
    private Button mLogin, mRegistration;

    //Variables de los datos a registrar
    private String name = "";
    private String lastname = "";
    private String email = "";
    private String password = "";
    private String phone = "";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private DatabaseReference mDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //referencia de variables (datos a registrar)
        mName = (EditText) findViewById(R.id.name);
        mLastName = (EditText) findViewById(R.id.lastname);
        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mPhone = (EditText) findViewById(R.id.phone);

        mLogin = (Button) findViewById(R.id.logInButton);
        mRegistration = (Button) findViewById(R.id.signUpButton);

        mAuth = FirebaseAuth.getInstance();
        mDataBase = FirebaseDatabase.getInstance().getReference();

        //al presionar boton ingresar lleva a pantalla de inicio
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        ///al presionar boton registrar
        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Convierte en texto las variables de los datos ingresados
                name = mName.getText().toString();
                lastname = mLastName.getText().toString();
                email = mEmail.getText().toString();
                password = mPassword.getText().toString();
                phone = mPhone.getText().toString();

                //Valida que los campos no esten vacios
                if (!name.isEmpty() && !lastname.isEmpty() && !email.isEmpty() && !password.isEmpty() && !phone.isEmpty()){
                    //Valida que la contraseña sea mayor o igual a 6 caracteres
                    if (password.length() >= 6){
                        registerUser();
                    }else{
                        //en caso de que el usuario inglese una contraseña no valida se muestra un mensaje en pantalla
                        Toast.makeText(RegisterActivity.this, "La contraseña debe de ser de al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    //en caso de que el usuario no complete todos los campos se muestra un mensaje en pantalla
                    Toast.makeText(RegisterActivity.this, "Debe de completar los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void registerUser(){
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull final Task<AuthResult> task) {
                if(task.isSuccessful()){

                    //se crea un objeto que almacena todas los datos inglesados en los campos por el usuario
                    Map<String, Object> map = new HashMap<>();
                    map.put("nombre", name);
                    map.put("apellidos", lastname);
                    map.put("email", email);
                    map.put("password", password);
                    map.put("telefono", phone);

                    //se obtiene el ID donde se almacenaran sus datos
                    String user_id = mAuth.getCurrentUser().getUid();

                    //se valida que el correo no sea de un usuario ya registrado
                    mDataBase.child("usuarios").child(user_id).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task2) {
                            if(task2.isSuccessful()){
                                //si no se ha registrado, se crea el nuevo usuario en la base de datos y se envia al usuario al menu principal para que pueda iniciar sesion
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }else{
                                //en caso de que el correo ya se haya registrado  se muestra un mensaje en pantalla
                                Toast.makeText(RegisterActivity.this, "No se pudo registrar este usuario correctamente", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Toast.makeText(RegisterActivity.this, "No se pudo registrar este usuario", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
