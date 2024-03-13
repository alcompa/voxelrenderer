package com.alcompa.voxelrenderer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelSelectionActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MODEL_SELECTION";

    private Spinner spinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.activity_model_selection);

        String[] filenames = null;

        try{
            filenames = getAssets().list("");
        } catch (IOException e){
            Log.e(TAG, "Failed to load assets names");
            System.exit(-1);
        }

        Pattern pattern = Pattern.compile("\\b\\w+\\.vly\\b");
        Matcher matcher;

        ArrayList<String> modelsFilenames = new ArrayList<>();

        for(String filename: filenames){
            matcher = pattern.matcher(filename);
            if(matcher.find()){
                Log.v(TAG, filename + " file found");
                modelsFilenames.add(filename);
            }
        }

        Collections.sort(modelsFilenames);

        SpinnerAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modelsFilenames);
        spinner = (Spinner) findViewById(R.id.models_spinner);
        spinner.setAdapter(adapter);

        // Restore the selected item
        if (savedInstanceState != null) {
            int selectedIndex = savedInstanceState.getInt("selectedIndex", -1);
            if (selectedIndex != -1) {
                spinner.setSelection(selectedIndex);
            }
        }

        Button btn = (Button) findViewById(R.id.view_btn);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, ModelViewerActivity.class);
        intent.putExtra("modelFilename", spinner.getSelectedItem().toString());
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        int selectedIndex = spinner.getSelectedItemPosition();
        b.putInt("selectedIndex", selectedIndex);
    }

}
