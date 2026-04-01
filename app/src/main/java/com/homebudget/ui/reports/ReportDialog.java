package com.homebudget.ui.reports;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.homebudget.R;
import java.util.Calendar;
import java.util.Date;

public class ReportDialog {

    private Context context;
    private int userId;
    private OnReportGenerateListener listener;

    private AlertDialog dialog;
    private TextView tvStartDate, tvEndDate;
    private RadioGroup rgReportType;
    private RadioButton rbIncome, rbExpense, rbSummary, rbCategories, rbPeriod;
    private Button btnGenerate, btnCancel;

    private Date startDate;
    private Date endDate;
    private String selectedReportType = "summary";

    public interface OnReportGenerateListener {
        void onGenerate(String reportType, Date startDate, Date endDate);
    }

    public ReportDialog(Context context, int userId, OnReportGenerateListener listener) {
        this.context = context;
        this.userId = userId;
        this.listener = listener;

        Calendar cal = Calendar.getInstance();
        endDate = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        startDate = cal.getTime();

        createDialog();
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.RoundedCornersDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_report, null);

        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        rgReportType = view.findViewById(R.id.rg_report_type);
        rbIncome = view.findViewById(R.id.rb_income);
        rbExpense = view.findViewById(R.id.rb_expense);
        rbSummary = view.findViewById(R.id.rb_summary);
        rbCategories = view.findViewById(R.id.rb_categories);
        rbPeriod = view.findViewById(R.id.rb_period);
        btnGenerate = view.findViewById(R.id.btn_generate);
        btnCancel = view.findViewById(R.id.btn_cancel);

        updateDateDisplay();

        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));

        rgReportType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_income) {
                selectedReportType = "income";
            } else if (checkedId == R.id.rb_expense) {
                selectedReportType = "expense";
            } else if (checkedId == R.id.rb_summary) {
                selectedReportType = "summary";
            } else if (checkedId == R.id.rb_categories) {
                selectedReportType = "categories";
            } else if (checkedId == R.id.rb_period) {
                selectedReportType = "period";
            }
        });

        btnGenerate.setOnClickListener(v -> {
            if (startDate == null || endDate == null) {
                Toast.makeText(context, "Выберите период", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startDate.after(endDate)) {
                Toast.makeText(context, "Начальная дата не может быть позже конечной", Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onGenerate(selectedReportType, startDate, endDate);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        builder.setView(view);
        dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showDatePicker(boolean isStart) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(isStart ? startDate : endDate);

        DatePickerDialog datePicker = new DatePickerDialog(context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    if (isStart) {
                        startDate = calendar.getTime();
                    } else {
                        endDate = calendar.getTime();
                    }
                    updateDateDisplay();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void updateDateDisplay() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy",
                java.util.Locale.getDefault());
        tvStartDate.setText(sdf.format(startDate));
        tvEndDate.setText(sdf.format(endDate));
    }

    public void show() {
        dialog.show();
    }
}