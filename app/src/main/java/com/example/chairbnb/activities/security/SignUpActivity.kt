package com.example.chairbnb.activities.security

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chairbnb.R
import com.example.chairbnb.objects.dataBase.AuthManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView

class SignUpActivity : AppCompatActivity() {
    private lateinit var registerButton: Button
    private lateinit var userNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var completeTextView: MaterialTextView
    private lateinit var passInfoButton: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        findViews()
        registerButton.setOnClickListener { clickedRegister() }
        passInfoButton.setOnClickListener { passInfo() }
    }

    private fun passInfo() {
        MaterialAlertDialogBuilder(this, R.style.MyCustomDialog).setTitle("Password Requirements")
            .setMessage(
                "The password must be at least 6 characters long"
            ).setIcon(R.drawable.password)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun clickedRegister() {
        completeTextView.visibility = View.INVISIBLE
        val fullName = userNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "You need to fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        AuthManager.register(fullName, email, password, this) { errorMessage ->
            showError(errorMessage)
        }
    }

    private fun showError(message: String) {
        completeTextView.text = message
        completeTextView.visibility = View.VISIBLE
    }


    private fun findViews() {
        registerButton = findViewById(R.id.registerButton)
        userNameEditText = findViewById(R.id.userNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        completeTextView = findViewById(R.id.completeTextView)
        passInfoButton = findViewById(R.id.passInfoButton)
    }
}