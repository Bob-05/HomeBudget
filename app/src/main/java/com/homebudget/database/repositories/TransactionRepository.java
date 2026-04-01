package com.homebudget.database.repositories;

import android.content.Context;
import com.homebudget.database.AppDatabase;
import com.homebudget.database.dao.TransactionDao;
import com.homebudget.database.entities.Transaction;
import java.util.Date;
import java.util.List;

public class TransactionRepository {

    private TransactionDao transactionDao;

    public TransactionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        transactionDao = db.transactionDao();
    }

    public long insertTransaction(Transaction transaction) {
        return transactionDao.insert(transaction);
    }

    public void updateTransaction(Transaction transaction) {
        transactionDao.update(transaction);
    }

    public void deleteTransaction(Transaction transaction) {
        transactionDao.delete(transaction);
    }

    public List<Transaction> getTransactionsByUser(int userId) {
        return transactionDao.getTransactionsByUser(userId);
    }

    public List<Transaction> getRecentTransactions(int userId, int limit) {
        return transactionDao.getRecentTransactions(userId, limit);
    }

    public List<Transaction> getTransactionsByDateRange(int userId, Date startDate, Date endDate) {
        return transactionDao.getTransactionsByDateRange(userId, startDate, endDate);
    }

    public double getTotalByTypeAndDateRange(int userId, String type, Date startDate, Date endDate) {
        Double total = transactionDao.getTotalByTypeAndDateRange(userId, type, startDate, endDate);
        return total != null ? total : 0.0;
    }

    public List<TransactionDao.CategoryTotal> getCategoryTotals(int userId, String type,
                                                                Date startDate, Date endDate) {
        return transactionDao.getCategoryTotals(userId, type, startDate, endDate);
    }

    public void updateTransactionsCategory(int userId, int oldCategoryId, int newCategoryId) {
        transactionDao.updateTransactionsCategory(userId, oldCategoryId, newCategoryId);
    }
}