package com.baymax.ai.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.baymax.ai.HomeActivity;
import com.baymax.ai.R;
import com.baymax.ai.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    Button logout;
    private FragmentAccountBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel =
                new ViewModelProvider(this).get(AccountViewModel.class);

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        logout = (Button) root.findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) throws NullPointerException{
                try{
                    ((HomeActivity) getActivity()).SignOut();

                }catch( Exception e ){
                    Toast.makeText(getContext() ,e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        return root;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}