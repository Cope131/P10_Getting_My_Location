package com.myapplicationdev.android.p10_getting_my_location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private final String DEBUG_TAG = MainActivity2.class.getSimpleName();

    // Data
    private File recordFile;
    private File faveFile;

    // List View Components
    private ListView lv;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> locations;

    // Views
    private TextView recordsTV;
    private Button refreshBtn, favoritesBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        initViews();
        initLVComp();
        getFilePath();
        read(DataType.LOCATION);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initViews() {
        recordsTV = findViewById(R.id.number_of_records_text_view);
        refreshBtn = findViewById(R.id.refresh_button);
        favoritesBtn = findViewById(R.id.faves_button);
        refreshBtn.setOnClickListener(this);
        favoritesBtn.setOnClickListener(this);
    }

    private void initLVComp() {
        lv = findViewById(R.id.list_view);
        locations = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locations);
        lv.setAdapter(arrayAdapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh_button:
                read(DataType.LOCATION);
                break;
            case R.id.faves_button:
                read(DataType.FAVORITES);
                break;
        }
    }

    private void getFilePath() {
        String folderPath = getIntent().getStringExtra("folderPath");
        // Locations
        String fileName = getIntent().getStringExtra("fileName");
        recordFile = new File(folderPath, fileName);
        // Favorites
        fileName = "favorites.txt";
        faveFile = new File(folderPath, fileName);
    }

    private void read(DataType type) {
        File dataFile = type == DataType.LOCATION ? recordFile : faveFile;
        if (!dataFile.exists()) {
            locations.clear();
            arrayAdapter.notifyDataSetChanged();
            return;
        }

        if (dataFile != null && dataFile.exists()) {
            StringBuilder sb = new StringBuilder();
            try {
                FileReader reader = new FileReader(recordFile);
                BufferedReader br = new BufferedReader(reader);
                locations.clear();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line + "\n");
                    if (line != null) {
                        locations.add(line);
                    }
                    line = br.readLine();
                }
                br.close();
                reader.close();
                Log.d(DEBUG_TAG, "Content: \n" + sb.toString());
                Log.d(DEBUG_TAG, locations.toString());

                arrayAdapter.notifyDataSetChanged();
                recordsTV.setText(locations.size() + "");

                if (type == DataType.FAVORITES) {
                    // disable item click
                    lv.setOnItemClickListener(null);
                    return;
                }
                lv.setOnItemClickListener(this);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String location = locations.get(position);
        showDialog(location);
    }

    private void showDialog(String location) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb
                .setTitle("Add this location in your favorite list")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        writeRecord(location);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        AlertDialog ad = adb.create();
        ad.show();
    }

    private void writeRecord(String location) {
        String folderLocPath = getFilesDir().getAbsolutePath() + "/LocationRecords";
        recordFile = new File(folderLocPath, "favorites.txt");
        Log.d(DEBUG_TAG, folderLocPath + recordFile.getAbsolutePath());
        try {
            FileWriter writer = new FileWriter(recordFile, true);
            writer.write(location + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to write!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    enum DataType {
        LOCATION, FAVORITES
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}