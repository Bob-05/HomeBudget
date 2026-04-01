package com.homebudget.ui.transaction;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.homebudget.R;
import com.homebudget.database.entities.Category;
import com.homebudget.database.entities.Transaction;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddEditTransactionDialog {

    private Context context;
    private List<Category> categories;
    private Transaction existingTransaction;
    private OnTransactionSaveListener listener;

    private AlertDialog dialog;
    private EditText etAmount, etNote;
    private Spinner spType, spCategory;
    private TextView tvDateTime;
    private Button btnSave, btnCancel;

    private Date selectedDate;
    private int selectedType = 1;

    public interface OnTransactionSaveListener {
        void onSave(Transaction transaction);
    }

    public AddEditTransactionDialog(Context context, List<Category> categories,
                                    OnTransactionSaveListener listener) {
        this.context = context;
        this.categories = categories;
        this.existingTransaction = null;
        this.listener = listener;
        this.selectedDate = new Date();
        createDialog();
    }

    public AddEditTransactionDialog(Context context, List<Category> categories,
                                    Transaction transaction, OnTransactionSaveListener listener) {
        this.context = context;
        this.categories = categories;
        this.existingTransaction = transaction;
        this.listener = listener;
        this.selectedDate = transaction.getDateTime();
        this.selectedType = "income".equals(transaction.getType()) ? 0 : 1;
        createDialog();
        populateFields();
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.RoundedCornersDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_add_transaction, null);

        etAmount = view.findViewById(R.id.et_amount);
        etNote = view.findViewById(R.id.et_note);
        spType = view.findViewById(R.id.sp_type);
        spCategory = view.findViewById(R.id.sp_category);
        tvDateTime = view.findViewById(R.id.tv_date_time);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);

        setupSpinners();

        tvDateTime.setOnClickListener(v -> showDateTimePicker());
        updateDateTimeDisplay();

        btnSave.setOnClickListener(v -> saveTransaction());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        builder.setView(view);
        dialog = builder.create();
    }

    private void setupSpinners() {
        // Тип транзакции
        String[] types = {"Доход", "Расход"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);
        spType.setSelection(selectedType);

        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Категории - используем обычный ArrayAdapter<String>
        if (categories != null && !categories.isEmpty()) {
            // Создаем массив названий категорий
            String[] categoryNames = new String[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                categoryNames[i] = categories.get(i).getName();
            }

            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, categoryNames);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategory.setAdapter(categoryAdapter);
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);

        DatePickerDialog datePicker = new DatePickerDialog(context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    TimePickerDialog timePicker = new TimePickerDialog(context,
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                selectedDate = calendar.getTime();
                                updateDateTimeDisplay();
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                    timePicker.show();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void updateDateTimeDisplay() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",
                java.util.Locale.getDefault());
        tvDateTime.setText(sdf.format(selectedDate));
    }

    private void populateFields() {
        if (existingTransaction != null) {
            etAmount.setText(String.valueOf(existingTransaction.getAmount()));
            if (existingTransaction.getNote() != null) {
                etNote.setText(existingTransaction.getNote());
            }

            if (categories != null && !categories.isEmpty()) {
                // Находим позицию категории по ID
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId() == existingTransaction.getCategoryId()) {
                        spCategory.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError("Введите сумму");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Введите корректную сумму");
            return;
        }

        if (amount <= 0) {
            etAmount.setError("Сумма должна быть положительной");
            return;
        }

        if (categories == null || categories.isEmpty()) {
            Toast.makeText(context, "Нет доступных категорий", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spCategory.getSelectedItemPosition();
        Category selectedCategory = categories.get(selectedPosition);
        String type = selectedType == 0 ? "income" : "expense";
        String note = etNote.getText().toString().trim();

        Transaction transaction;
        if (existingTransaction != null) {
            transaction = existingTransaction;
            transaction.setAmount(amount);
            transaction.setCategoryId(selectedCategory.getId());
            transaction.setType(type);
            transaction.setDateTime(selectedDate);
            transaction.setNote(note);
        } else {
            transaction = new Transaction(0, selectedCategory.getId(), type,
                    amount, selectedDate, note);
        }

        listener.onSave(transaction);
        dialog.dismiss();
    }

    public void show() {
        dialog.show();
    }
}