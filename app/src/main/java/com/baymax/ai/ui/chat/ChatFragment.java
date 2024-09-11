package com.baymax.ai.ui.chat;

import static android.app.Activity.RESULT_OK;

import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

        imageChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_MULTIPLE_IMAGES);
            }
        });

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
    final int SELECT_MULTIPLE_IMAGES = 1;
    ArrayList<String> selectedImagesPaths; // Paths of the image(s) selected by the user.
    boolean imagesSelected = false;

    public void connectServer(View v) {


        String postUrl = FLASK_ENDPOINT;

        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        for (int i = 0; i < selectedImagesPaths.size(); i++) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                // Read BitMap by file path.
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImagesPaths.get(i), options);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            }catch(Exception e){
                return;
            }
            byte[] byteArray = stream.toByteArray();

            multipartBodyBuilder.addFormDataPart("image" + i, "Android_Flask_" + i + ".jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray));
        }

        RequestBody postBodyImage = multipartBodyBuilder.build();

//        RequestBody postBodyImage = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
//                .build();

        postRequest(postUrl, postBodyImage);
    }

    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == SELECT_MULTIPLE_IMAGES && resultCode == RESULT_OK && null != data) {
                // When a single image is selected.
                String currentImagePath;
                selectedImagesPaths = new ArrayList<>();
                if (data.getData() != null) {
                    Uri uri = data.getData();
                    currentImagePath = getPath(getContext(), uri);
                    Log.d("ImageDetails", "Single Image URI : " + uri);
                    Log.d("ImageDetails", "Single Image Path : " + currentImagePath);
                    selectedImagesPaths.add(currentImagePath);
                    imagesSelected = true;
                } else {
                    // When multiple images are selected.
                    // Thanks tp Laith Mihyar for this Stackoverflow answer : https://stackoverflow.com/a/34047251/5426539
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {

                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();

                            currentImagePath = getPath(getContext(), uri);
                            selectedImagesPaths.add(currentImagePath);
                            Log.d("ImageDetails", "Image URI " + i + " = " + uri);
                            Log.d("ImageDetails", "Image Path " + i + " = " + currentImagePath);
                            imagesSelected = true;
                        }
                    }
                }
            } else {
                Toast.makeText(getContext(), "You haven't Picked any Image.", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(getContext(), selectedImagesPaths.size() + " Image(s) Selected.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Something Went Wrong.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}