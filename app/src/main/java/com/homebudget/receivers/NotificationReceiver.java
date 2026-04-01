package com.homebudget.receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.homebudget.R;
import com.homebudget.database.entities.NotificationSettings;
import com.homebudget.database.entities.User;
import com.homebudget.database.repositories.NotificationRepository;
import com.homebudget.database.repositories.ReportRepository;
import com.homebudget.database.repositories.UserRepository;
import com.homebudget.network.AiApiService;
import com.homebudget.utils.NotificationScheduler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "=== NOTIFICATION RECEIVER TRIGGERED ===");

        int userId = intent.getIntExtra("user_id", -1);
        if (userId == -1) {
            Log.e(TAG, "No user_id in intent");
            return;
        }

        Log.d(TAG, "User ID: " + userId);

        executor.execute(() -> {
            try {
                NotificationRepository notificationRepository = new NotificationRepository(context);
                UserRepository userRepository = new UserRepository(context);
                ReportRepository reportRepository = new ReportRepository(context);

                NotificationSettings settings = notificationRepository.getSettingsByUser(userId);
                if (settings == null || !settings.isEnabled()) {
                    Log.d(TAG, "Notifications disabled for user " + userId);
                    return;
                }

                Log.d(TAG, "Settings found - enabled: " + settings.isEnabled() +
                        ", period: " + settings.getPeriod() +
                        ", time: " + settings.getHour() + ":" + settings.getMinute() +
                        ", includeAi: " + settings.isIncludeAiSummary());


                // Проверяем, нужно ли отправлять

                // Временно для теста - всегда отправляем
                boolean shouldSend = true;
                // Закомментируйте проверку периода
                // boolean shouldSend = shouldSendForPeriod(settings);

                Log.d(TAG, "Should send notification: " + shouldSend);

                if (!shouldSend) {
                    Log.d(TAG, "Already sent for period for user " + userId);
                    NotificationScheduler.scheduleExactForUser(context, userId);
                    return;
                }

                // Получаем пользователя
                User user = userRepository.getUserById(userId);
                String userName = user != null ? user.getLogin() : "Пользователь";

                Log.d(TAG, "User name: " + userName);

                // Формируем даты для отчёта
                Calendar now = Calendar.getInstance();
                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(now.getTimeInMillis());
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);

                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(now.getTimeInMillis());
                endCal.set(Calendar.HOUR_OF_DAY, 23);
                endCal.set(Calendar.MINUTE, 59);
                endCal.set(Calendar.SECOND, 59);
                endCal.set(Calendar.MILLISECOND, 999);

                Date startDate;
                Date endDate = endCal.getTime();

                switch (settings.getPeriod()) {
                    case "daily":
                        startDate = startCal.getTime();
                        break;
                    case "weekly":
                        startCal.add(Calendar.DAY_OF_MONTH, -7);
                        startDate = startCal.getTime();
                        break;
                    case "monthly":
                        startCal.add(Calendar.MONTH, -1);
                        startDate = startCal.getTime();
                        break;
                    default:
                        startDate = startCal.getTime();
                }

                Log.d(TAG, "Report period: " + startDate + " to " + endDate);

                ReportRepository.ReportData report = reportRepository.getReport(userId, startDate, endDate);

                Log.d(TAG, "Report - Income: " + report.totalIncome + ", Expense: " + report.totalExpense);

                String title = getTitleForPeriod(settings.getPeriod(), userName);
                String content;

                // Если нужно включить ИИ в уведомление
                if (settings.isIncludeAiSummary()) {
                    Log.d(TAG, "Getting AI analysis for notification...");
                    content = buildNotificationContentWithAi(context, userId, report, settings, userName);
                } else {
                    content = buildNotificationContent(report, settings);
                }

                sendNotification(context, userId, title, content);

                // Обновляем время последней отправки
                notificationRepository.updateLastNotificationSent(userId, System.currentTimeMillis());
                Log.d(TAG, "Last sent time updated");

                // Перепланируем следующее уведомление
                NotificationScheduler.scheduleExactForUser(context, userId);

                Log.d(TAG, "=== NOTIFICATION SENT SUCCESSFULLY ===");

            } catch (Exception e) {
                Log.e(TAG, "Error in NotificationReceiver", e);
            }
        });
    }

    /**
     * Формирует содержимое уведомления с анализом ИИ
     */
    private String buildNotificationContentWithAi(Context context, int userId,
                                                  ReportRepository.ReportData report,
                                                  NotificationSettings settings,
                                                  String userName) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("💰 Доходы: %.2f ₽\n", report.totalIncome));
        sb.append(String.format("💸 Расходы: %.2f ₽\n", report.totalExpense));

        double balance = report.balance;
        if (balance >= 0) {
            sb.append(String.format("✅ Баланс: +%.2f ₽\n\n", balance));
        } else {
            sb.append(String.format("⚠️ Баланс: %.2f ₽\n\n", balance));
        }

        // Получаем анализ от ИИ
        String aiAnalysis = getAiAnalysis(context, userId, report, userName);
        sb.append("🤖 Анализ ИИ:\n");
        sb.append(aiAnalysis);

        return sb.toString();
    }

    /**
     * Получает анализ от ИИ
     */
    private String getAiAnalysis(Context context, int userId, ReportRepository.ReportData report, String userName) {
        try {
            // Создаём промпт для ИИ
            String prompt = buildAiPrompt(report, userName);

            Log.d(TAG, "AI Prompt: " + prompt);

            AiApiService aiService = new AiApiService(context);

            // Используем CountDownLatch для синхронного ожидания
            CountDownLatch latch = new CountDownLatch(1);
            final String[] aiResponse = new String[1];

            aiService.sendMessage(prompt, "", userId, new AiApiService.AiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "AI response received: " + response);
                    aiResponse[0] = response;
                    latch.countDown();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "AI error: " + error);
                    aiResponse[0] = "Не удалось получить анализ ИИ: " + error;
                    latch.countDown();
                }
            });

            // Ждём ответ максимум 5 секунд
            latch.await(5, TimeUnit.SECONDS);

            // Ограничиваем длину для уведомления (максимум 200 символов)
            String analysis = aiResponse[0] != null ? aiResponse[0] : "Анализ временно недоступен";
            if (analysis.length() > 200) {
                analysis = analysis.substring(0, 197) + "...";
            }

            return analysis;

        } catch (Exception e) {
            Log.e(TAG, "Error getting AI analysis", e);
            return "⚠️ Анализ временно недоступен. Попробуйте позже.";
        }
    }

    /**
     * Формирует промпт для ИИ на основе отчёта
     */
    private String buildAiPrompt(ReportRepository.ReportData report, String userName) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Ты — весёлый и остроумный финансовый ассистент в приложении Budget. ");
        prompt.append("Твоя задача — дать краткий, но смешной анализ финансов пользователя.\n\n");

        prompt.append("СТИЛЬ ОТВЕТА:\n");
        prompt.append("✅ Отвечай только простым текстом (Plain Text).\n");
        prompt.append("✅ Используй яркие метафоры, сравнения из жизни, шутки.\n");
        //prompt.append("✅ Будь остроумным и саркастичным, но доброжелательным.\n");
        //prompt.append("✅ Смешно преувеличивай, если траты большие.\n");
        prompt.append("✅ Используй эмодзи: 😂 🤡 🍔 💸 📉 🎢 💀 🏦 💪 🚀\n");
        prompt.append("✅ Ответ должен быть кратким (максимум 3 предложения).\n");
        //prompt.append("✅ Обязательно дай один конкретный совет по улучшению ситуации (тоже в смешной форме).\n");
        prompt.append("✅ Отвечай на русском языке.\n\n");

        prompt.append("ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:\n");
        prompt.append("Имя: ").append(userName).append("\n");
        prompt.append("💰 Доходы: ").append(String.format("%.2f", report.totalIncome)).append(" ₽\n");
        prompt.append("💸 Расходы: ").append(String.format("%.2f", report.totalExpense)).append(" ₽\n");
        prompt.append("📊 Баланс: ").append(String.format("%.2f", report.balance)).append(" ₽\n");
        prompt.append("📝 Всего транзакций: ").append(report.transactions.size()).append("\n\n");

        if (!report.categoryExpenses.isEmpty()) {
            prompt.append("Основные расходы:\n");
            int count = Math.min(3, report.categoryExpenses.size());
            for (int i = 0; i < count; i++) {
                ReportRepository.CategoryExpense ce = report.categoryExpenses.get(i);
                prompt.append(String.format("  • %s: %.2f ₽ (%.0f%%)\n",
                        ce.categoryName, ce.total, ce.percentage));
            }
            prompt.append("\n");
        }

        /*
        // Добавляем забавные комментарии в зависимости от ситуации
        if (report.balance < 0) {
            prompt.append("⚠️ ВНИМАНИЕ: Расходы превышают доходы! Придумай смешное, но полезное предупреждение и совет.\n\n");
        } else if (report.balance > report.totalIncome * 0.3) {
            prompt.append("🎉 ОТЛИЧНО: Высокая норма сбережений! Похвали пользователя с юмором и предложи, как потратить сэкономленное.\n\n");
        } else if (report.totalExpense > report.totalIncome * 0.8) {
            prompt.append("⚠️ ВНИМАНИЕ: Тратится больше 80% доходов! Придумай смешное предостережение и совет по экономии.\n\n");
        }
        */
        //prompt.append("Напиши смешной, остроумный анализ финансов. Используй яркие сравнения и шутки. ");
        //prompt.append("Обязательно дай один конкретный совет по улучшению ситуации (тоже в смешной форме).");

        return prompt.toString();
    }
    /**
     * Проверяет, нужно ли отправлять уведомление в текущем периоде
     */
    private boolean shouldSendForPeriod(NotificationSettings settings) {
        long lastSent = settings.getLastNotificationSent();

        if (lastSent == 0) {
            Log.d(TAG, "First time sending");
            return true;
        }

        Calendar now = Calendar.getInstance();
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastSent);

        switch (settings.getPeriod()) {
            case "daily":
                return !isSameDay(now, last);
            case "weekly":
                return !isSameWeek(now, last);
            case "monthly":
                return !isSameMonth(now, last);
            default:
                return true;
        }
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameMonth(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
    }

    private String getTitleForPeriod(String period, String userName) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        switch (period) {
            case "daily":
                return "📊 Финансовый отчёт за " + dateStr + ", " + userName;
            case "weekly":
                return "📊 Финансовый отчёт за неделю, " + userName;
            case "monthly":
                return "📊 Финансовый отчёт за месяц, " + userName;
            default:
                return "📊 Финансовый отчёт, " + userName;
        }
    }

    private String buildNotificationContent(ReportRepository.ReportData report, NotificationSettings settings) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("💰 Доходы: %.2f ₽\n", report.totalIncome));
        sb.append(String.format("💸 Расходы: %.2f ₽\n", report.totalExpense));

        double balance = report.balance;
        if (balance >= 0) {
            sb.append(String.format("✅ Баланс: +%.2f ₽", balance));
        } else {
            sb.append(String.format("⚠️ Баланс: %.2f ₽", balance));
        }

        if (!report.categoryExpenses.isEmpty() && report.categoryExpenses.size() > 0) {
            sb.append("\n\n📊 Основные расходы:\n");
            int count = Math.min(3, report.categoryExpenses.size());
            for (int i = 0; i < count; i++) {
                ReportRepository.CategoryExpense ce = report.categoryExpenses.get(i);
                sb.append(String.format("  • %s: %.2f ₽ (%.0f%%)\n",
                        ce.categoryName, ce.total, ce.percentage));
            }
        }

        return sb.toString();
    }

    private void sendNotification(Context context, int userId, String title, String content) {
        Log.d(TAG, "=== SENDING NOTIFICATION ===");
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Content: " + content);

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel("budget_channel");
            if (channel == null) {
                channel = new NotificationChannel(
                        "budget_channel",
                        "Уведомления бюджета",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Уведомления о финансовой статистике");
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }

        //int icon = context.getApplicationInfo().icon;
        int icon = R.drawable.logo;  // Вместо getIdentifier
        if (icon == 0) {
            icon = android.R.drawable.ic_dialog_info;
        }

        Notification notification = new NotificationCompat.Builder(context, "budget_channel")
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        notificationManager.notify(userId, notification);
        Log.d(TAG, "Notification posted with ID: " + userId);
    }
}