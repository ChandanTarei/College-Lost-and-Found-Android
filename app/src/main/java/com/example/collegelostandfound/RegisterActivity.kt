package com.example.collegelostandfound
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val name = findViewById<EditText>(R.id.etName)
        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val confirmPassword =
            findViewById<EditText>(R.id.etConfirmPassword)

        val registerButton =
            findViewById<Button>(R.id.btnRegister)

        val loginText =
            findViewById<TextView>(R.id.tvLogin)

        registerButton.setOnClickListener {

            val studentName = name.text.toString().trim()
            val studentEmail = email.text.toString().trim()
            val studentPassword = password.text.toString()
            val confirm = confirmPassword.text.toString()

            when {
                studentName.isEmpty() ||
                        studentEmail.isEmpty() ||
                        studentPassword.isEmpty() -> {
                    Toast.makeText(
                        this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                studentPassword.length < 6 -> {
                    Toast.makeText(
                        this,
                        "Password must be at least 6 characters",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                studentPassword != confirm -> {
                    Toast.makeText(
                        this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    registerButton.isEnabled = false

                    auth.createUserWithEmailAndPassword(
                        studentEmail,
                        studentPassword
                    ).addOnCompleteListener(this) { task ->

                        registerButton.isEnabled = true

                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Registration Successful!",
                                Toast.LENGTH_LONG
                            ).show()

                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                task.exception?.localizedMessage
                                    ?: "Registration failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        loginText.setOnClickListener {
            finish()
        }
    }
}
