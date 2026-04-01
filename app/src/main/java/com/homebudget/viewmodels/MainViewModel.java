package com.homebudget.viewmodels;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.homebudget.database.entities.*;
import com.homebudget.database.repositories.*;
import com.homebudget.utils.NotificationScheduler;
import com.homebudget.utils.SingleLiveEvent;
import java.util.Date;
import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private UserRepository userRepository;
    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private ReportRepository reportRepository;
    private NotificationRepository notificationRepository;

    private MutableLiveData<String> status = new MutableLiveData<>();
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();

    // Используем SingleLiveEvent
    private SingleLiveEvent<ReportRepository.ReportData> report = new SingleLiveEvent<>();

    private MutableLiveData<String> message = new MutableLiveData<>();

    public MainViewModel(Application application) {
        super(application);
        userRepository = new UserRepository(application);
        transactionRepository = new TransactionRepository(application);
        categoryRepository = new CategoryRepository(application);
        reportRepository = new ReportRepository(application);
        notificationRepository = new NotificationRepository(application);

        status.setValue("Готов к работе");
    }

    public int getCurrentUserId() {
        return userRepository.getCurrentUserId();
    }

    public void loadUser(int userId) {
        new Thread(() -> {
            User user = userRepository.getUserById(userId);
            currentUser.postValue(user);
        }).start();
    }

    public void loadTransactions(int userId) {
        new Thread(() -> {
            List<Transaction> transactionList = transactionRepository.getTransactionsByUser(userId);
            transactions.postValue(transactionList);
        }).start();
    }

    public void loadCategories(int userId) {
        new Thread(() -> {
            List<Category> categoryList = categoryRepository.getCategoriesByUser(userId);
            categories.postValue(categoryList);
        }).start();
    }

    public void loadReport(int userId, Date startDate, Date endDate) {
        new Thread(() -> {
            ReportRepository.ReportData reportData = reportRepository.getReport(userId, startDate, endDate);
            report.postValue(reportData);  // ← используем postValue
        }).start();
    }

    public void loadIncomeReport(int userId, Date startDate, Date endDate) {
        new Thread(() -> {
            ReportRepository.ReportData reportData = reportRepository.getIncomeReport(userId, startDate, endDate);
            report.postValue(reportData);  // ← используем postValue
        }).start();
    }

    public void loadExpenseReport(int userId, Date startDate, Date endDate) {
        new Thread(() -> {
            ReportRepository.ReportData reportData = reportRepository.getExpenseReport(userId, startDate, endDate);
            report.postValue(reportData);  // ← используем postValue
        }).start();
    }

    public void loadSummaryReport(int userId, Date startDate, Date endDate) {
        new Thread(() -> {
            ReportRepository.ReportData reportData = reportRepository.getSummaryReport(userId, startDate, endDate);
            report.postValue(reportData);  // ← используем postValue
        }).start();
    }

    public void loadCategoriesReport(int userId, Date startDate, Date endDate) {
        new Thread(() -> {
            ReportRepository.ReportData reportData = reportRepository.getCategoriesReport(userId, startDate, endDate);
            report.postValue(reportData);  // ← используем postValue
        }).start();
    }

    public void loadTransactionsReport(int userId, Date startDate, Date endDate) {
        new Thread(() -> {
            ReportRepository.ReportData reportData = reportRepository.getTransactionsReport(userId, startDate, endDate);
            report.postValue(reportData);  // ← используем postValue
        }).start();
    }

    public void addTransaction(Transaction transaction) {
        new Thread(() -> {
            long id = transactionRepository.insertTransaction(transaction);
            if (id > 0) {
                message.postValue("Транзакция добавлена");
                loadTransactions(transaction.getUserId());
            } else {
                message.postValue("Ошибка добавления");
            }
        }).start();
    }

    public void updateTransaction(Transaction transaction) {
        new Thread(() -> {
            transactionRepository.updateTransaction(transaction);
            message.postValue("Транзакция обновлена");
            loadTransactions(transaction.getUserId());
        }).start();
    }

    public void deleteTransaction(Transaction transaction) {
        new Thread(() -> {
            transactionRepository.deleteTransaction(transaction);
            message.postValue("Транзакция удалена");
            loadTransactions(transaction.getUserId());
        }).start();
    }

    public void addCategory(Category category) {
        new Thread(() -> {
            long id = categoryRepository.insertCategory(category);
            if (id > 0) {
                message.postValue("Категория добавлена");
                loadCategories(category.getUserId());
            } else {
                message.postValue("Категория с таким названием уже существует");
            }
        }).start();
    }

    public void updateNotificationSettings(NotificationSettings settings) {
        new Thread(() -> {
            try {
                notificationRepository.setEnabled(settings.getUserId(), settings.isEnabled());
                notificationRepository.updateSchedule(
                        settings.getUserId(),
                        settings.getPeriod(),
                        settings.getHour(),
                        settings.getMinute()
                );
                notificationRepository.setIncludeAiSummary(settings.getUserId(), settings.isIncludeAiSummary());

                message.postValue("Настройки уведомлений сохранены");

                if (settings.isEnabled()) {
                    NotificationScheduler.scheduleExactForUser(getApplication(), settings.getUserId());
                } else {
                    NotificationScheduler.cancelForUser(getApplication(), settings.getUserId());
                }

            } catch (Exception e) {
                Log.e("MainViewModel", "Error saving notification settings", e);
                message.postValue("Ошибка сохранения настроек");
            }
        }).start();
    }

    public void updateThemePreference(int userId, String theme) {
        new Thread(() -> {
            if (userId != -1 && theme != null) {
                userRepository.updateThemePreference(userId, theme);
            }
        }).start();
    }

    public void updateCategory(Category category) {
        new Thread(() -> {
            categoryRepository.updateCategory(category);
            message.postValue("Категория обновлена");
            loadCategories(category.getUserId());
            loadTransactions(category.getUserId());
        }).start();
    }

    public void deleteCategory(Category category) {
        new Thread(() -> {
            boolean success = categoryRepository.deleteCategory(category.getId(), category.getUserId());
            if (success) {
                message.postValue("Категория удалена");
            } else {
                message.postValue("Нельзя удалить последнюю категорию или категорию 'Прочее'");
            }
            loadCategories(category.getUserId());
            loadTransactions(category.getUserId());
        }).start();
    }

    public LiveData<String> getStatus() { return status; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<List<Transaction>> getTransactions() { return transactions; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<ReportRepository.ReportData> getReport() { return report; }
    public LiveData<String> getMessage() { return message; }
}