package com.homebudget.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.homebudget.R;
import com.homebudget.database.entities.NotificationSettings;

public class SettingsDialog {

    private Context context;
    private int userId;
    private NotificationSettings settings;
    private OnSettingsSaveListener listener;

    private AlertDialog dialog;
    private Switch swEnabled;
    private Spinner spPeriod;
    private TimePicker timePicker;
    private CheckBox cbIncludeAi;
    private Button btnSave, btnCancel;

    public interface OnSettingsSaveListener {
        void onSave(NotificationSettings settings);
    }

    public SettingsDialog(Context context, int userId, OnSettingsSaveListener listener) {
        this.context = context;
        this.userId = userId;
        this.settings = new NotificationSettings(userId);
        this.listener = listener;
        createDialog();
    }

    public SettingsDialog(Context context, int userId, NotificationSettings settings,
                          OnSettingsSaveListener listener) {
        this.context = context;
        this.userId = userId;
        this.settings = settings;
        this.listener = listener;
        createDialog();
        populateFields();
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_settings, null);

        swEnabled = view.findViewById(R.id.sw_enabled);
        spPeriod = view.findViewById(R.id.sp_period);
        timePicker = view.findViewById(R.id.time_picker);
        cbIncludeAi = view.findViewById(R.id.cb_include_ai);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);

        timePicker.setIs24HourView(true);

        setupSpinner();

        swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("SettingsDialog", "Switch changed to: " + isChecked);
            spPeriod.setEnabled(isChecked);
            timePicker.setEnabled(isChecked);
            cbIncludeAi.setEnabled(isChecked);
        });

        btnSave.setOnClickListener(v -> saveSettings());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        builder.setView(view);
        dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void setupSpinner() {
        String[] periods = {"Ежедневно", "Еженедельно", "Ежемесячно"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPeriod.setAdapter(adapter);
    }

    private void populateFields() {
        if (settings != null) {
            Log.d("SettingsDialog", "Populating fields - enabled: " + settings.isEnabled());
            swEnabled.setChecked(settings.isEnabled());

            String period = settings.getPeriod();
            if ("daily".equals(period)) spPeriod.setSelection(0);
            else if ("weekly".equals(period)) spPeriod.setSelection(1);
            else spPeriod.setSelection(2);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                timePicker.setHour(settings.getHour());
                timePicker.setMinute(settings.getMinute());
            } else {
                timePicker.setCurrentHour(settings.getHour());
                timePicker.setCurrentMinute(settings.getMinute());
            }
            cbIncludeAi.setChecked(settings.isIncludeAiSummary());

            // Включаем/выключаем элементы в зависимости от состояния
            spPeriod.setEnabled(settings.isEnabled());
            timePicker.setEnabled(settings.isEnabled());
            cbIncludeAi.setEnabled(settings.isEnabled());
        }
    }

    private void saveSettings() {
        Log.d("SettingsDialog", "=== SAVING SETTINGS ===");
        Log.d("SettingsDialog", "Switch enabled: " + swEnabled.isChecked());

        settings.setEnabled(swEnabled.isChecked());

        String period;
        switch (spPeriod.getSelectedItemPosition()) {
            case 0: period = "daily"; break;
            case 1: period = "weekly"; break;
            default: period = "monthly"; break;
        }
        settings.setPeriod(period);

        int hour, minute;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }
        settings.setHour(hour);
        settings.setMinute(minute);
        settings.setIncludeAiSummary(cbIncludeAi.isChecked());

        Log.d("SettingsDialog", "Final settings:");
        Log.d("SettingsDialog", "  enabled: " + settings.isEnabled());
        Log.d("SettingsDialog", "  period: " + settings.getPeriod());
        Log.d("SettingsDialog", "  time: " + settings.getHour() + ":" + settings.getMinute());
        Log.d("SettingsDialog", "  includeAi: " + settings.isIncludeAiSummary());
        Log.d("SettingsDialog", "  settings id: " + settings.getId());

        listener.onSave(settings);
        dialog.dismiss();
    }

    public void show() {
        dialog.show();
    }
}