package com.homebudget.workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.homebudget.R;
import com.homebudget.database.entities.NotificationSettings;
import com.homebudget.database.entities.User;
import com.homebudget.database.repositories.NotificationRepository;
import com.homebudget.database.repositories.ReportRepository;
import com.homebudget.database.repositories.UserRepository;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationWorker";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "NotificationWorker started");

        try {
            Context context = getApplicationContext();

            executor.execute(() -> {
                try {
                    processNotifications(context);
                } catch (Exception e) {
                    Log.e(TAG, "Error in notification processing", e);
                }
            });

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error in NotificationWorker", e);
            return Result.failure();
        }
    }

    private void processNotifications(Context context) {
        try {
            NotificationRepository notificationRepository = new NotificationRepository(context);
            UserRepository userRepository = new UserRepository(context);
            ReportRepository reportRepository = new ReportRepository(context);

            List<NotificationSettings> enabledSettings = notificationRepository.getAllEnabledSettings();
            if (enabledSettings.isEmpty()) {
                Log.d(TAG, "No enabled notifications");
                return;
            }

            long now = System.currentTimeMillis();
            Calendar nowCal = Calendar.getInstance();
            nowCal.setTimeInMillis(now);

            int hourNow = nowCal.get(Calendar.HOUR_OF_DAY);
            int minuteNow = nowCal.get(Calendar.MINUTE);

            Log.d(TAG, "Current time: " + hourNow + ":" + minuteNow);
            Log.d(TAG, "Found " + enabledSettings.size() + " enabled notification settings");

            for (NotificationSettings settings : enabledSettings) {
                int targetHour = settings.getHour();
                int targetMinute = settings.getMinute();

                Log.d(TAG, "Checking user " + settings.getUserId() +
                        " - target time: " + targetHour + ":" + targetMinute +
                        ", period: " + settings.getPeriod());

                // Проверяем, наступило ли время отправки
                boolean timeReached = (hourNow > targetHour) ||
                        (hourNow == targetHour && minuteNow >= targetMinute);

                if (!timeReached) {
                    Log.d(TAG, "Time not reached for user " + settings.getUserId());
                    continue;
                }

                Log.d(TAG, "Time reached for user " + settings.getUserId());

                long lastSent = settings.getLastNotificationSent();
                Calendar lastCal = Calendar.getInstance();
                lastCal.setTimeInMillis(lastSent);

                boolean shouldSend = false;

                switch (settings.getPeriod()) {
                    case "daily":
                        shouldSend = !isSameDay(nowCal, lastCal);
                        Log.d(TAG, "Daily check - last sent: " + new Date(lastSent) +
                                ", should send: " + shouldSend);
                        break;
                    case "weekly":
                        shouldSend = !isSameWeek(nowCal, lastCal);
                        Log.d(TAG, "Weekly check - should send: " + shouldSend);
                        break;
                    case "monthly":
                        shouldSend = !isSameMonth(nowCal, lastCal);
                        Log.d(TAG, "Monthly check - should send: " + shouldSend);
                        break;
                }

                // Если никогда не отправляли
                if (lastSent == 0) {
                    shouldSend = true;
                    Log.d(TAG, "First time sending for user " + settings.getUserId());
                }

                if (!shouldSend) {
                    Log.d(TAG, "Already sent for period for user " + settings.getUserId());
                    continue;
                }

                // Получаем пользователя
                User user = userRepository.getUserById(settings.getUserId());
                String userName = user != null ? user.getLogin() : "Пользователь";

                Log.d(TAG, "Sending notification for user: " + userName);

                // Формируем даты для отчёта
                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(now);
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);

                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(now);
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

                // Получаем отчёт
                ReportRepository.ReportData report = reportRepository.getReport(
                        settings.getUserId(), startDate, endDate);

                // Формируем уведомление
                String title = getTitleForPeriod(settings.getPeriod(), userName);
                String content = buildNotificationContent(report, settings);

                sendNotification(settings.getUserId(), title, content);

                // Обновляем время отправки
                notificationRepository.updateLastNotificationSent(settings.getUserId(), now);

                Log.d(TAG, "Notification sent successfully to user " + userName + " (ID: " + settings.getUserId() + ")");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in processNotifications", e);
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

        if (settings.isIncludeAiSummary()) {
            sb.append("\n💡 Для детального анализа откройте приложение.");
        }

        return sb.toString();
    }

    private void sendNotification(int userId, String title, String content) {
        NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel("budget_channel");
            if (channel == null) {
                channel = new NotificationChannel(
                        "budget_channel",
                        "Уведомления бюджета",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Уведомления о финансовой статистике");
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Используем стандартную иконку приложения
        int icon = getApplicationContext().getApplicationInfo().icon;
        if (icon == 0) {
            icon = android.R.drawable.ic_dialog_info;
        }

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), "budget_channel")
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();

        notificationManager.notify(userId, notification);
    }
}