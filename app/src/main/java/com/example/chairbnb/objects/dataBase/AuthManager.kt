package com.example.chairbnb.objects.dataBase

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.example.chairbnb.activities.MainActivity
import com.example.chairbnb.activities.security.SignInActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest

object AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    fun signIn(email: String, password: String, activity: Activity) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        Toast.makeText(
                            activity, "Authentication successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        activity.startActivity(Intent(activity, MainActivity::class.java))
                        activity.finish()
                    } else {
                        Toast.makeText(
                            activity, "Please verify your email before logging in.",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                    }
                }
                else Toast.makeText(
                    activity, "Incorrect password, please try again!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun currentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun currentUserUid(): String? {
        return currentUser()?.uid
    }

    fun isUserIn(): Boolean {
        return currentUserUid() != null
    }

    fun register(
        fullName: String, email: String, password: String, activity: Activity,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = currentUser()
                    val userId = currentUserUid()
                    if (user != null && userId != null) {
                        val profileUpdates = userProfileChangeRequest {//update user name
                            displayName = fullName
                        }
                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    //save user data in firestore
                                    FireStoreManager.saveUserData(
                                        userId,
                                        mapOf("fullName" to fullName, "email" to email),
                                        onSuccess = {
                                            // sent notification email
                                            user.sendEmailVerification().addOnSuccessListener {
                                                Toast.makeText(
                                                    activity,
                                                    "A verification email was sent to your mailbox.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                activity.startActivity(
                                                    Intent(
                                                        activity,
                                                        SignInActivity::class.java
                                                    )
                                                )
                                                activity.finish()
                                            }
                                                .addOnFailureListener { e ->
                                                    onError("Failed to send verification email: ${e.message}")
                                                }
                                        },
                                        onFailure = { e ->
                                            onError("Error saving user data: ${e.message}")
                                        }
                                    )
                                } else onError("Error updating profile: ${updateTask.exception?.message}")
                            }
                    } else onError("Error: User ID is empty")
                } else onError("Error: ${task.exception?.message}")
            }
    }

    fun resetPasswordByEmail(
        email: String, activity: Activity,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Failed to send reset email.")
                }
            }
    }
}


