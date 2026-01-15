package com.example.fakecallangel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button btnAddCaller;
    ListView listView;

    ArrayList<String> callerNames = new ArrayList<>();
    ArrayList<String> callerFileNames = new ArrayList<>();
    ArrayAdapter<String> adapter;

    Uri tempUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAddCaller = findViewById(R.id.btnAddCaller);
        listView = findViewById(R.id.listViewCallers);

        // 1. Setup Adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, callerNames);
        listView.setAdapter(adapter);

        // 2. Load Saved Data (Restore list on startup)
        loadList();

        // 3. File Picker Setup
        ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            tempUri = uri;
                            showNameDialog();
                        }
                    }
                }
        );

        // 4. Add Button
        btnAddCaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Select voice file first", Toast.LENGTH_SHORT).show();
                filePickerLauncher.launch("audio/*");
            }
        });

        // 5. Short Click -> START CALL
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedName = callerNames.get(position);
                String selectedFile = callerFileNames.get(position);
                showTimerDialog(selectedName, selectedFile);
            }
        });

        // 6. Long Click -> DELETE CALLER
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteDialog(position);
                return true;
            }
        });
    }

    // --- DIALOGS ---

    private void showNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name this Caller (e.g. Mom)");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString();
                if (!name.isEmpty()) {
                    String fileName = "voice_" + System.currentTimeMillis() + ".mp3";
                    saveAudioFile(tempUri, fileName);

                    callerNames.add(name);
                    callerFileNames.add(fileName);
                    adapter.notifyDataSetChanged();

                    saveList(); // <--- SAVE TO MEMORY
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showTimerDialog(final String name, final String audioFileName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Call from " + name);
        builder.setMessage("Enter delay in seconds:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("5");
        builder.setView(input);

        builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();
                int seconds = 5;
                if (!text.isEmpty()) seconds = Integer.parseInt(text);

                Toast.makeText(MainActivity.this, "Waiting " + seconds + "s...", Toast.LENGTH_SHORT).show();

                // Hide App
                moveTaskToBack(true);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, FakeCallActivity.class);
                        intent.putExtra("name", name);
                        intent.putExtra("audioFileName", audioFileName);
                        startActivity(intent);
                    }
                }, seconds * 1000);
            }
        });
        builder.show();
    }

    private void showDeleteDialog(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Caller?")
                .setMessage("Are you sure you want to remove " + callerNames.get(position) + "?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Remove file to save space
                        File file = new File(getFilesDir(), callerFileNames.get(position));
                        if(file.exists()) file.delete();

                        // Remove from list
                        callerNames.remove(position);
                        callerFileNames.remove(position);
                        adapter.notifyDataSetChanged();

                        saveList(); // <--- UPDATE MEMORY
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    // --- HELPERS FOR SAVING DATA ---

    private void saveList() {
        SharedPreferences prefs = getSharedPreferences("FakeCallPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < callerNames.size(); i++) {
                JSONObject obj = new JSONObject();
                obj.put("name", callerNames.get(i));
                obj.put("file", callerFileNames.get(i));
                jsonArray.put(obj);
            }
            editor.putString("callersList", jsonArray.toString());
            editor.apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadList() {
        SharedPreferences prefs = getSharedPreferences("FakeCallPrefs", Context.MODE_PRIVATE);
        String jsonString = prefs.getString("callersList", null);

        if (jsonString != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                callerNames.clear();
                callerFileNames.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    callerNames.add(obj.getString("name"));
                    callerFileNames.add(obj.getString("file"));
                }
                adapter.notifyDataSetChanged();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void saveAudioFile(Uri uri, String fileName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getFilesDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
            outputStream.close();
            inputStream.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}