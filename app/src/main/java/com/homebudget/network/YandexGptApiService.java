package com.homebudget.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class YandexGptApiService {

    private static final String TAG = "YandexGPT";
    private static final String BASE_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion";

    private static String FOLDER_ID = "";
    private static String API_KEY = "";

    private final OkHttpClient client;
    private final Handler mainHandler;
    private final Gson gson;
    private Context context; // Добавляем context

    public static void setApiKey(String apiKey) {
        API_KEY = apiKey;
        Log.d(TAG, "API Key set: " + (apiKey.isEmpty() ? "empty" : "***" + apiKey.substring(Math.max(0, apiKey.length()-4))));
    }

    public static void setFolderId(String folderId) {
        FOLDER_ID = folderId;
        Log.d(TAG, "Folder ID set: " + (folderId.isEmpty() ? "empty" : folderId));
    }

    public interface AiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    // Новый конструктор с Context
    public YandexGptApiService(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build();

        // Загружаем сохранённые настройки
        loadSettingsFromPrefs();
    }

    // Конструктор без context для обратной совместимости
    public YandexGptApiService() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build();
    }

    // Загружаем настройки из SharedPreferences
    private void loadSettingsFromPrefs() {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("yandex_gpt_prefs", Context.MODE_PRIVATE);
            String apiKey = prefs.getString("api_key", "");
            String folderId = prefs.getString("folder_id", "");

            if (!apiKey.isEmpty()) {
                API_KEY = apiKey;
                Log.d(TAG, "Loaded API Key from prefs");
            }
            if (!folderId.isEmpty()) {
                FOLDER_ID = folderId;
                Log.d(TAG, "Loaded Folder ID from prefs");
            }
        }
    }

    // Метод для принудительной перезагрузки настроек
    public static void reloadSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("yandex_gpt_prefs", Context.MODE_PRIVATE);
        String apiKey = prefs.getString("api_key", "");
        String folderId = prefs.getString("folder_id", "");

        if (!apiKey.isEmpty()) {
            API_KEY = apiKey;
            Log.d(TAG, "Reloaded API Key from prefs");
        }
        if (!folderId.isEmpty()) {
            FOLDER_ID = folderId;
            Log.d(TAG, "Reloaded Folder ID from prefs");
        }

        Log.d(TAG, "Settings reloaded - API Key: " + (API_KEY.isEmpty() ? "empty" : "present") +
                ", Folder ID: " + (FOLDER_ID.isEmpty() ? "empty" : "present"));
    }

    public void sendMessage(String prompt, AiCallback callback) {
        Log.d(TAG, "=== Sending message to YandexGPT ===");

        // Перед отправкой перезагружаем настройки
        if (context != null) {
            reloadSettings(context);
        }

        Log.d(TAG, "Using API Key: " + (API_KEY.isEmpty() ? "NOT SET" : "SET (length: " + API_KEY.length() + ")"));
        Log.d(TAG, "Using Folder ID: " + (FOLDER_ID.isEmpty() ? "NOT SET" : "SET"));

        if (API_KEY.isEmpty() || FOLDER_ID.isEmpty()) {
            String error = "API-ключ или Folder ID не настроены. Настройте в главном меню.";
            Log.e(TAG, error);
            mainHandler.post(() -> callback.onError(error));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String jsonBody = buildRequestBody(prompt);
                    Log.d(TAG, "Request body: " + jsonBody);

                    RequestBody body = RequestBody.create(
                            MediaType.get("application/json"),
                            jsonBody
                    );

                    Request request = new Request.Builder()
                            .url(BASE_URL)
                            .post(body)
                            .addHeader("Authorization", "Api-Key " + API_KEY)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    Log.d(TAG, "Sending request to: " + BASE_URL);

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";

                        Log.d(TAG, "Response code: " + response.code());
                        Log.d(TAG, "Response body: " + responseBody);

                        if (response.isSuccessful()) {
                            parseResponse(responseBody, callback);
                        } else {
                            Log.e(TAG, "HTTP error: " + response.code());
                            final String errorMessage = "HTTP ошибка " + response.code() + ": " + responseBody;
                            mainHandler.post(() -> callback.onError(errorMessage));
                        }
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Network error", e);
                    final String errorMessage = "Сетевая ошибка: " + e.getMessage();
                    mainHandler.post(() -> callback.onError(errorMessage));
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error", e);
                    final String errorMessage = "Ошибка: " + e.getMessage();
                    mainHandler.post(() -> callback.onError(errorMessage));
                }
            }
        }).start();
    }

    private String buildRequestBody(String prompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("modelUri", "gpt://" + FOLDER_ID + "/yandexgpt-lite");
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("maxTokens", 1000);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("text", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);
        requestBody.add("messages", messages);

        return gson.toJson(requestBody);
    }

    private void parseResponse(String responseBody, AiCallback callback) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            Log.d(TAG, "Parsed JSON: " + jsonResponse.toString());

            String answer = null;

            if (jsonResponse.has("result")) {
                JsonObject result = jsonResponse.getAsJsonObject("result");
                if (result.has("alternatives")) {
                    JsonArray alternatives = result.getAsJsonArray("alternatives");
                    if (!alternatives.isEmpty()) {
                        JsonObject alternative = alternatives.get(0).getAsJsonObject();
                        if (alternative.has("message")) {
                            JsonObject messageObj = alternative.getAsJsonObject("message");
                            if (messageObj.has("text")) {
                                answer = messageObj.get("text").getAsString();
                            }
                        }
                    }
                }
            }

            if (answer == null && jsonResponse.has("response")) {
                JsonObject responseObj = jsonResponse.getAsJsonObject("response");
                if (responseObj.has("text")) {
                    answer = responseObj.get("text").getAsString();
                }
            }

            if (answer != null && !answer.isEmpty()) {
                Log.d(TAG, "Successfully parsed answer: " + answer);
                final String finalAnswer = answer;
                mainHandler.post(() -> callback.onSuccess(finalAnswer));
            } else {
                Log.e(TAG, "Could not find answer in response");
                final String errorMessage = "Не удалось извлечь ответ из JSON: " + responseBody;
                mainHandler.post(() -> callback.onError(errorMessage));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            final String errorMessage = "Ошибка парсинга ответа: " + e.getMessage();
            mainHandler.post(() -> callback.onError(errorMessage));
        }
    }

    public static boolean isConfigured() {
        return !API_KEY.isEmpty() && !FOLDER_ID.isEmpty();
    }
}