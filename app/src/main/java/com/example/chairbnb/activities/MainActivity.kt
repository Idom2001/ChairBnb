package com.example.chairbnb.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chairbnb.R
import com.example.chairbnb.activities.bookingManage.ChooseDateActivity
import com.example.chairbnb.activities.bookingManage.ManageBookingsActivity
import com.example.chairbnb.activities.security.SignInActivity
import com.example.chairbnb.objects.dataBase.AuthManager
import com.example.chairbnb.objects.dataBase.BookingStoreManager
import com.example.chairbnb.objects.dataBase.FireStoreManager
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {
    private var userIn: Boolean = false
    private lateinit var orderRoomBtn: Button
    private lateinit var manageBtn: Button
    private lateinit var signBtn: Button
    private lateinit var userNameText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViews()
        refreshUI()
        orderRoomBtn.setOnClickListener { clickOrderRoom() }
        manageBtn.setOnClickListener { clickManageBookings() }
        signBtn.setOnClickListener { clickSign() }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun refreshUI() {
        val user = AuthManager.currentUser()
        val userUid = AuthManager.currentUserUid()
        if (user != null && user.isEmailVerified) {
            userIn = true
            signBtn.text = getString(R.string.sign_out)
            signBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_red))
            userUid?.let {
                FireStoreManager.getUserFullName(
                    it, onSuccess = { fullName ->
                        userNameText.text = getString(R.string.hello_user, fullName)
                    },
                    onFailure = { userNameText.text = getString(R.string.hello_u) })
            }
        } else {
            userIn = false
            signBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green))
            signBtn.text = getString(R.string.sign_in)
            userNameText.text = getString(R.string.hello_g)
            AuthManager.signOut()//sign out if the user is not modified
        }
    }

    private fun findViews() {
        BookingStoreManager.deleteExpiredBookings({}, {})
        orderRoomBtn = findViewById(R.id.order_btn)
        manageBtn = findViewById(R.id.manage_btn)
        signBtn = findViewById(R.id.sign_btn)
        userNameText = findViewById(R.id.UserNameText)

    }

    private fun clickOrderRoom() {
        if (!userIn) {
            Toast.makeText(this, "You must be signed in to book a room.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        } else {
            startActivity(Intent(this, ChooseDateActivity::class.java))
        }
    }

    private fun clickManageBookings() {
        if (!userIn) {
            Toast.makeText(
                this,
                "You must be signed in to manage your bookings.",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        } else {
            startActivity(Intent(this, ManageBookingsActivity::class.java))
        }

    }

    private fun clickSign() {
        if (!userIn) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            AuthManager.signOut()
            Toast.makeText(this, "You disconnected successfully", Toast.LENGTH_SHORT).show()
            refreshUI()
        }
    }
}