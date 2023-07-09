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

import com.baymax.ai.R;
import com.baymax.ai.databinding.FragmentChatBinding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
     public EditText input;
    public Button send;
    public ListView list;
    public ArrayList<String> history;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        binding = FragmentChatBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        input = root.findViewById(R.id.input);
        send = root.findViewById(R.id.send);
        list = root.findViewById(R.id.list);
        history = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, history);
        list.setAdapter(adapter);


        send.setOnClickListener(new View.OnClickListener() {
            private static final String FLASK_ENDPOINT = "http://192.168.1.100:5000/askgpt";
            @Override
            public void onClick(View v) {
                if (input!=null) {
                    String query =  input.getText().toString();
                    String prompt = String.join("\n", history)  + query;
                    String gen_message;
                    history.add(query);
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
                        String requestBody = "Message=" + prompt;

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
                            gen_message = response.substring(12, (response.length()-2));
                        } else {
                            gen_message = "API request failed with response code: " + responseCode;
                        }

                        connection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        gen_message = "API request failed: " + e.getMessage();

                    }

                   history.add(gen_message);
                    adapter.notifyDataSetChanged();

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