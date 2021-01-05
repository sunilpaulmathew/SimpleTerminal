package com.sunilpaulmathew.terminal.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.textview.MaterialTextView;
import com.sunilpaulmathew.terminal.R;
import com.sunilpaulmathew.terminal.fragments.LicenceFragment;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on December 29, 2020
 */

public class LicenceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

        AppCompatImageButton mBack = findViewById(R.id.back);
        MaterialTextView mCancel = findViewById(R.id.cancel_button);
        mCancel.setOnClickListener(v -> onBackPressed());

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new LicenceFragment()).commit();

        mBack.setOnClickListener(v -> onBackPressed());
    }

}