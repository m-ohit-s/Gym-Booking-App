package com.intimetec.gymbookingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.intimetec.gymbookingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.lang.Exception

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener{
            val username = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            loginUser(username, password)
            checkLogin()
        }
    }

    override fun onStart() {
        super.onStart()
        checkLogin()
    }

    fun loginUser(username: String, password: String){
        auth = FirebaseAuth.getInstance()
        val pattern = "@intimetec.com"

        if(username.isNotEmpty() && password.isNotEmpty()) {
            if (username.contains(pattern.toRegex())) {
                try {
                    auth.signInWithEmailAndPassword(username, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Log.d("Auth", "sign In success")
                                val user = auth.currentUser
                                updateUI(user)
                            } else {
                                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                                Log.d("Auth", "Login failed")
                                updateUI(null)
                            }
                        }
                } catch (exception: Exception) {
                    Toast.makeText(this@LoginActivity, exception.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Enter Office Mail Id", Toast.LENGTH_SHORT).show()
            }
        }
        else{
                Toast.makeText(this, "Details Should Not be empty", Toast.LENGTH_SHORT).show()
        }

    }

    private fun checkLogin(){
        auth = FirebaseAuth.getInstance()
        if(auth.currentUser != null){
            Intent(this, BookingActivity::class.java).also {
                startActivity(it)
                finish()
            }
            Toast.makeText(this, "Logging In", Toast.LENGTH_SHORT).show()
        }
        else{
            Log.d("Auth", "no user")
        }
    }

    private fun updateUI(user: FirebaseUser?){
        if(user != null){
            Intent(this, BookingActivity::class.java).also {
                it.putExtra("username", user.email)
                startActivity(it)
            }
        }
        else{
            Toast.makeText(this, "Invalid user or password", Toast.LENGTH_SHORT).show()
            Log.d("Auth", "User Empty")
        }
    }
}