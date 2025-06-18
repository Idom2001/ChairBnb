package com.example.chairbnb.activities.security

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chairbnb.R
import com.example.chairbnb.objects.dataBase.AuthManager
import com.google.android.material.textview.MaterialTextView

class SignInActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var signUpButton: MaterialTextView
    private lateinit var resetPasswordText: MaterialTextView
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        findViews()
        loginButton.setOnClickListener { clickLogIn() }
        signUpButton.setOnClickListener { clickSignUp() }
        resetPasswordText.setOnClickListener { clickResetPassword() }
    }


    private fun clickResetPassword() {
        startActivity(Intent(this, ResetPasswordActivity::class.java))
    }

    private fun clickSignUp() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun findViews() {
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpBtn)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        resetPasswordText = findViewById(R.id.resetPasswordText)
    }

    private fun clickLogIn() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }
        AuthManager.signIn(email, password, this)
    }
}
