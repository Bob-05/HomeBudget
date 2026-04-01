package com.homebudget.database.dao;

import androidx.room.*;
import com.homebudget.database.entities.AiChatHistory;
import java.util.Date;
import java.util.List;

@Dao
public interface AiChatHistoryDao {

    @Insert
    long insert(AiChatHistory chatHistory);

    @Delete
    void delete(AiChatHistory chatHistory);

    @Query("SELECT * FROM ai_chat_history WHERE id = :chatId")
    AiChatHistory getChatById(int chatId);

    // ИСПРАВЛЕНО: ORDER BY created_at ASC (от старых к новым)
    @Query("SELECT * FROM ai_chat_history WHERE user_id = :userId ORDER BY created_at ASC")
    List<AiChatHistory> getChatHistoryByUser(int userId);

    // ИСПРАВЛЕНО: ORDER BY created_at ASC (от старых к новым)
    @Query("SELECT * FROM ai_chat_history WHERE user_id = :userId ORDER BY created_at ASC LIMIT :limit")
    List<AiChatHistory> getRecentChats(int userId, int limit);

    @Query("DELETE FROM ai_chat_history WHERE user_id = :userId")
    void deleteAllUserChats(int userId);

    @Query("DELETE FROM ai_chat_history WHERE created_at < :date")
    void deleteOldChats(Date date);

    @Update
    void update(AiChatHistory chatHistory);
}