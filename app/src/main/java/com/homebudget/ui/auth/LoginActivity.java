package com.homebudget.ui.auth;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.homebudget.BudgetApplication;
import com.homebudget.R;
import com.homebudget.database.entities.User;
import com.homebudget.database.repositories.UserRepository;
import com.homebudget.ui.main.MainActivity;
import com.homebudget.utils.SessionManager;
import com.homebudget.utils.ThemeManager;
import com.homebudget.viewmodels.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ImageButton btnBack;
    private AuthViewModel viewModel;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);
        sessionManager = SessionManager.getInstance(this);
        themeManager = BudgetApplication.getInstance().getThemeManager();

        initViews();

        TextView tvAgreement = findViewById(R.id.tv_agreement);
        String privacyUrl = getString(R.string.privacy_policy_url);
        String termsUrl = getString(R.string.terms_url);
        String html = String.format(getString(R.string.agreement_text), privacyUrl, termsUrl);
        tvAgreement.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        tvAgreement.setMovementMethod(LinkMovementMethod.getInstance());


        setupViewModel();
        setupClickListeners();
    }

    private void initViews() {
        etLogin = findViewById(R.id.et_login);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Внутри setupViewModel(), после успешного входа:
        viewModel.getLoginResult().observe(this, user -> {
            if (user == null) return;

            Log.d("LoginActivity", "✅ Login successful for user: " + user.getLogin());

            new Thread(() -> {
                User fullUser = userRepository.getUserById(user.getId());
                if (fullUser != null) {
                    userRepository.saveUserId(user.getId());

                    // ИНИЦИАЛИЗИРУЕМ СЕССИЮ!
                    sessionManager.updateLastActivity();
                    Log.d("LoginActivity", "🕐 Session initialized");

                    String theme = fullUser.getThemePreference();
                    runOnUiThread(() -> {
                        themeManager.applyTheme(theme);
                        themeManager.saveLoginState(true);
                    });
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Добро пожаловать, " + user.getLogin() + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }).start();
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (login.isEmpty()) {
                etLogin.setError("Введите логин");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Введите пароль");
                return;
            }

            viewModel.login(login, password);
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}