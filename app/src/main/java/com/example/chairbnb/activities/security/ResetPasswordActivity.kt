package com.example.chairbnb.Activities.Security

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chairbnb.Objects.DataBase.AuthManager
import com.example.chairbnb.R

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var resetButton: Button
    private lateinit var emailEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)
        findViews()
        resetButton.setOnClickListener { clickReset() }
    }

    private fun clickReset() {
        val email = emailEditText.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            return
        }
        AuthManager.resetPasswordByEmail(
            email, this,
            onSuccess = {
                Toast.makeText(this, "Email for reset was sent", Toast.LENGTH_LONG).show()
                finish()
            }, onFailure = { })
    }

    private fun findViews() {
        resetButton = findViewById(R.id.resetButton)
        emailEditText = findViewById(R.id.emailResetEditText)
    }
}