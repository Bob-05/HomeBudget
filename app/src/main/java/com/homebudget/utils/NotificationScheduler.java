package com.homebudget.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.homebudget.database.entities.NotificationSettings;
import com.homebudget.database.repositories.NotificationRepository;
import com.homebudget.receivers.NotificationReceiver;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Планирует уведомление для пользователя
     * Всегда планирует на следующее указанное время
     */
    public static void scheduleForUser(Context context, int userId) {
        scheduleExactForUser(context, userId);
    }

    /**
     * Точное планирование через AlarmManager
     * Всегда планирует на следующее указанное время
     */
    public static boolean scheduleExactForUser(Context context, int userId) {
        // Проверяем разрешение на точные будильники
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarm for user " + userId +
                        " - permission not granted");
                return false;
            }
        }

        executor.execute(() -> {
            try {
                NotificationRepository repo = new NotificationRepository(context);
                NotificationSettings settings = repo.getSettingsByUser(userId);

                if (settings == null || !settings.isEnabled()) {
                    Log.d(TAG, "Notifications disabled for user " + userId);
                    cancelForUser(context, userId);
                    return;
                }

                // Получаем следующее время отправки
                Calendar next = getNextScheduleTime(settings);

                Log.d(TAG, "Scheduling notification for user " + userId +
                        " at " + String.format("%02d:%02d", settings.getHour(), settings.getMinute()) +
                        " (next: " + next.getTime() + ")");

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager == null) {
                    Log.e(TAG, "AlarmManager is null");
                    return;
                }

                Intent intent = new Intent(context, NotificationReceiver.class);
                intent.putExtra("user_id", userId);
                intent.setAction("com.homebudget.NOTIFICATION_" + userId);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        userId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Отменяем старый будильник
                alarmManager.cancel(pendingIntent);

                // Устанавливаем новый
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            next.getTimeInMillis(), pendingIntent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                            next.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP,
                            next.getTimeInMillis(), pendingIntent);
                }

                Log.d(TAG, "Notification scheduled successfully for user " + userId);

            } catch (Exception e) {
                Log.e(TAG, "Error scheduling notification", e);
            }
        });

        return true;
    }

    /**
     * Получает следующее время отправки уведомления
     * ВСЕГДА планирует на следующее указанное время (сегодня, если время ещё не наступило, иначе завтра)
     */
    private static Calendar getNextScheduleTime(NotificationSettings settings) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, settings.getHour());
        next.set(Calendar.MINUTE, settings.getMinute());
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Если время уже прошло сегодня, планируем на завтра
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "Time already passed today, scheduling for tomorrow");
        }

        return next;
    }

    /**
     * Отмена всех уведомлений для пользователя
     */
    public static void cancelForUser(Context context, int userId) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                Intent intent = new Intent(context, NotificationReceiver.class);
                intent.putExtra("user_id", userId);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        userId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntent);
            }
            Log.d(TAG, "Cancelled notifications for user " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notifications for user " + userId, e);
        }
    }

    /**
     * Перепланирование для всех пользователей
     */
    public static void rescheduleForAll(Context context) {
        executor.execute(() -> {
            try {
                NotificationRepository repo = new NotificationRepository(context);
                for (NotificationSettings settings : repo.getAllEnabledSettings()) {
                    scheduleExactForUser(context, settings.getUserId());
                }
                Log.d(TAG, "Rescheduled for all users");
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling for all users", e);
            }
        });
    }

    /**
     * Проверяет, имеет ли приложение разрешение на точные будильники
     */
    public static boolean hasExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    /**
     * Запрашивает разрешение на точные будильники
     */
    public static void requestExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission(context)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}