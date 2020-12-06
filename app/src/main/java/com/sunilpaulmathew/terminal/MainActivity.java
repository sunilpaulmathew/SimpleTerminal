package com.sunilpaulmathew.terminal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.sunilpaulmathew.terminal.utils.Utils;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on September 25, 2020
 */

public class MainActivity extends AppCompatActivity {

    private AppCompatEditText mShellCommand;
    private MaterialTextView mShellCommandTitle, mShellOutput;
    private boolean mExit, mSU = false, mRunning = false;
    private CharSequence mHistory = null;
    private Handler mHandler = new Handler();
    private int i;
    private List<String> mResult = null, PWD = null, whoAmI = null;
    private NestedScrollView mScrollView;
    private String[] mCommand;
    private StringBuilder mLastCommand;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize App Theme
        Utils.initializeAppTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatImageButton mEnter = findViewById(R.id.enter_button);
        AppCompatImageButton mUpButtom = findViewById(R.id.up_button);
        mShellCommand = findViewById(R.id.shell_command);
        mShellCommandTitle = findViewById(R.id.shell_command_title);
        mShellOutput = findViewById(R.id.shell_output);
        mScrollView = findViewById(R.id.scroll_view);

        mShellCommand.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().contains("\n")) {
                    runShellCommand(MainActivity.this);
                }
            }
        });
        mEnter.setOnClickListener(v -> runShellCommand(this));
        mUpButtom.setOnClickListener(v -> {
            String[] lines = mLastCommand.toString().split(",");
            PopupMenu popupMenu = new PopupMenu(this, mShellCommand);
            Menu menu = popupMenu.getMenu();
            if (mLastCommand.toString().isEmpty()) {
                return;
            }
            for (i = 0; i < lines.length; i++) {
                menu.add(Menu.NONE, i, Menu.NONE, lines[i]);
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                for (i = 0; i < lines.length; i++) {
                    if (item.getItemId() == i) {
                        mShellCommand.setText(lines[i]);
                    }
                }
                return false;
            });
            popupMenu.show();
        });

        refreshStatus();
    }

    public void refreshStatus() {
        new Thread() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(() -> {
                            if (mRunning) {
                                mShellOutput.setTextIsSelectable(false);
                                mScrollView.fullScroll(NestedScrollView.FOCUS_DOWN);
                                try {
                                    mShellOutput.setText(Utils.getOutput(mResult));
                                } catch (ConcurrentModificationException | NullPointerException ignored) {
                                }
                            } else {
                                mShellOutput.setTextIsSelectable(true);
                            }
                        });
                    }
                } catch (InterruptedException ignored) {}
            }
        }.start();
    }

    @SuppressLint({"SetTextI18n", "StaticFieldLeak"})
    private void runShellCommand(Context context) {
        if (mShellCommand.getText() == null || mShellCommand.getText().toString().isEmpty()) {
            return;
        }
        String[] array = Objects.requireNonNull(mShellCommand.getText()).toString().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String s : array) {
            if (s != null && !s.isEmpty())
                sb.append(" ").append(s);
        }
        mCommand = new String[] {sb.toString().replaceFirst(" ", "")};
        mLastCommand.append(mCommand[0]).append(",");
        if (mShellCommand.getText() != null && !mCommand[0].isEmpty()) {
            if (mCommand[0].equals("clear")) {
                clearAll();
                return;
            }
            if (mCommand[0].equals("exit")) {
                if (mSU) {
                    mSU = false;
                    whoAmI = new ArrayList<>();
                    Utils.runCommand("whoami", whoAmI);
                    mShellCommand.setText(null);
                    mShellCommandTitle.setText(Utils.getOutput(whoAmI) + ": " + Utils.getOutput(PWD));
                } else {
                    mShellCommand.setText(null);
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(R.string.exit_confirmation)
                            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            })
                            .setPositiveButton(R.string.exit, (dialog, which) -> {
                                super.onBackPressed();
                            })
                            .show();
                }
                return;
            }
            if (mCommand[0].equals("su") || mCommand[0].startsWith("su ")) {
                if (mSU && Utils.rootAccess()) {
                    mShellCommand.setText(null);
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(R.string.root_status_available)
                            .setPositiveButton(R.string.cancel, (dialog, which) -> {
                            })
                            .show();
                    return;
                } else if (Utils.rootAccess()) {
                    mSU = true;
                    mShellCommand.setText(null);
                    whoAmI = new ArrayList<>();
                    Utils.runRootCommand("whoami", whoAmI);
                    mShellCommandTitle.setText(Utils.getOutput(whoAmI) + ": " + Utils.getOutput(PWD));
                    return;
                } else {
                    mShellCommand.setText(null);
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(R.string.root_status_unavailable)
                            .setPositiveButton(R.string.cancel, (dialog, which) -> {
                            })
                            .show();
                    return;
                }
            }
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mRunning = true;
                mHistory = mShellOutput.getText();
                mResult = new ArrayList<>();
                PWD = new ArrayList<>();
                mScrollView.fullScroll(NestedScrollView.FOCUS_UP);
            }
            @SuppressLint("WrongThread")
            @Override
            protected Void doInBackground(Void... voids) {
                if (mShellCommand.getText() != null && !mCommand[0].isEmpty()) {
                    mResult.add(whoAmI + ": " + mCommand[0]);
                    if (mSU) {
                        Utils.runRootCommand(mCommand[0], mResult);
                        Utils.runRootCommand("pwd", PWD);
                    } else {
                        Utils.runCommand(mCommand[0], mResult);
                        Utils.runCommand("pwd", PWD);
                    }
                    if (Utils.getOutput(mResult).equals(whoAmI + ": " + mCommand[0] + "\n")) {
                        mResult.add(whoAmI + ": " + mCommand[0] + "\n" + mCommand[0]);
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mShellCommand.setText(null);
                mShellCommand.requestFocus();
                mShellOutput.setText(Utils.getOutput(mResult) + "\n\n" + mHistory);
                mShellCommandTitle.setText(Utils.getOutput(whoAmI) + ": " + Utils.getOutput(PWD));
                mHistory = null;
                mRunning = false;
                mShellOutput.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(mShellCommand, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.dismiss, v -> snackbar.dismiss());
        snackbar.show();
    }

    private void close() {
        Utils.closeSU();
        super.onBackPressed();
    }

    private void clearAll() {
        mShellOutput.setText(null);
        mShellOutput.setVisibility(View.GONE);
        mShellCommand.setText(null);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onStart() {
        super.onStart();

        mShellCommand.requestFocus();
        mLastCommand = new StringBuilder();
        whoAmI = new ArrayList<>();
        PWD = new ArrayList<>();
        Utils.runCommand("whoami", whoAmI);
        Utils.runCommand("pwd", PWD);
        mShellCommandTitle.setText(Utils.getOutput(whoAmI) + ": " + Utils.getOutput(PWD));
    }

    @Override
    public void onBackPressed() {
        if (mRunning) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.stop_command_question, mCommand[0]))
                    .setNegativeButton(getString(R.string.cancel), (dialog1, id1) -> {
                    })
                    .setPositiveButton(getString(R.string.exit), (dialog1, id1) -> {
                        close();
                    }).show();
            return;
        }
        if (mExit) {
            mExit = false;
            super.onBackPressed();
        } else {
            showSnackbar(getString(R.string.press_back));
            mExit = true;
            mHandler.postDelayed(() -> mExit = false, 2000);
        }
    }

}