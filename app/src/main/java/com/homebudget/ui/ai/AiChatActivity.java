package com.homebudget.ui.ai;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.homebudget.R;
import com.homebudget.adapters.ChatMessageAdapter;
import com.homebudget.database.entities.AiChatHistory;
import com.homebudget.network.AiApiService;
import com.homebudget.network.YandexGptApiService;
import com.homebudget.ui.base.BaseActivity;
import com.homebudget.viewmodels.AiViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiChatActivity extends BaseActivity {

    private static final String TAG = "AiChatActivity";

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnAttachReport, btnSend, btnHistory;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;

    private ChatMessageAdapter adapter;
    private AiViewModel viewModel;
    private AiApiService apiService;

    private Date selectedStartDate;
    private Date selectedEndDate;
    private String selectedReportType = "summary";

    private final String[] reportTypes = {
            "📊 Сводный отчет (доходы/расходы)",
            "💰 Отчет по доходам",
            "💸 Отчет по расходам",
            "📂 Отчет по категориям",
            "📅 Список транзакций за период"
    };

    private final String[] reportTypeValues = {
            "summary", "income", "expense", "categories", "period"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        loadYandexGptSettings();

        setupBackButton();
        initViews();
        setupViewModel();
        setupRecyclerView();
        setupClickListeners();

        apiService = new AiApiService(this);

        Calendar cal = Calendar.getInstance();
        selectedEndDate = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        selectedStartDate = cal.getTime();

        int userId = viewModel.getCurrentUserId();
        if (userId != -1) {
            viewModel.loadChatHistory(userId);
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        etMessage = findViewById(R.id.et_message);
        btnAttachReport = findViewById(R.id.btn_attach_report);
        btnSend = findViewById(R.id.btn_send);
        btnHistory = findViewById(R.id.btn_history);
        progressBar = findViewById(R.id.progress_bar);
        layoutEmpty = findViewById(R.id.layout_empty);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AiViewModel.class);

        viewModel.getChatHistory().observe(this, history -> {
            Log.d(TAG, "Chat history updated, size: " + (history != null ? history.size() : 0));
            if (history != null) {
                adapter.updateList(history);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);

                if (layoutEmpty != null) {
                    layoutEmpty.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                etMessage.setEnabled(false);
                btnAttachReport.setEnabled(false);
                btnSend.setEnabled(false);
                btnHistory.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                etMessage.setEnabled(true);
                btnAttachReport.setEnabled(true);
                btnSend.setEnabled(true);
                btnHistory.setEnabled(true);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendMessage());
        }

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });

        btnAttachReport.setOnClickListener(v -> showReportPeriodDialog());
        btnHistory.setOnClickListener(v -> showChatHistoryDialog());
    }

    private void showReportPeriodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);

        View view = getLayoutInflater().inflate(R.layout.dialog_report_period, null);
        TextView tvStartDate = view.findViewById(R.id.tv_start_date);
        TextView tvEndDate = view.findViewById(R.id.tv_end_date);
        TextView tvReportType = view.findViewById(R.id.tv_report_type);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        tvStartDate.setText(sdf.format(selectedStartDate));
        tvEndDate.setText(sdf.format(selectedEndDate));
        tvReportType.setText(getReportTypeName(selectedReportType));

        tvStartDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedStartDate);
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view1, year, month, dayOfMonth) -> {
                        cal.set(year, month, dayOfMonth);
                        selectedStartDate = cal.getTime();
                        tvStartDate.setText(sdf.format(selectedStartDate));
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        tvEndDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedEndDate);
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view1, year, month, dayOfMonth) -> {
                        cal.set(year, month, dayOfMonth);
                        selectedEndDate = cal.getTime();
                        tvEndDate.setText(sdf.format(selectedEndDate));
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        tvReportType.setOnClickListener(v -> {
            AlertDialog.Builder typeBuilder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
            typeBuilder.setTitle("Выберите тип отчёта");

            int checkedItem = getReportTypeIndex(selectedReportType);

            typeBuilder.setSingleChoiceItems(reportTypes, checkedItem, (dialog, which) -> {
                selectedReportType = reportTypeValues[which];
                tvReportType.setText(getReportTypeName(selectedReportType));
                dialog.dismiss();
            });

            AlertDialog typeDialog = typeBuilder.create();
            typeDialog.show();
        });

        builder.setView(view);
        builder.setTitle("Настройки отчёта");

        builder.setPositiveButton("Приложить к сообщению", (dialog, which) -> {
            String reportText = generateReportText();
            etMessage.setText(reportText);
            etMessage.setSelection(etMessage.getText().length());
            Toast.makeText(this, "Отчёт добавлен в сообщение. Можете дополнить его своим текстом.", Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton("Отправить сразу", (dialog, which) -> {
            sendReportRequest();
        });

        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#2196F3"));
        }
        if (neutralButton != null) {
            neutralButton.setTextColor(Color.parseColor("#4CAF50"));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#BDBDBD"));
        }
    }

    private void showChatHistoryDialog() {
        List<AiChatHistory> currentMessages = adapter.getMessages();
        if (currentMessages == null || currentMessages.isEmpty()) {
            Toast.makeText(this, "Нет сохраненных вопросов", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AiChatHistory> questionsOnly = new ArrayList<>();
        for (AiChatHistory msg : currentMessages) {
            if (msg.getQuestion() != null && !msg.getQuestion().isEmpty()) {
                questionsOnly.add(msg);
            }
        }

        if (questionsOnly.isEmpty()) {
            Toast.makeText(this, "Нет сохраненных вопросов", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] historyItems = new String[questionsOnly.size()];
        for (int i = 0; i < questionsOnly.size(); i++) {
            String question = questionsOnly.get(i).getQuestion();
            historyItems[i] = (question.length() > 50 ? question.substring(0, 50) + "..." : question);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_chat_history, null);
        ListView listView = dialogView.findViewById(R.id.list_view_history);
        Button btnClearAll = dialogView.findViewById(R.id.btn_clear_all);

        ArrayAdapter<String> historyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyItems);
        listView.setAdapter(historyAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            AiChatHistory selected = questionsOnly.get(position);
            showChatMessageDialog(selected);
        });

        btnClearAll.setOnClickListener(v -> {
            AlertDialog confirmDialog = new AlertDialog.Builder(this, R.style.RoundedCornersDialog)
                    .setTitle("Очистить историю")
                    .setMessage("Вы уверены, что хотите очистить всю историю чата?")
                    .setPositiveButton("Очистить", (d, w) -> {
                        int userId = viewModel.getCurrentUserId();
                        if (userId != -1) {
                            viewModel.clearChatHistory(userId);
                            d.dismiss();
                            Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .create();
            confirmDialog.show();

            confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#F44336"));
            confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9E9E9E"));
        });

        builder.setView(dialogView);
        builder.setNegativeButton("Закрыть", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#BDBDBD"));
        }
    }

    private int getReportTypeIndex(String reportType) {
        for (int i = 0; i < reportTypeValues.length; i++) {
            if (reportTypeValues[i].equals(reportType)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Отправляет обычное сообщение (то, что пользователь ввёл в поле)
     */
    private void sendMessage() {
        String userQuestion = etMessage.getText().toString().trim();
        if (userQuestion.isEmpty()) {
            Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show();
            return;
        }

        loadYandexGptSettings();

        if (!YandexGptApiService.isConfigured()) {
            Toast.makeText(this, "⚠️ Сначала настройте YandexGPT в главном меню", Toast.LENGTH_LONG).show();
            return;
        }

        etMessage.setText("");

        int userId = viewModel.getCurrentUserId();
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        // Сохраняем вопрос с временным ответом
        viewModel.saveChatHistory(userId, userQuestion, "🤔 Генерирую ответ...");
        viewModel.getIsLoading().postValue(true);

        apiService.sendMessage(userQuestion, userId, selectedStartDate, selectedEndDate, selectedReportType, new AiApiService.AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    viewModel.getIsLoading().postValue(false);
                    viewModel.updateLastChatResponse(userId, userQuestion, response);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    viewModel.getIsLoading().postValue(false);
                    viewModel.updateLastChatResponse(userId, userQuestion, "❌ " + error);
                    Toast.makeText(AiChatActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Отправляет запрос на анализ отчёта с выбранными параметрами
     */
    private void sendReportRequest() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String reportTypeName = getReportTypeName(selectedReportType);

        String userQuestion = "Проанализируй мои финансы за период с " +
                sdf.format(selectedStartDate) + " по " + sdf.format(selectedEndDate) +
                ". Тип отчета: " + reportTypeName +
                ". Дай рекомендации по оптимизации расходов.";

        loadYandexGptSettings();

        if (!YandexGptApiService.isConfigured()) {
            Toast.makeText(this, "⚠️ Сначала настройте YandexGPT в главном меню", Toast.LENGTH_LONG).show();
            return;
        }

        int userId = viewModel.getCurrentUserId();
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        etMessage.setText("");

        // Сохраняем вопрос с временным ответом
        viewModel.saveChatHistory(userId, userQuestion, "🤔 Генерирую ответ...");
        viewModel.getIsLoading().postValue(true);

        apiService.sendMessage(userQuestion, userId, selectedStartDate, selectedEndDate, selectedReportType, new AiApiService.AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    viewModel.getIsLoading().postValue(false);
                    viewModel.updateLastChatResponse(userId, userQuestion, response);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    viewModel.getIsLoading().postValue(false);
                    viewModel.updateLastChatResponse(userId, userQuestion, "❌ " + error);
                    Toast.makeText(AiChatActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadYandexGptSettings() {
        SharedPreferences prefs = getSharedPreferences("yandex_gpt_prefs", MODE_PRIVATE);
        String apiKey = prefs.getString("api_key", "");
        String folderId = prefs.getString("folder_id", "");

        Log.d(TAG, "Loading YandexGPT settings - API Key: " + (apiKey.isEmpty() ? "empty" : "present") +
                ", Folder ID: " + (folderId.isEmpty() ? "empty" : "present"));

        if (!apiKey.isEmpty() && !folderId.isEmpty()) {
            YandexGptApiService.setApiKey(apiKey);
            YandexGptApiService.setFolderId(folderId);
            YandexGptApiService.reloadSettings(this);
            Log.d(TAG, "✅ YandexGPT settings loaded and applied");
        } else {
            Log.w(TAG, "⚠️ YandexGPT settings NOT found");
        }
    }

    private String getReportTypeName(String reportType) {
        switch (reportType) {
            case "income": return "Отчет по доходам";
            case "expense": return "Отчет по расходам";
            case "summary": return "Сводный отчет";
            case "categories": return "Отчет по категориям";
            case "period": return "Список транзакций";
            default: return "Сводный отчет";
        }
    }

    private String generateReportText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String reportTypeName = getReportTypeName(selectedReportType);
        return "Проанализируй мои финансы за период с " +
                sdf.format(selectedStartDate) + " по " + sdf.format(selectedEndDate) +
                ". Тип отчета: " + reportTypeName +
                ". Дай рекомендации по оптимизации расходов.";
    }

    private void showChatMessageDialog(AiChatHistory chat) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
        builder.setTitle("Вопрос");
        builder.setMessage(chat.getQuestion());
        builder.setPositiveButton("Показать ответ", (dialog, which) -> {
            AlertDialog.Builder answerBuilder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
            answerBuilder.setTitle("Ответ ИИ");
            String answer = chat.getAnswer();
            if (answer != null && (answer.equals("...") || answer.equals("🤔 Генерирую ответ..."))) {
                answer = "Ответ еще не загружен";
            }
            answerBuilder.setMessage(answer != null ? answer : "Ответ не сохранен");
            answerBuilder.setPositiveButton("OK", null);

            AlertDialog answerDialog = answerBuilder.show();
            answerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#4CAF50"));
        });
        builder.setNegativeButton("Закрыть", null);

        AlertDialog dialog = builder.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#4CAF50"));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#BDBDBD"));
        }
    }
}