package com.homebudget.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.homebudget.database.entities.User;
import com.homebudget.database.repositories.UserRepository;
import com.homebudget.utils.SingleLiveEvent;

public class AuthViewModel extends AndroidViewModel {

    private UserRepository userRepository;
    private SingleLiveEvent<User> loginResult = new SingleLiveEvent<>(); // Используем SingleLiveEvent
    private MutableLiveData<Long> registerResult = new MutableLiveData<>();
    private MutableLiveData<String> securityQuestion = new MutableLiveData<>();
    private MutableLiveData<Boolean> verifyResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> resetResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel(Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    public void login(String login, String password) {
        new Thread(() -> {
            User user = userRepository.login(login, password);
            loginResult.postValue(user); // SingleLiveEvent сработает только один раз
            if (user == null) {
                errorMessage.postValue("Неверный логин или пароль");
            }
        }).start();
    }

    public void register(String login, String email, String password,
                         String securityQuestion, String securityAnswer) {
        new Thread(() -> {
            long result = userRepository.registerUser(login, email, password,
                    securityQuestion, securityAnswer);
            registerResult.postValue(result);
        }).start();
    }

    public void getSecurityQuestion(String login) {
        new Thread(() -> {
            String question = userRepository.getSecurityQuestion(login);
            securityQuestion.postValue(question);
        }).start();
    }

    public void verifySecurityAnswer(String login, String answer) {
        new Thread(() -> {
            boolean isValid = userRepository.verifySecurityAnswer(login, answer);
            verifyResult.postValue(isValid);
            if (!isValid) {
                errorMessage.postValue("Неверный ответ");
            }
        }).start();
    }

    public void resetPassword(String login, String newPassword) {
        new Thread(() -> {
            boolean success = userRepository.resetPassword(login, newPassword);
            resetResult.postValue(success);
        }).start();
    }

    public SingleLiveEvent<User> getLoginResult() { return loginResult; }
    public MutableLiveData<Long> getRegisterResult() { return registerResult; }
    public MutableLiveData<String> getSecurityQuestion() { return securityQuestion; }
    public MutableLiveData<Boolean> getVerifyResult() { return verifyResult; }
    public MutableLiveData<Boolean> getResetResult() { return resetResult; }
    public MutableLiveData<String> getErrorMessage() { return errorMessage; }
}