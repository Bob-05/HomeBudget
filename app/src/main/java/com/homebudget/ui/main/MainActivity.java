package com.homebudget.ui.main;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.homebudget.R;
import com.homebudget.adapters.TransactionAdapter;
import com.homebudget.database.entities.Category;
import com.homebudget.database.entities.Transaction;
import com.homebudget.database.entities.User;
import com.homebudget.database.repositories.ReportRepository;
import com.homebudget.database.repositories.UserRepository;
import com.homebudget.ui.ai.AiChatActivity;
import com.homebudget.ui.auth.LoginActivity;
import com.homebudget.ui.base.BaseActivity;
import com.homebudget.ui.category.ManageCategoryDialog;
import com.homebudget.ui.reports.ReportDialog;
import com.homebudget.ui.settings.SettingsDialog;
import com.homebudget.ui.splash.SplashActivity;
import com.homebudget.ui.transaction.AddEditTransactionDialog;
import com.homebudget.utils.ChartUtils;
import com.homebudget.utils.NotificationScheduler;
import com.homebudget.utils.PdfExporter;
import com.homebudget.viewmodels.MainViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TextView tvTotalIncome, tvTotalExpense, tvBalance;
    private EditText etSearch;
    private ImageView btnSort, btnFilter;
    private FloatingActionButton fabAdd, fabAi;

    private List<Transaction> allTransactions = new ArrayList<>();
    private String currentSortOrder = "date_desc";
    private ExecutorService executorService;
    private Handler mainHandler;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupViewModel();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();

        loadDataAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Проверяем разрешение на точные будильники и планируем уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                int userId = getCurrentUserId();
                if (userId != -1) {
                    NotificationScheduler.scheduleExactForUser(this, userId);
                }
            }
        }

        int userId = getCurrentUserId();
        if (userId != -1) {
            viewModel.loadTransactions(userId);
            viewModel.loadCategories(userId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void loadDataAsync() {
        executorService.execute(() -> {
            int userId = getCurrentUserId();
            if (userId != -1) {
                User user = userRepository.getUserById(userId);
                if (user != null) {
                    String theme = user.getThemePreference();
                    mainHandler.post(() -> {
                        if (!theme.equals(themeManager.getCurrentTheme())) {
                            themeManager.applyTheme(theme);
                        }
                    });
                }
            }

            mainHandler.post(() -> {
                int currentUserId = getCurrentUserId();
                if (currentUserId != -1) {
                    viewModel.loadTransactions(currentUserId);
                    viewModel.loadCategories(currentUserId);
                }
            });
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvBalance = findViewById(R.id.tv_balance);
        etSearch = findViewById(R.id.et_search);
        btnSort = findViewById(R.id.btn_sort);
        btnFilter = findViewById(R.id.btn_filter);
        fabAdd = findViewById(R.id.fab_add);
        fabAi = findViewById(R.id.fab_ai);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getTransactions().observe(this, transactions -> {
            if (transactions != null) {
                allTransactions = transactions;
                applyFilterAndSort();
                updateStats();
            }
        });

        viewModel.getCategories().observe(this, categories -> {
            if (categories != null && adapter != null) {
                adapter.setCategories(categories);
            }
        });

        viewModel.getReport().observe(this, report -> {
            if (report != null) {
                showReportResult(report.reportType, report);
            }
        });

        viewModel.getMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(
                transaction -> showEditTransactionDialog(transaction),
                transaction -> {
                    AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedCornersDialog)
                            .setTitle("Удаление")
                            .setMessage("Удалить эту транзакцию?")
                            .setPositiveButton("Да", (d, w) -> viewModel.deleteTransaction(transaction))
                            .setNegativeButton("Нет", null)
                            .show();

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#4CAF50"));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#BDBDBD"));
                }
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> showFloatingMenu());
        fabAi.setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        btnSort.setOnClickListener(v -> showSortDialog());
        btnFilter.setOnClickListener(v -> showMenuDialog());
    }

    private void showFloatingMenu() {
        PopupMenu popupMenu = new PopupMenu(this, fabAdd);
        popupMenu.getMenuInflater().inflate(R.menu.floating_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_add_transaction) {
                showAddTransactionDialog();
                return true;
            } else if (itemId == R.id.menu_add_category) {
                showAddCategoryDialog();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showAddTransactionDialog() {
        List<Category> categories = viewModel.getCategories().getValue();
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "Сначала создайте категорию", Toast.LENGTH_SHORT).show();
            return;
        }

        AddEditTransactionDialog dialog = new AddEditTransactionDialog(this,
                categories,
                transaction -> {
                    transaction.setUserId(getCurrentUserId());
                    viewModel.addTransaction(transaction);
                });
        dialog.show();
    }

    private void showEditTransactionDialog(Transaction transaction) {
        List<Category> categories = viewModel.getCategories().getValue();
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "Нет доступных категорий", Toast.LENGTH_SHORT).show();
            return;
        }

        AddEditTransactionDialog dialog = new AddEditTransactionDialog(this,
                categories,
                transaction,
                updatedTransaction -> viewModel.updateTransaction(updatedTransaction));
        dialog.show();
    }

    private void showAddCategoryDialog() {
        ManageCategoryDialog dialog = new ManageCategoryDialog(this,
                getCurrentUserId(),
                viewModel.getCategories().getValue(),
                category -> viewModel.addCategory(category));
        dialog.show();
    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
        builder.setTitle("Сортировка");
        String[] sortOptions = {"По дате (новые)", "По дате (старые)",
                "По сумме (возраст)", "По сумме (убыв)"};
        int checkedItem = getSortIndex(currentSortOrder);

        builder.setSingleChoiceItems(sortOptions, checkedItem, (dialog, which) -> {
            switch (which) {
                case 0:
                    currentSortOrder = "date_desc";
                    break;
                case 1:
                    currentSortOrder = "date_asc";
                    break;
                case 2:
                    currentSortOrder = "amount_asc";
                    break;
                case 3:
                    currentSortOrder = "amount_desc";
                    break;
            }
            applyFilterAndSort();
            dialog.dismiss();
        });
        builder.show();
    }

    private void showMenuDialog() {
        String[] menuOptions = {"Финансовый отчет", "Настройки уведомлений",
                "Управление категориями", "Настройки темы", "Выйти из аккаунта"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
        builder.setTitle("Меню");
        builder.setItems(menuOptions, (dialog, which) -> {
            switch (which) {
                case 0:
                    showReportDialog();
                    break;
                case 1:
                    showNotificationSettings();
                    break;
                case 2:
                    showCategoriesManagementDialog();
                    break;
                case 3:
                    showThemeSettings();
                    break;
                case 4:
                    showLogoutDialog();
                    break;
            }
        });
        builder.show();
    }

    private void showCategoriesManagementDialog() {
        List<Category> categories = viewModel.getCategories().getValue();
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "Нет категорий", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] categoryNames = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            categoryNames[i] = categories.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
        builder.setTitle("Управление категориями");
        builder.setItems(categoryNames, (dialog, which) -> {
            Category selected = categories.get(which);
            showEditCategoryDialog(selected);
        });
        builder.setNegativeButton("Закрыть", null);
        builder.show();
    }

    private void showEditCategoryDialog(Category category) {
        List<Category> categories = viewModel.getCategories().getValue();

        ManageCategoryDialog dialog = new ManageCategoryDialog(this,
                getCurrentUserId(),
                categories,
                category,
                updatedCategory -> viewModel.updateCategory(updatedCategory),
                deletedCategory -> viewModel.deleteCategory(deletedCategory));
        dialog.show();
    }

    private void showThemeSettings() {
        String[] themes = {"Светлая", "Темная", "Системная"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
        builder.setTitle("Выберите тему");
        builder.setItems(themes, (dialog, which) -> {
            String theme;
            switch (which) {
                case 0:
                    theme = "light";
                    break;
                case 1:
                    theme = "dark";
                    break;
                default:
                    theme = "system";
                    break;
            }

            int userId = getCurrentUserId();
            if (userId != -1) {
                themeManager.saveAndApplyTheme(userId, theme, () -> {
                    Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            } else {
                themeManager.applyTheme(theme);
            }
        });
        builder.show();
    }

    private void showLogoutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedCornersDialog)
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                .setPositiveButton("Да", (dialogInterface, i) -> {
                    int userId = getCurrentUserId();
                    NotificationScheduler.cancelForUser(this, userId);

                    // Очищаем сессию
                    if (userRepository != null) {
                        userRepository.clearSession();
                    }
                    if (sessionManager != null) {
                        sessionManager.clearSession();
                    }

                    // Применяем системную тему
                    themeManager.applyThemeAfterLogout();

                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Нет", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#BDBDBD"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#4CAF50"));
    }

    private void showReportDialog() {
        ReportDialog dialog = new ReportDialog(this,
                getCurrentUserId(),
                (reportType, startDate, endDate) -> {
                    switch (reportType) {
                        case "income":
                            viewModel.loadIncomeReport(getCurrentUserId(), startDate, endDate);
                            break;
                        case "expense":
                            viewModel.loadExpenseReport(getCurrentUserId(), startDate, endDate);
                            break;
                        case "summary":
                            viewModel.loadSummaryReport(getCurrentUserId(), startDate, endDate);
                            break;
                        case "categories":
                            viewModel.loadCategoriesReport(getCurrentUserId(), startDate, endDate);
                            break;
                        case "period":
                        default:
                            viewModel.loadTransactionsReport(getCurrentUserId(), startDate, endDate);
                            break;
                    }
                });
        dialog.show();
    }

    private void showNotificationSettings() {
        int userId = getCurrentUserId();
        if (userId == -1) return;

        new Thread(() -> {
            com.homebudget.database.repositories.NotificationRepository repo =
                    new com.homebudget.database.repositories.NotificationRepository(this);
            com.homebudget.database.entities.NotificationSettings settings = repo.getSettingsByUser(userId);

            runOnUiThread(() -> {
                SettingsDialog dialog = new SettingsDialog(this, userId, settings,
                        updatedSettings -> viewModel.updateNotificationSettings(updatedSettings));
                dialog.show();
            });
        }).start();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilterAndSort() {
        List<Transaction> filtered = new ArrayList<>(allTransactions);
        String query = etSearch.getText().toString().toLowerCase();
        if (!query.isEmpty()) {
            filtered.removeIf(t -> t.getNote() == null || !t.getNote().toLowerCase().contains(query));
        }

        filtered.sort((t1, t2) -> {
            switch (currentSortOrder) {
                case "date_desc":
                    return t2.getDateTime().compareTo(t1.getDateTime());
                case "date_asc":
                    return t1.getDateTime().compareTo(t2.getDateTime());
                case "amount_desc":
                    return Double.compare(t2.getAmount(), t1.getAmount());
                case "amount_asc":
                    return Double.compare(t1.getAmount(), t2.getAmount());
                default:
                    return t2.getDateTime().compareTo(t1.getDateTime());
            }
        });

        adapter.updateList(filtered);
    }

    private void updateStats() {
        double totalIncome = 0, totalExpense = 0;
        for (Transaction t : allTransactions) {
            if ("income".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }
        tvTotalIncome.setText(String.format("%.2f ₽", totalIncome));
        tvTotalExpense.setText(String.format("%.2f ₽", totalExpense));
        tvBalance.setText(String.format("%.2f ₽", totalIncome - totalExpense));
    }

    private int getSortIndex(String sortOrder) {
        switch (sortOrder) {
            case "date_desc": return 0;
            case "date_asc": return 1;
            case "amount_asc": return 2;
            case "amount_desc": return 3;
            default: return 0;
        }
    }

    private void showReportResult(String reportType, ReportRepository.ReportData report) {
        boolean hasChartData = false;

        if (reportType.equals("categories")) {
            hasChartData = !report.categoryExpenses.isEmpty();
        } else if (reportType.equals("income") && report.totalIncome > 0) {
            hasChartData = !report.transactions.isEmpty();
        } else if (reportType.equals("expense") && report.totalExpense > 0) {
            hasChartData = !report.transactions.isEmpty();
        } else if (reportType.equals("summary") && (report.totalIncome > 0 || report.totalExpense > 0)) {
            hasChartData = true;
        }

        if (hasChartData) {
            showReportWithChart(reportType, report);
        } else {
            showReportTextOnly(reportType, report);
        }
    }

    private void showReportWithChart(String reportType, ReportRepository.ReportData report) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_report_with_chart, null);

        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvReportContent = view.findViewById(R.id.tv_report_content);
        TextView tvChartExpenseTitle = view.findViewById(R.id.tv_chart_expense_title);
        TextView tvChartIncomeTitle = view.findViewById(R.id.tv_chart_income_title);
        PieChart pieChartExpense = view.findViewById(R.id.pie_chart_expense);
        PieChart pieChartIncome = view.findViewById(R.id.pie_chart_income);
        android.widget.Button btnClose = view.findViewById(R.id.btn_close);
        android.widget.Button btnExport = view.findViewById(R.id.btn_export);

        tvTitle.setText(getReportTitle(reportType));

        StringBuilder sb = new StringBuilder();
        sb.append("📅 Период: ").append(android.text.format.DateFormat.format("dd.MM.yyyy", report.startDate))
                .append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy", report.endDate)).append("\n\n");

        switch (reportType) {
            case "income":
                sb.append("📊 ОТЧЕТ ПО ДОХОДАМ\n\n");
                sb.append("💰 Общая сумма доходов: ").append(String.format("%.2f", report.totalIncome)).append(" ₽\n\n");
                if (report.transactions.isEmpty()) {
                    sb.append("Нет доходов за выбранный период\n");
                } else {
                    sb.append("Детализация:\n");
                    for (Transaction t : report.transactions) {
                        if ("income".equals(t.getType())) {
                            sb.append("  • ").append(String.format("%.2f", t.getAmount())).append(" ₽");
                            if (t.getNote() != null && !t.getNote().isEmpty()) {
                                sb.append(" (").append(t.getNote()).append(")");
                            }
                            sb.append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy", t.getDateTime()))
                                    .append("\n");
                        }
                    }
                }

                if (report.totalIncome > 0) {
                    tvChartIncomeTitle.setVisibility(View.VISIBLE);
                    pieChartIncome.setVisibility(View.VISIBLE);
                    List<ReportRepository.CategoryExpense> incomeList = new ArrayList<>();
                    for (Transaction t : report.transactions) {
                        if ("income".equals(t.getType())) {
                            ReportRepository.CategoryExpense ce = new ReportRepository.CategoryExpense();
                            ce.categoryName = t.getNote() != null && !t.getNote().isEmpty() ? t.getNote() : "Доход";
                            ce.total = t.getAmount();
                            ce.type = "income";
                            incomeList.add(ce);
                        }
                    }
                    ChartUtils.setupPieChart(pieChartIncome, incomeList, "income");
                }
                break;

            case "expense":
                sb.append("📊 ОТЧЕТ ПО РАСХОДАМ\n\n");
                sb.append("💸 Общая сумма расходов: ").append(String.format("%.2f", report.totalExpense)).append(" ₽\n\n");
                if (report.transactions.isEmpty()) {
                    sb.append("Нет расходов за выбранный период\n");
                } else {
                    sb.append("Детализация:\n");
                    for (Transaction t : report.transactions) {
                        if ("expense".equals(t.getType())) {
                            sb.append("  • ").append(String.format("%.2f", t.getAmount())).append(" ₽");
                            if (t.getNote() != null && !t.getNote().isEmpty()) {
                                sb.append(" (").append(t.getNote()).append(")");
                            }
                            sb.append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy", t.getDateTime()))
                                    .append("\n");
                        }
                    }
                }

                if (report.totalExpense > 0) {
                    tvChartExpenseTitle.setVisibility(View.VISIBLE);
                    pieChartExpense.setVisibility(View.VISIBLE);
                    List<ReportRepository.CategoryExpense> expenseList = new ArrayList<>();
                    for (Transaction t : report.transactions) {
                        if ("expense".equals(t.getType())) {
                            ReportRepository.CategoryExpense ce = new ReportRepository.CategoryExpense();
                            ce.categoryName = t.getNote() != null && !t.getNote().isEmpty() ? t.getNote() : "Расход";
                            ce.total = t.getAmount();
                            ce.type = "expense";
                            expenseList.add(ce);
                        }
                    }
                    ChartUtils.setupPieChart(pieChartExpense, expenseList, "expense");
                }
                break;

            case "summary":
                sb.append("📊 СВОДНЫЙ ОТЧЕТ\n\n");
                sb.append("💰 Доходы: ").append(String.format("%.2f", report.totalIncome)).append(" ₽\n");
                sb.append("💸 Расходы: ").append(String.format("%.2f", report.totalExpense)).append(" ₽\n");
                if (report.balance >= 0) {
                    sb.append("✅ Баланс: ").append(String.format("%.2f", report.balance)).append(" ₽\n");
                } else {
                    sb.append("⚠️ Баланс: ").append(String.format("%.2f", report.balance)).append(" ₽\n");
                }

                if (report.totalIncome > 0 || report.totalExpense > 0) {
                    tvChartExpenseTitle.setText("Сравнение доходов и расходов");
                    tvChartExpenseTitle.setVisibility(View.VISIBLE);
                    pieChartExpense.setVisibility(View.VISIBLE);

                    List<ReportRepository.CategoryExpense> comparisonList = new ArrayList<>();
                    if (report.totalIncome > 0) {
                        ReportRepository.CategoryExpense ce = new ReportRepository.CategoryExpense();
                        ce.categoryName = "Доходы";
                        ce.total = report.totalIncome;
                        ce.type = "income";
                        comparisonList.add(ce);
                    }
                    if (report.totalExpense > 0) {
                        ReportRepository.CategoryExpense ce = new ReportRepository.CategoryExpense();
                        ce.categoryName = "Расходы";
                        ce.total = report.totalExpense;
                        ce.type = "expense";
                        comparisonList.add(ce);
                    }
                    ChartUtils.setupComparisonPieChart(pieChartExpense, comparisonList);
                }
                break;

            case "categories":
                tvChartExpenseTitle.setText("Расходы по категориям");
                tvChartIncomeTitle.setText("Доходы по категориям");

                if (ChartUtils.hasExpenses(report.categoryExpenses)) {
                    tvChartExpenseTitle.setVisibility(View.VISIBLE);
                    pieChartExpense.setVisibility(View.VISIBLE);
                    ChartUtils.setupPieChart(pieChartExpense, report.categoryExpenses, "expense");
                }

                if (ChartUtils.hasIncomes(report.categoryExpenses)) {
                    tvChartIncomeTitle.setVisibility(View.VISIBLE);
                    pieChartIncome.setVisibility(View.VISIBLE);
                    ChartUtils.setupPieChart(pieChartIncome, report.categoryExpenses, "income");
                }

                if (report.categoryExpenses.isEmpty()) {
                    sb.append("Нет транзакций за выбранный период\n");
                } else {
                    boolean hasExpenses = false;
                    boolean hasIncomes = false;

                    for (ReportRepository.CategoryExpense ce : report.categoryExpenses) {
                        if ("expense".equals(ce.type)) {
                            if (!hasExpenses) {
                                sb.append("💸 РАСХОДЫ\n");
                                hasExpenses = true;
                            }
                            sb.append("  • ").append(ce.categoryName).append(": ");
                            sb.append(String.format("%.2f", ce.total)).append(" ₽");
                            if (report.totalExpense > 0) {
                                sb.append(String.format(" (%.1f%%)", ce.percentage));
                            }
                            sb.append("\n");
                        }
                    }

                    for (ReportRepository.CategoryExpense ce : report.categoryExpenses) {
                        if ("income".equals(ce.type)) {
                            if (!hasIncomes) {
                                if (hasExpenses) sb.append("\n");
                                sb.append("💰 ДОХОДЫ\n");
                                hasIncomes = true;
                            }
                            sb.append("  • ").append(ce.categoryName).append(": ");
                            sb.append(String.format("%.2f", ce.total)).append(" ₽");
                            if (report.totalIncome > 0) {
                                sb.append(String.format(" (%.1f%%)", ce.percentage));
                            }
                            sb.append("\n");
                        }
                    }
                }
                break;

            case "period":
            default:
                sb.append("📊 ТРАНЗАКЦИИ ЗА ПЕРИОД\n\n");
                if (report.transactions.isEmpty()) {
                    sb.append("Нет транзакций за выбранный период\n");
                } else {
                    for (Transaction t : report.transactions) {
                        String typeIcon = "income".equals(t.getType()) ? "💰" : "💸";
                        sb.append("  ").append(typeIcon).append(" ").append(String.format("%.2f", t.getAmount())).append(" ₽");
                        if (t.getNote() != null && !t.getNote().isEmpty()) {
                            sb.append(" (").append(t.getNote()).append(")");
                        }
                        sb.append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", t.getDateTime()))
                                .append("\n");
                    }
                }
                break;
        }

        tvReportContent.setText(sb.toString());

        AlertDialog dialog = builder.setView(view).create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnExport.setOnClickListener(v -> {
            showProgressDialog("Создание PDF...");

            new Thread(() -> {
                File pdfFile = PdfExporter.exportReport(this, reportType, report, report.categoryExpenses);

                runOnUiThread(() -> {
                    dismissProgressDialog();

                    if (pdfFile != null) {
                        AlertDialog dialogA = new AlertDialog.Builder(this, R.style.RoundedCornersDialog)
                                .setTitle("PDF создан")
                                .setMessage("Файл сохранён в:\n" + pdfFile.getAbsolutePath())
                                .setPositiveButton("Открыть", (d, w) -> openPdfFile(pdfFile))
                                .setNegativeButton("OK", null)
                                .create();
                        dialogA.show();

                        dialogA.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#4CAF50"));
                        dialogA.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9E9E9E"));
                    } else {
                        Toast.makeText(this, "Ошибка создания PDF", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
        dialog.show();
    }

    private void showReportTextOnly(String reportType, ReportRepository.ReportData report) {
        StringBuilder sb = new StringBuilder();

        sb.append("📅 Период: ").append(android.text.format.DateFormat.format("dd.MM.yyyy", report.startDate))
                .append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy", report.endDate)).append("\n\n");

        switch (reportType) {
            case "income":
                sb.append("📊 ОТЧЕТ ПО ДОХОДАМ\n\n");
                sb.append("💰 Общая сумма доходов: ").append(String.format("%.2f", report.totalIncome)).append(" ₽\n\n");
                if (report.transactions.isEmpty()) {
                    sb.append("Нет доходов за выбранный период\n");
                } else {
                    sb.append("Детализация:\n");
                    for (Transaction t : report.transactions) {
                        sb.append("  • ").append(String.format("%.2f", t.getAmount())).append(" ₽");
                        if (t.getNote() != null && !t.getNote().isEmpty()) {
                            sb.append(" (").append(t.getNote()).append(")");
                        }
                        sb.append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy", t.getDateTime()))
                                .append("\n");
                    }
                }
                break;

            case "expense":
                sb.append("📊 ОТЧЕТ ПО РАСХОДАМ\n\n");
                sb.append("💸 Общая сумма расходов: ").append(String.format("%.2f", report.totalExpense)).append(" ₽\n\n");
                if (report.transactions.isEmpty()) {
                    sb.append("Нет расходов за выбранный период\n");
                } else {
                    sb.append("Детализация:\n");
                    for (Transaction t : report.transactions) {
                        sb.append("  • ").append(String.format("%.2f", t.getAmount())).append(" ₽");
                        if (t.getNote() != null && !t.getNote().isEmpty()) {
                            sb.append(" (").append(t.getNote()).append(")");
                        }
                        sb.append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy", t.getDateTime()))
                                .append("\n");
                    }
                }
                break;

            case "summary":
                sb.append("📊 СВОДНЫЙ ОТЧЕТ\n\n");
                sb.append("💰 Доходы: ").append(String.format("%.2f", report.totalIncome)).append(" ₽\n");
                sb.append("💸 Расходы: ").append(String.format("%.2f", report.totalExpense)).append(" ₽\n");
                if (report.balance >= 0) {
                    sb.append("✅ Баланс: ").append(String.format("%.2f", report.balance)).append(" ₽\n");
                } else {
                    sb.append("⚠️ Баланс: ").append(String.format("%.2f", report.balance)).append(" ₽\n");
                }
                break;

            case "categories":
                sb.append("📊 ОТЧЕТ ПО КАТЕГОРИЯМ\n\n");
                if (report.categoryExpenses.isEmpty()) {
                    sb.append("Нет транзакций за выбранный период\n");
                } else {
                    boolean hasExpenses = false;
                    boolean hasIncomes = false;

                    for (ReportRepository.CategoryExpense ce : report.categoryExpenses) {
                        if ("expense".equals(ce.type)) {
                            if (!hasExpenses) {
                                sb.append("💸 РАСХОДЫ\n");
                                hasExpenses = true;
                            }
                            sb.append("  • ").append(ce.categoryName).append(": ");
                            sb.append(String.format("%.2f", ce.total)).append(" ₽");
                            if (report.totalExpense > 0) {
                                sb.append(String.format(" (%.1f%%)", ce.percentage));
                            }
                            sb.append("\n");
                        }
                    }

                    for (ReportRepository.CategoryExpense ce : report.categoryExpenses) {
                        if ("income".equals(ce.type)) {
                            if (!hasIncomes) {
                                if (hasExpenses) sb.append("\n");
                                sb.append("💰 ДОХОДЫ\n");
                                hasIncomes = true;
                            }
                            sb.append("  • ").append(ce.categoryName).append(": ");
                            sb.append(String.format("%.2f", ce.total)).append(" ₽");
                            if (report.totalIncome > 0) {
                                sb.append(String.format(" (%.1f%%)", ce.percentage));
                            }
                            sb.append("\n");
                        }
                    }
                }
                break;

            case "period":
            default:
                sb.append("📊 ТРАНЗАКЦИИ ЗА ПЕРИОД\n\n");
                if (report.transactions.isEmpty()) {
                    sb.append("Нет транзакций за выбранный период\n");
                } else {
                    for (Transaction t : report.transactions) {
                        String typeIcon = "income".equals(t.getType()) ? "💰" : "💸";
                        sb.append("  ").append(typeIcon).append(" ").append(String.format("%.2f", t.getAmount())).append(" ₽");
                        if (t.getNote() != null && !t.getNote().isEmpty()) {
                            sb.append(" (").append(t.getNote()).append(")");
                        }
                        sb.append(" - ").append(android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", t.getDateTime()))
                                .append("\n");
                    }
                }
                break;
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedCornersDialog)
                .setTitle(getReportTitle(reportType))
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#4CAF50"));
    }

    private String getReportTitle(String reportType) {
        switch (reportType) {
            case "income": return "📊 Отчет по доходам";
            case "expense": return "📊 Отчет по расходам";
            case "summary": return "📊 Сводный отчет";
            case "categories": return "📊 Отчет по категориям";
            default: return "📊 Отчет по транзакциям";
        }
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void openPdfFile(File pdfFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Открыть PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Не найдено приложение для открытия PDF", Toast.LENGTH_SHORT).show();
        }
    }
}