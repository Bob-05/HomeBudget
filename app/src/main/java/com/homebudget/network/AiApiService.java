package com.homebudget.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.homebudget.database.entities.Category;
import com.homebudget.database.entities.Transaction;
import com.homebudget.database.dao.TransactionDao;
import com.homebudget.database.repositories.CategoryRepository;
import com.homebudget.database.repositories.TransactionRepository;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiApiService {

    private static final String TAG = "AiApiService";
    private YandexGptApiService yandexGptApiService;
    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private Handler mainHandler;
    private ExecutorService executor;
    private Context context;

    public interface AiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public AiApiService(Context context) {
        this.context = context;
        this.yandexGptApiService = new YandexGptApiService(context);
        this.transactionRepository = new TransactionRepository(context);
        this.categoryRepository = new CategoryRepository(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    // Новый метод с параметрами периода
    public void sendMessage(String message, int userId, Date startDate, Date endDate, String reportType, AiCallback callback) {
        if (!YandexGptApiService.isConfigured()) {
            callback.onError("❌ API-ключ или Folder ID не настроены. Настройте в главном меню.");
            return;
        }

        Log.d(TAG, "sendMessage called with message: " + message);
        Log.d(TAG, "Period: " + startDate + " - " + endDate + ", reportType: " + reportType);

        executor.execute(() -> {
            String fullPrompt = buildPromptWithFinancialData(userId, message, startDate, endDate, reportType);
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
        });
    }

    // Старый метод для обратной совместимости (без периода)
    public void sendMessage(String message, String financialData, int userId, AiCallback callback) {
        // Используем последние 30 дней как период по умолчанию
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        Date startDate = cal.getTime();
        sendMessage(message, userId, startDate, endDate, "summary", callback);
    }

    private String buildPromptWithFinancialData(int userId, String userQuestion,
                                                Date startDate, Date endDate,
                                                String reportType) {
        Log.d(TAG, "Building prompt for userId: " + userId);
        StringBuilder prompt = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        prompt.append("Ты - финансовый ассистент в приложении Budget. ");
        prompt.append("Помогай пользователю анализировать расходы, давать советы по бюджету, ");
        prompt.append("(НЕ ИСПОЛЬЗУЙ Markdown, жирный текст, курсив, списки с * или -, заголовки), ");
        prompt.append("отвечать на вопросы о финансах. Отвечай кратко, по делу, ");
        prompt.append("используй эмодзи для наглядности. Отвечай на русском языке.\n\n");

        prompt.append("📅 ПЕРИОД АНАЛИЗА: ").append(sdf.format(startDate))
                .append(" - ").append(sdf.format(endDate)).append("\n");
        prompt.append("📊 ТИП ОТЧЁТА: ").append(getReportTypeName(reportType)).append("\n\n");

        // Получаем транзакции ЗА ВЫБРАННЫЙ период
        List<Transaction> transactions = transactionRepository.getTransactionsByDateRange(userId, startDate, endDate);
        Log.d(TAG, "Got " + (transactions != null ? transactions.size() : 0) + " transactions for period");

        if (transactions != null && !transactions.isEmpty()) {
            // Считаем статистику за период
            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction t : transactions) {
                if ("income".equals(t.getType())) {
                    totalIncome += t.getAmount();
                } else {
                    totalExpense += t.getAmount();
                }
            }

            prompt.append("📈 ОБЩАЯ СТАТИСТИКА ЗА ПЕРИОД:\n");
            prompt.append(String.format("💰 Доходы: %.2f ₽\n", totalIncome));
            prompt.append(String.format("💸 Расходы: %.2f ₽\n", totalExpense));
            prompt.append(String.format("📊 Баланс: %.2f ₽\n", totalIncome - totalExpense));

            if (totalExpense > 0 && totalIncome > 0) {
                double savingsRate = ((totalIncome - totalExpense) / totalIncome) * 100;
                prompt.append(String.format("💪 Норма сбережений: %.1f%%\n", savingsRate));
            }

            // Получаем категории пользователя
            List<Category> categories = categoryRepository.getCategoriesByUser(userId);

            // Получаем расходы по категориям за период
            List<TransactionDao.CategoryTotal> expenseByCategory =
                    transactionRepository.getCategoryTotals(userId, "expense", startDate, endDate);

            if (!expenseByCategory.isEmpty()) {
                prompt.append("\n📂 РАСХОДЫ ПО КАТЕГОРИЯМ:\n");
                // Сортируем по убыванию
                expenseByCategory.sort((a, b) -> Double.compare(b.total, a.total));
                int count = Math.min(5, expenseByCategory.size());
                for (int i = 0; i < count; i++) {
                    TransactionDao.CategoryTotal ct = expenseByCategory.get(i);
                    String categoryName = getCategoryName(categories, ct.category_id);
                    double percentage = totalExpense > 0 ? (ct.total / totalExpense) * 100 : 0;
                    prompt.append(String.format("  • %s: %.2f ₽ (%.0f%%)\n", categoryName, ct.total, percentage));
                }
            }

            // Получаем доходы по категориям за период
            List<TransactionDao.CategoryTotal> incomeByCategory =
                    transactionRepository.getCategoryTotals(userId, "income", startDate, endDate);

            if (!incomeByCategory.isEmpty()) {
                prompt.append("\n💰 ДОХОДЫ ПО КАТЕГОРИЯМ:\n");
                incomeByCategory.sort((a, b) -> Double.compare(b.total, a.total));
                int count = Math.min(3, incomeByCategory.size());
                for (int i = 0; i < count; i++) {
                    TransactionDao.CategoryTotal ct = incomeByCategory.get(i);
                    String categoryName = getCategoryName(categories, ct.category_id);
                    double percentage = totalIncome > 0 ? (ct.total / totalIncome) * 100 : 0;
                    prompt.append(String.format("  • %s: %.2f ₽ (%.0f%%)\n", categoryName, ct.total, percentage));
                }
            }

            // Последние 5 транзакций для контекста (из выбранного периода)
            int transactionCount = Math.min(5, transactions.size());
            if (transactionCount > 0) {
                prompt.append("\n📝 ПОСЛЕДНИЕ ТРАНЗАКЦИИ ЗА ПЕРИОД:\n");
                for (int i = transactions.size() - transactionCount; i < transactions.size(); i++) {
                    Transaction t = transactions.get(i);
                    String type = "income".equals(t.getType()) ? "💰" : "💸";
                    prompt.append(String.format("%s %.2f ₽", type, t.getAmount()));
                    if (t.getNote() != null && !t.getNote().isEmpty()) {
                        prompt.append(String.format(" (%s)", t.getNote()));
                    }
                    prompt.append(String.format(" - %s\n", sdf.format(t.getDateTime())));
                }
            }
        } else {
            prompt.append("📝 За выбранный период нет транзакций.\n");
        }

        prompt.append("\n❓ Вопрос пользователя: ").append(userQuestion);

        Log.d(TAG, "Final prompt length: " + prompt.length() + " chars");
        return prompt.toString();
    }

    private String getCategoryName(List<Category> categories, int categoryId) {
        for (Category cat : categories) {
            if (cat.getId() == categoryId) {
                return cat.getName();
            }
        }
        return "Другое";
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

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}