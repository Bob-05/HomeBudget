package com.homebudget.database.repositories;

import android.content.Context;
import com.homebudget.database.AppDatabase;
import com.homebudget.database.dao.AiChatHistoryDao;
import com.homebudget.database.entities.AiChatHistory;
import java.util.List;

public class AiChatRepository {

    private AiChatHistoryDao chatDao;

    public AiChatRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        chatDao = db.aiChatHistoryDao();
    }

    public long saveChat(AiChatHistory chat) {
        return chatDao.insert(chat);
    }

    public void updateChat(AiChatHistory chat) {
        chatDao.update(chat);
    }

    public List<AiChatHistory> getChatHistory(int userId) {
        return chatDao.getChatHistoryByUser(userId);
    }

    public void deleteAllUserChats(int userId) {
        chatDao.deleteAllUserChats(userId);
    }
}