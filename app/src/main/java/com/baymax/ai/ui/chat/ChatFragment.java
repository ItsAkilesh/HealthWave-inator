package com.baymax.ai.ui.chat;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.baymax.ai.HomeActivity;
import com.baymax.ai.R;
import com.baymax.ai.databinding.FragmentChatBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import org.apache.commons.text.StringEscapeUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class ChatFragment extends Fragment {

    private static final String FLASK_ENDPOINT = "http://192.168.29.100:5000/askgpt";
    private FragmentChatBinding binding;
     public EditText input;
    public Button send,newChat;
    public ListView list;
    public Button imageChat;
    public ArrayList<String> history;

    public String docId;

    public HomeActivity home;

    public Callable<String> createOrUpdateHistory;

    public FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ArrayAdapter<String> adapter;

    public String displayEmail;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        binding = FragmentChatBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        home = (HomeActivity) getActivity();

        input = root.findViewById(R.id.input);
        send = root.findViewById(R.id.send);
        newChat = root.findViewById(R.id.newChat);
        list = root.findViewById(R.id.list);
        imageChat = root.findViewById(R.id.imageChat);

        Map<String, Object> historyChat = new HashMap<>();
        history = new ArrayList<>();
        historyChat.put("history", history);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, history);
        list.setAdapter(adapter);


        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        displayEmail= account.getEmail();
         Bundle bundle = getArguments();
         if(bundle != null )
            docId = bundle.getString("docId", null);

        newChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                docId = null;
                NavHostFragment.findNavController(ChatFragment.this)
                        .navigate(R.id.action_navigation_chat_self, bundle);

            }
        });

        imageChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        if(docId != null){
            db.collection(displayEmail)
                    .document(docId)
                    .get()
                    .addOnCompleteListener((task) ->{
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()){
                           Map<String, Object> hash = document.getData();
                           history.clear();
                           history.addAll(( ArrayList<String>) hash.get("history"));
                            adapter.notifyDataSetChanged();
                            Toast.makeText(home, history.toString(), Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(home, "No such chat found!", Toast.LENGTH_SHORT).show();
                        }
                    })
            ;
        }

        createOrUpdateHistory = (Callable<String>) () -> {
            db.collection(displayEmail)
                    .document(docId)
                    .set(historyChat)
                    .addOnSuccessListener((documentReference)->
                            Toast.makeText(getContext(), "Added to DB successfully", Toast.LENGTH_SHORT).show()
                    ).addOnFailureListener(e ->
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
            return "Works";
        };


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (input!=null) {
                    String query =  input.getText().toString();
                    String prompt = history.stream().map(i -> i.substring(6)).collect(Collectors.joining("\n")) + query;
                    String gen_message;
                    history.add("You:    " + query);
                    adapter.notifyDataSetChanged();
                    InputMethodManager inputManager = (InputMethodManager) root.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(root.getWindowToken(), 0);
                    input.setText("");

                    try {
                        URL url = new URL(FLASK_ENDPOINT);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);

                        // Construct the request body
                        String requestBody = "Message=" + prompt + "Decline to answer if question is not medically relevant.";

                        OutputStream outputStream = connection.getOutputStream();
                        outputStream.write(requestBody.getBytes());
                        outputStream.flush();
                        outputStream.close();

                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            reader.close();

                            // Extract the generated message from the JSON response
                            gen_message = response.substring(15, (response.length()-2));
                            gen_message = StringEscapeUtils.unescapeJava(gen_message);
                        } else {
                            gen_message = "API request failed with response code: " + responseCode;
                        }

                        connection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        gen_message = "API request failed: " + e.getMessage();

                    }

                   history.add("HealthWave: " + gen_message);
                   adapter.notifyDataSetChanged();
                    if( docId == null){
                        db.collection(displayEmail)
                                .add(historyChat)
                                .addOnSuccessListener(documentReference ->{
                                    try {
                                        createOrUpdateHistory.call();
                                    } catch (Exception e) {
                                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT);
                                    }
                                    docId= documentReference.getId();
                                }).addOnFailureListener(e ->
                                        Toast.makeText(home, e.getMessage(), Toast.LENGTH_SHORT)
                                );
                    }
                    try {
                        createOrUpdateHistory.call();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT);
                    }

                }
                else
                    Toast.makeText(getContext(), "Cannot send empty message", Toast.LENGTH_SHORT).show();
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