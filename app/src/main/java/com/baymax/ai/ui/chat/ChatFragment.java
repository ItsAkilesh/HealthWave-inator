package com.baymax.ai.ui.chat;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.baymax.ai.R;
import com.baymax.ai.databinding.FragmentChatBinding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private static final String API_URL = "https://api.openai.com/v1/engines/davinci-codex/completions";
    private static final String API_KEY = "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";
    public TextView user_input,gpt_out;
    public EditText input;
    public Button send;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        ChatViewModel chatViewModel =

                new ViewModelProvider(this).get(ChatViewModel.class);

        binding = FragmentChatBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        user_input = (TextView) root.findViewById(R.id.user_input);
        gpt_out=(TextView) root.findViewById(R.id.gpt_out);
        input = (EditText) root.findViewById(R.id.input);
        send = (Button) root.findViewById(R.id.send);

        send.setOnClickListener(new View.OnClickListener() {
            private static final String FLASK_ENDPOINT = "http:/192.168.1.100:5000/askgpt";
            @Override
            public void onClick(View v) {
                if (input!=null) {
                    String query = input.getText().toString();
                    String gen_message;
                    user_input.setText(query);
                    InputMethodManager inputManager = (InputMethodManager) root.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(root.getWindowToken(), 0);
                    input.setText("");

                    try {
                        URL url = new URL(FLASK_ENDPOINT);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);

                        // Construct the request body
                        String requestBody = "Message=" + query;

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
                            gen_message = response.toString().substring(12, (response.length()-2));
                        } else {
                            gen_message = "API request failed with response code: " + responseCode;
                        }

                        connection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        gen_message = "API request failed: " + e.getMessage();
                        gpt_out.setText("");
                    }

                    gpt_out.setText(gen_message);

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