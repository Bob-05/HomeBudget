package com.homebudget.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.homebudget.R;
import com.homebudget.ui.base.BaseActivity;
import com.homebudget.viewmodels.AuthViewModel;

public class ForgotPasswordActivity extends BaseActivity {

    private EditText etLogin, etSecurityAnswer, etNewPassword, etConfirmPassword;
    private TextView tvSecurityQuestion;
    private Button btnGetQuestion, btnResetPassword;
    private AuthViewModel viewModel;

    private String currentLogin;
    private boolean questionLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        setupBackButton();
        initViews();
        setupViewModel();
        setupClickListeners();
    }

    private void initViews() {
        etLogin = findViewById(R.id.et_login);
        tvSecurityQuestion = findViewById(R.id.tv_security_question);
        etSecurityAnswer = findViewById(R.id.et_security_answer);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnGetQuestion = findViewById(R.id.btn_get_question);
        btnResetPassword = findViewById(R.id.btn_reset_password);

        btnResetPassword.setEnabled(false);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        viewModel.getSecurityQuestion().observe(this, question -> {
            if (question != null && !question.isEmpty()) {
                tvSecurityQuestion.setText("Вопрос: " + question);
                questionLoaded = true;
                btnResetPassword.setEnabled(true);
            } else {
                Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getVerifyResult().observe(this, isValid -> {
            if (isValid) {
                String newPassword = etNewPassword.getText().toString();
                String confirmPassword = etConfirmPassword.getText().toString();

                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPassword.length() < 8) {
                    Toast.makeText(this, "Пароль должен быть не менее 8 символов", Toast.LENGTH_SHORT).show();
                    return;
                }

                viewModel.resetPassword(currentLogin, newPassword);
            } else {
                Toast.makeText(this, "Неверный ответ на секретный вопрос", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getResetResult().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Пароль успешно изменен!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Ошибка изменения пароля", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnGetQuestion.setOnClickListener(v -> {
            currentLogin = etLogin.getText().toString().trim();
            if (currentLogin.isEmpty()) {
                etLogin.setError("Введите логин");
                return;
            }
            viewModel.getSecurityQuestion(currentLogin);
        });

        btnResetPassword.setOnClickListener(v -> {
            if (!questionLoaded) {
                Toast.makeText(this, "Сначала получите секретный вопрос", Toast.LENGTH_SHORT).show();
                return;
            }

            String answer = etSecurityAnswer.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString();

            if (answer.isEmpty()) {
                etSecurityAnswer.setError("Введите ответ");
                return;
            }
            if (newPassword.isEmpty()) {
                etNewPassword.setError("Введите новый пароль");
                return;
            }

            viewModel.verifySecurityAnswer(currentLogin, answer);
        });
    }
}