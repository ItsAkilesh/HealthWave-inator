package com.baymax.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.baymax.ai.databinding.ActivityHomeBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    public String displayName, displayEmail,dob;
    public Boolean gender;
    public Uri displayPhoto;
    public int age;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        //AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
         //       R.id.navigation_chat, R.id.navigation_history, R.id.navigation_account)
           //     .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_chat);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);


        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestProfile().build();
        gsc = GoogleSignIn.getClient(HomeActivity.this,gso);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(HomeActivity.this);
        if(account!=null){
            displayName= account.getDisplayName();
            displayEmail= account.getEmail();
            displayPhoto=account.getPhotoUrl();
            dob=account.getGrantedScopes().contains("https://www.googleapis.com/auth/user.birthday.read") ? account.getDisplayName() : null;
            //Toast.makeText(this, dob, Toast.LENGTH_SHORT).show();
            gender = account.getGrantedScopes().contains("https://www.googleapis.com/auth/user.gender.read") ? account.getGrantedScopes().contains("https://www.googleapis.com/auth/user.birthday.read") : null;
    }

    }
    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }
    public void SignOut() {
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
            }
        });
    }
}