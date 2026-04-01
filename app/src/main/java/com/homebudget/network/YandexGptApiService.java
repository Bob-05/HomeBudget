package com.homebudget.network;

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

    // ЗАМЕНИТЕ НА ВАШ НОВЫЙ КЛЮЧ
    private static final String FOLDER_ID = "k";
    private static final String API_KEY = ""; // ВСТАВЬТЕ НОВЫЙ КЛЮЧ ЗДЕСЬ

    private final OkHttpClient client;
    private final Handler mainHandler;
    private final Gson gson;

    public interface AiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public YandexGptApiService() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public void sendMessage(String prompt, AiCallback callback) {
        Log.d(TAG, "=== Sending message to YandexGPT ===");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Формируем тело запроса в отдельном методе
                    String jsonBody = buildRequestBody(prompt);
                    Log.d(TAG, "Request body: " + jsonBody);

                    // Используем MediaType.get() для API 26+
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

                    // Используем try-with-resources для Response
                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";

                        Log.d(TAG, "Response code: " + response.code());
                        Log.d(TAG, "Response body: " + responseBody);

                        if (response.isSuccessful()) {
                            parseResponse(responseBody, callback);
                        } else {
                            Log.e(TAG, "HTTP error: " + response.code());
                            final String errorMessage = "HTTP ошибка " + response.code() + ": " + responseBody;
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError(errorMessage);
                                }
                            });
                        }
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Network error", e);
                    final String errorMessage = "Сетевая ошибка: " + e.getMessage();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(errorMessage);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error", e);
                    final String errorMessage = "Ошибка: " + e.getMessage();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(errorMessage);
                        }
                    });
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

            // Альтернативный вариант парсинга (для старых версий API)
            if (answer == null && jsonResponse.has("response")) {
                JsonObject responseObj = jsonResponse.getAsJsonObject("response");
                if (responseObj.has("text")) {
                    answer = responseObj.get("text").getAsString();
                }
            }

            if (answer != null && !answer.isEmpty()) {
                Log.d(TAG, "Successfully parsed answer: " + answer);
                final String finalAnswer = answer;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(finalAnswer);
                    }
                });
            } else {
                Log.e(TAG, "Could not find answer in response");
                final String errorMessage = "Не удалось извлечь ответ из JSON: " + responseBody;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(errorMessage);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            final String errorMessage = "Ошибка парсинга ответа: " + e.getMessage();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(errorMessage);
                }
            });
        }
    }
}