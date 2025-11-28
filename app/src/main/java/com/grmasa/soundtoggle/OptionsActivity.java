package com.grmasa.soundtoggle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OptionsActivity extends AppCompatActivity {

    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private static final String KEY_EXCLUDED_MODES = "excluded_modes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        LinearLayout checkBoxLayout = findViewById(R.id.ll_exclude_modes);
        for (SoundModes.Mode mode : SoundModes.MODES) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(mode.name);
            checkBox.setTag(mode.name);
            checkBoxLayout.addView(checkBox);
            checkBoxes.add(checkBox);
        }

        Button btnSave = findViewById(R.id.btn_save_exclusions);

        loadExclusions();

        btnSave.setOnClickListener(v -> {
            saveExclusions();
            Toast.makeText(this, "Exclusions saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadExclusions() {
        SharedPreferences sharedpreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        Set<String> excluded = sharedpreferences.getStringSet(KEY_EXCLUDED_MODES, new HashSet<>());

        for (CheckBox checkBox : checkBoxes) {
            checkBox.setChecked(excluded.contains(checkBox.getTag().toString()));
        }
    }

    private void saveExclusions() {
        Set<String> excluded = new HashSet<>();
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                excluded.add(checkBox.getTag().toString());
            }
        }

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_EXCLUDED_MODES, excluded).apply();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return false;
    }
}
