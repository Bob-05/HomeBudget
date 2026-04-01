package com.homebudget.viewmodels;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.homebudget.database.entities.AiChatHistory;
import com.homebudget.database.repositories.AiChatRepository;
import com.homebudget.database.repositories.UserRepository;
import com.homebudget.network.AiApiService;
import java.util.List;

public class AiViewModel extends AndroidViewModel {

    private static final String TAG = "AiViewModel";
    private UserRepository userRepository;
    private AiChatRepository chatRepository;
    private AiApiService apiService;

    private MutableLiveData<List<AiChatHistory>> chatHistory = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AiViewModel(Application application) {
        super(application);
        userRepository = new UserRepository(application);
        chatRepository = new AiChatRepository(application);
        apiService = new AiApiService(application);
    }

    /**
     * Загружает историю чата для пользователя
     */
    public void loadChatHistory(int userId) {
        Log.d(TAG, "loadChatHistory for userId: " + userId);
        new Thread(() -> {
            List<AiChatHistory> history = chatRepository.getChatHistory(userId);
            Log.d(TAG, "Loaded " + (history != null ? history.size() : 0) + " messages");
            chatHistory.postValue(history);
        }).start();
    }

    /**
     * Отправляет сообщение в ИИ и сохраняет диалог
     */
    public void sendMessage(String message) {
        Log.d(TAG, "sendMessage: " + message);
        isLoading.postValue(true);

        int userId = getCurrentUserId();
        if (userId == -1) {
            errorMessage.postValue("Пользователь не авторизован");
            isLoading.postValue(false);
            return;
        }

        // Сохраняем вопрос с временным ответом
        saveChatHistory(userId, message, "🤔 Генерирую ответ...");

        new Thread(() -> {
            apiService.sendMessage(message, "", userId, new AiApiService.AiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "API success, response: " + response);
                    isLoading.postValue(false);
                    updateLastChatResponse(userId, message, response);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "API error: " + error);
                    isLoading.postValue(false);
                    updateLastChatResponse(userId, message, "❌ Ошибка: " + error);
                    errorMessage.postValue(error);
                }
            });
        }).start();
    }

    /**
     * Обновляет ответ в последнем сообщении чата
     * Сделано public для вызова из AiChatActivity
     */
    public void updateLastChatResponse(int userId, String question, String answer) {
        Log.d(TAG, "updateLastChatResponse for question: " + question);
        new Thread(() -> {
            List<AiChatHistory> history = chatRepository.getChatHistory(userId);
            if (history != null && !history.isEmpty()) {
                for (AiChatHistory chat : history) {
                    if (question.equals(chat.getQuestion())) {
                        Log.d(TAG, "Found matching chat, updating answer");
                        chat.setAnswer(answer);
                        chatRepository.updateChat(chat);
                        break;
                    }
                }
            }
            // Перезагружаем историю, чтобы обновить UI
            loadChatHistory(userId);
        }).start();
    }

    /**
     * Сохраняет новый диалог в историю чата
     */
    public void saveChatHistory(int userId, String question, String answer) {
        Log.d(TAG, "saveChatHistory: " + question);
        new Thread(() -> {
            AiChatHistory chat = new AiChatHistory(userId, question, answer);
            long id = chatRepository.saveChat(chat);
            Log.d(TAG, "Saved chat with id: " + id);
            // Перезагружаем историю после сохранения
            loadChatHistory(userId);
        }).start();
    }

    /**
     * Возвращает ID текущего пользователя
     */
    public int getCurrentUserId() {
        return userRepository.getCurrentUserId();
    }

    /**
     * Очищает всю историю чата пользователя
     */
    public void clearChatHistory(int userId) {
        Log.d(TAG, "clearChatHistory for userId: " + userId);
        new Thread(() -> {
            chatRepository.deleteAllUserChats(userId);
            loadChatHistory(userId);
        }).start();
    }

    // Getters для LiveData
    public MutableLiveData<List<AiChatHistory>> getChatHistory() {
        return chatHistory;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }
}