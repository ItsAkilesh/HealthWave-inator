package com.baymax.ai.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.baymax.ai.R;
import com.baymax.ai.databinding.FragmentHistoryBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    List<String> history = new ArrayList<>();
    List<String> docIdList = new ArrayList<>();
    private FragmentHistoryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ListView list = root.findViewById(R.id.list);
        history = new ArrayList<>();


        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, history);
        list.setAdapter(adapter);
        list.setOnItemClickListener(
            (parent, view, position, id) -> {
                Bundle bundle = new Bundle();
                bundle.putString("docId", docIdList.get(position));
                NavHostFragment.findNavController(HistoryFragment.this)
                        .navigate(R.id.action_navigation_history_to_navigation_chat, bundle);




            }
        );


        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        String displayEmail= "default";
        if (account!=null)
            displayEmail = account.getEmail();
        db.collection(displayEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object > hash = document.getData();
                            String title = hash.get("history").toString();
                            title = title.substring(1, Math.min(title.length(), 100)) + " ...";
                            history.add(title);
                            docIdList.add(document.getId());

                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Sorry, couldn't reach the DB." + task.getException(), Toast.LENGTH_SHORT).show();
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