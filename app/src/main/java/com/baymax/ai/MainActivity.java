package com.baymax.ai;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button signin;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signin = findViewById(R.id.signin);

        gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignIn(); 
            }
        });


    }

    private void SignIn() {
        Intent intent = gsc.getSignInIntent();
        startActivityForResult(intent,100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==100){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            ChatActivity();
            try {
                task.getResult(ApiException.class);
            } catch (ApiException e){
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            }


        }
    }

    private void ChatActivity() {
        finish();
        Intent intent = new Intent(getApplicationContext(),HomeActivity.class);
        startActivity(intent);
    }
}