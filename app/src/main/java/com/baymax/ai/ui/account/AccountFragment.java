package com.baymax.ai.ui.account;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.baymax.ai.HomeActivity;
import com.baymax.ai.R;
import com.baymax.ai.databinding.FragmentAccountBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AccountFragment extends Fragment {

    Button logout,info;
    TextView text_account;
    TextView email;
    ImageView displayPicture;

    private FragmentAccountBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel =
                new ViewModelProvider(this).get(AccountViewModel.class);

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        HomeActivity home = (HomeActivity) getActivity();
        text_account = (TextView) root.findViewById(R.id.text_account);
        email = (TextView) root.findViewById(R.id.email);
        info = (Button) root.findViewById(R.id.info);
        logout = (Button) root.findViewById(R.id.logout);
        info.setBackgroundColor(getResources().getColor(R.color.glass));
        displayPicture = (ImageView) root.findViewById(R.id.displayPicture);
        logout.setBackgroundColor(getResources().getColor(R.color.danger));
        logout.setTextColor(getResources().getColor(R.color.white));

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(getContext());
                alert.setTitle("Policies and Licenses");
                alert.setMessage("This app was created purely for educational and research purposes and not intended for any other use. This app was built with ♥ in Android Studio, Flask, Firestore and OpenAI GPT 3.5 by Akilesh S (akileshworks.now@gmail.com). This app is completely closed source and any illegal means to procure the source code is actionable.\n\nThe name 'HealthWave' and the images used are not our property.\n\nⓒ All rights reserved, 2024");
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alert.show();
            }
        });

        logout.setOnClickListener(v -> {
            try{
                home.SignOut();

            }catch( Exception e ){
                Toast.makeText(getContext() ,e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        text_account.setText(home.displayName);
        email.append(home.displayEmail);


        Glide.with(this)
             .load(home.displayPhoto)
             .placeholder(R.drawable
                     .alt_pic)
             .apply(RequestOptions.circleCropTransform())
             .into(displayPicture);


        return root;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}