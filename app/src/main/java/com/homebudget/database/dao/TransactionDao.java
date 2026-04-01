package com.homebudget.database.dao;

import androidx.room.*;
import com.homebudget.database.entities.Transaction;
import java.util.Date;
import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(Transaction transaction);

    @Insert
    List<Long> insertAll(List<Transaction> transactions);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    Transaction getTransactionById(int transactionId);

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY date_time DESC")
    List<Transaction> getTransactionsByUser(int userId);

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY date_time DESC LIMIT :limit")
    List<Transaction> getRecentTransactions(int userId, int limit);

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND date_time BETWEEN :startDate AND :endDate ORDER BY date_time DESC")
    List<Transaction> getTransactionsByDateRange(int userId, Date startDate, Date endDate);

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND category_id = :categoryId ORDER BY date_time DESC")
    List<Transaction> getTransactionsByCategory(int userId, int categoryId);

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND type = :type ORDER BY date_time DESC")
    List<Transaction> getTransactionsByType(int userId, String type);

    @Query("SELECT SUM(amount) FROM transactions WHERE user_id = :userId AND type = :type AND date_time BETWEEN :startDate AND :endDate")
    Double getTotalByTypeAndDateRange(int userId, String type, Date startDate, Date endDate);

    @Query("SELECT category_id, SUM(amount) as total FROM transactions " +
            "WHERE user_id = :userId AND type = :type AND date_time BETWEEN :startDate AND :endDate " +
            "GROUP BY category_id")
    List<CategoryTotal> getCategoryTotals(int userId, String type, Date startDate, Date endDate);

    @Query("DELETE FROM transactions WHERE user_id = :userId")
    void deleteAllUserTransactions(int userId);

    @Query("UPDATE transactions SET category_id = :newCategoryId WHERE user_id = :userId AND category_id = :oldCategoryId")
    void updateTransactionsCategory(int userId, int oldCategoryId, int newCategoryId);

    @Query("SELECT COUNT(*) FROM transactions WHERE user_id = :userId AND date_time BETWEEN :startDate AND :endDate")
    int getTransactionCount(int userId, Date startDate, Date endDate);

    // Исправленный метод - поля соответствуют запросу
    @Query("SELECT category_id as categoryId, SUM(amount) as total FROM transactions " +
            "WHERE user_id = :userId AND type = 'expense' AND date_time BETWEEN :startDate AND :endDate " +
            "GROUP BY category_id ORDER BY total DESC")
    List<CategoryExpense> getExpensesByCategory(int userId, Date startDate, Date endDate);

    class CategoryTotal {
        public int category_id;
        public double total;
    }

    class CategoryExpense {
        public int categoryId;
        public double total;
    }
}