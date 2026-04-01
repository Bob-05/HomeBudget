package com.homebudget.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.homebudget.database.entities.Transaction;
import com.homebudget.database.repositories.TransactionRepository;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AiApiService {

    private static final String TAG = "AiApiService";
    private YandexGptApiService yandexGptApiService;
    private TransactionRepository transactionRepository;
    private Handler mainHandler;

    public interface AiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public AiApiService(Context context) {
        this.yandexGptApiService = new YandexGptApiService();
        this.transactionRepository = new TransactionRepository(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void sendMessage(String message, String financialData, int userId, AiCallback callback) {
        Log.d(TAG, "sendMessage called with message: " + message);
        String fullPrompt = buildPromptWithFinancialData(userId, message);
        Log.d(TAG, "Full prompt built, length: " + fullPrompt.length());

        yandexGptApiService.sendMessage(fullPrompt, new YandexGptApiService.AiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "YandexGPT success response: " + response);
                mainHandler.post(() -> callback.onSuccess(response));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "YandexGPT error: " + error);
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    private String buildPromptWithFinancialData(int userId, String userQuestion) {
        Log.d(TAG, "Building prompt for userId: " + userId);
        StringBuilder prompt = new StringBuilder();

        prompt.append("Ты - финансовый ассистент в приложении Budget. ");
        prompt.append("Помогай пользователю анализировать расходы, давать советы по бюджету, ");
        prompt.append("отвечать на вопросы о финансах. Отвечай кратко, по делу, ");
        prompt.append("используй эмодзи для наглядности. Отвечай на русском языке.\n\n");

        List<Transaction> recentTransactions = transactionRepository.getRecentTransactions(userId, 15);
        Log.d(TAG, "Got " + (recentTransactions != null ? recentTransactions.size() : 0) + " transactions");

        if (recentTransactions != null && !recentTransactions.isEmpty()) {
            prompt.append("Вот данные о моих финансах:\n\n");
            prompt.append("📊 ПОСЛЕДНИЕ ТРАНЗАКЦИИ:\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction t : recentTransactions) {
                String type = "income".equals(t.getType()) ? "💰 Доход" : "💸 Расход";
                prompt.append(String.format("%s: %.2f ₽", type, t.getAmount()));
                if (t.getNote() != null && !t.getNote().isEmpty()) {
                    prompt.append(String.format(" (%s)", t.getNote()));
                }
                prompt.append(String.format(" - %s\n", sdf.format(t.getDateTime())));

                if ("income".equals(t.getType())) {
                    totalIncome += t.getAmount();
                } else {
                    totalExpense += t.getAmount();
                }
            }

            prompt.append(String.format("\n📈 СТАТИСТИКА:\n"));
            prompt.append(String.format("💰 Доходы: %.2f ₽\n", totalIncome));
            prompt.append(String.format("💸 Расходы: %.2f ₽\n", totalExpense));
            prompt.append(String.format("📊 Баланс: %.2f ₽\n", totalIncome - totalExpense));

            if (totalExpense > 0 && totalIncome > 0) {
                double savingsRate = ((totalIncome - totalExpense) / totalIncome) * 100;
                prompt.append(String.format("💪 Норма сбережений: %.1f%%\n", savingsRate));
            }
        } else {
            prompt.append("У меня пока нет транзакций. Я только начинаю пользоваться приложением Budget.\n\n");
        }

        prompt.append("\nМой вопрос: ").append(userQuestion);

        Log.d(TAG, "Final prompt (first 200 chars): " + prompt.substring(0, Math.min(200, prompt.length())));
        return prompt.toString();
    }
}