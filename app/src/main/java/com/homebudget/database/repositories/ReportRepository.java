package com.homebudget.database.repositories;

import android.content.Context;
import android.util.Log;
import com.homebudget.database.dao.TransactionDao;
import com.homebudget.database.entities.Transaction;
import com.homebudget.database.entities.Category;
import java.util.*;

public class ReportRepository {

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;

    public ReportRepository(Context context) {
        transactionRepository = new TransactionRepository(context);
        categoryRepository = new CategoryRepository(context);
    }

    public ReportData getReport(int userId, Date startDate, Date endDate) {
        ReportData report = new ReportData();
        report.startDate = startDate;
        report.endDate = endDate;

        report.totalIncome = transactionRepository.getTotalByTypeAndDateRange(
                userId, "income", startDate, endDate);
        report.totalExpense = transactionRepository.getTotalByTypeAndDateRange(
                userId, "expense", startDate, endDate);
        report.balance = report.totalIncome - report.totalExpense;
        report.transactions = transactionRepository.getTransactionsByDateRange(
                userId, startDate, endDate);

        List<TransactionDao.CategoryTotal> categoryTotals =
                transactionRepository.getCategoryTotals(userId, "expense", startDate, endDate);

        Map<Integer, Category> categoriesMap = new HashMap<>();
        for (Category category : categoryRepository.getCategoriesByUser(userId)) {
            categoriesMap.put(category.getId(), category);
        }

        for (TransactionDao.CategoryTotal ct : categoryTotals) {
            if (ct.total > 0) {
                CategoryExpense ce = new CategoryExpense();
                Category category = categoriesMap.get(ct.category_id);
                if (category != null) {
                    ce.categoryId = ct.category_id;
                    ce.categoryName = category.getName();
                    ce.type = "expense";
                    ce.total = ct.total;
                    ce.percentage = report.totalExpense > 0 ? (ct.total / report.totalExpense) * 100 : 0;
                    report.categoryExpenses.add(ce);
                }
            }
        }

        report.categoryExpenses.sort((a, b) -> Double.compare(b.total, a.total));
        return report;
    }

    public ReportData getIncomeReport(int userId, Date startDate, Date endDate) {
        ReportData report = new ReportData();
        report.startDate = startDate;
        report.endDate = endDate;
        report.reportType = "income";

        List<Transaction> allTransactions = transactionRepository.getTransactionsByDateRange(
                userId, startDate, endDate);

        double totalIncome = 0;
        for (Transaction t : allTransactions) {
            if ("income".equals(t.getType())) {
                report.transactions.add(t);
                totalIncome += t.getAmount();
                Log.d("ReportRepository", "Income: " + t.getAmount() + " - " + t.getNote());
            }
        }
        report.totalIncome = totalIncome;

        Log.d("ReportRepository", "Income report - total: " + totalIncome + ", count: " + report.transactions.size());
        return report;
    }

    public ReportData getExpenseReport(int userId, Date startDate, Date endDate) {
        ReportData report = new ReportData();
        report.startDate = startDate;
        report.endDate = endDate;
        report.reportType = "expense";

        List<Transaction> allTransactions = transactionRepository.getTransactionsByDateRange(
                userId, startDate, endDate);

        double totalExpense = 0;
        for (Transaction t : allTransactions) {
            if ("expense".equals(t.getType())) {
                report.transactions.add(t);
                totalExpense += t.getAmount();
                Log.d("ReportRepository", "Expense: " + t.getAmount() + " - " + t.getNote());
            }
        }
        report.totalExpense = totalExpense;

        Log.d("ReportRepository", "Expense report - total: " + totalExpense + ", count: " + report.transactions.size());
        return report;
    }

    public ReportData getSummaryReport(int userId, Date startDate, Date endDate) {
        ReportData report = new ReportData();
        report.startDate = startDate;
        report.endDate = endDate;
        report.reportType = "summary";

        List<Transaction> allTransactions = transactionRepository.getTransactionsByDateRange(
                userId, startDate, endDate);

        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : allTransactions) {
            if ("income".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        report.totalIncome = totalIncome;
        report.totalExpense = totalExpense;
        report.balance = totalIncome - totalExpense;

        Log.d("ReportRepository", "Summary report - income: " + totalIncome + ", expense: " + totalExpense);
        return report;
    }

    public ReportData getCategoriesReport(int userId, Date startDate, Date endDate) {
        ReportData report = new ReportData();
        report.startDate = startDate;
        report.endDate = endDate;
        report.reportType = "categories";

        // Получаем суммы по категориям расходов
        List<TransactionDao.CategoryTotal> expenseCategoryTotals =
                transactionRepository.getCategoryTotals(userId, "expense", startDate, endDate);

        // Получаем суммы по категориям доходов
        List<TransactionDao.CategoryTotal> incomeCategoryTotals =
                transactionRepository.getCategoryTotals(userId, "income", startDate, endDate);

        // Считаем общую сумму расходов и доходов
        double totalExpense = 0;
        double totalIncome = 0;

        for (TransactionDao.CategoryTotal ct : expenseCategoryTotals) {
            totalExpense += ct.total;
        }
        for (TransactionDao.CategoryTotal ct : incomeCategoryTotals) {
            totalIncome += ct.total;
        }
        report.totalExpense = totalExpense;
        report.totalIncome = totalIncome;
        report.balance = totalIncome - totalExpense;

        // Получаем карту категорий
        Map<Integer, Category> categoriesMap = new HashMap<>();
        for (Category category : categoryRepository.getCategoriesByUser(userId)) {
            categoriesMap.put(category.getId(), category);
        }

        // Формируем список категорий с расходами
        for (TransactionDao.CategoryTotal ct : expenseCategoryTotals) {
            if (ct.total > 0) {
                CategoryExpense ce = new CategoryExpense();
                Category category = categoriesMap.get(ct.category_id);
                if (category != null) {
                    ce.categoryId = ct.category_id;
                    ce.categoryName = category.getName();
                    ce.type = "expense";
                    ce.total = ct.total;
                    ce.percentage = totalExpense > 0 ? (ct.total / totalExpense) * 100 : 0;
                    report.categoryExpenses.add(ce);
                    Log.d("ReportRepository", "Expense Category: " + ce.categoryName + " - " + ce.total);
                }
            }
        }

        // Формируем список категорий с доходами
        for (TransactionDao.CategoryTotal ct : incomeCategoryTotals) {
            if (ct.total > 0) {
                CategoryExpense ce = new CategoryExpense();
                Category category = categoriesMap.get(ct.category_id);
                if (category != null) {
                    ce.categoryId = ct.category_id;
                    ce.categoryName = category.getName();
                    ce.type = "income";
                    ce.total = ct.total;
                    ce.percentage = totalIncome > 0 ? (ct.total / totalIncome) * 100 : 0;
                    report.categoryExpenses.add(ce);
                    Log.d("ReportRepository", "Income Category: " + ce.categoryName + " - " + ce.total);
                }
            }
        }

        // Сортируем: сначала расходы по убыванию суммы, затем доходы по убыванию суммы
        report.categoryExpenses.sort((a, b) -> {
            // Сначала все расходы, потом все доходы
            if (a.type.equals("expense") && b.type.equals("income")) return -1;
            if (a.type.equals("income") && b.type.equals("expense")) return 1;
            // Внутри одного типа сортируем по убыванию суммы
            return Double.compare(b.total, a.total);
        });

        Log.d("ReportRepository", "Categories report - total expense: " + totalExpense +
                ", total income: " + totalIncome + ", categories: " + report.categoryExpenses.size());
        return report;
    }

    public ReportData getTransactionsReport(int userId, Date startDate, Date endDate) {
        ReportData report = new ReportData();
        report.startDate = startDate;
        report.endDate = endDate;
        report.reportType = "period";

        report.transactions = transactionRepository.getTransactionsByDateRange(userId, startDate, endDate);

        for (Transaction t : report.transactions) {
            if ("income".equals(t.getType())) {
                report.totalIncome += t.getAmount();
            } else {
                report.totalExpense += t.getAmount();
            }
        }
        report.balance = report.totalIncome - report.totalExpense;

        Log.d("ReportRepository", "Transactions report - count: " + report.transactions.size() +
                ", income: " + report.totalIncome + ", expense: " + report.totalExpense);

        return report;
    }

    public static class ReportData {
        public Date startDate;
        public Date endDate;
        public double totalIncome;
        public double totalExpense;
        public double balance;
        public List<Transaction> transactions = new ArrayList<>();
        public List<CategoryExpense> categoryExpenses = new ArrayList<>();
        public String reportType;
    }

    public static class CategoryExpense {
        public int categoryId;
        public String categoryName;
        public String type;  // "income" или "expense"
        public double total;
        public double percentage;
    }
}