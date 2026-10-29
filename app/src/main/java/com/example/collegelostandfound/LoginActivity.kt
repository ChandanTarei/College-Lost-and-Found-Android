package com.example.collegelostandfound
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.etLoginEmail)
        val password = findViewById<EditText>(R.id.etLoginPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerText = findViewById<TextView>(R.id.tvRegister)
        val passwordEye = findViewById<ImageView>(R.id.ivTogglePassword)

        // Password is hidden by default.
        password.transformationMethod =
            PasswordTransformationMethod.getInstance()

        var isPasswordVisible = false

        passwordEye.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

                passwordEye.setImageResource(
                    R.drawable.ic_visibility_off
                )
                passwordEye.contentDescription = "Hide password"
            } else {
                password.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                passwordEye.setImageResource(
                    R.drawable.ic_visibility
                )
                passwordEye.contentDescription = "Show password"
            }

            // Keep cursor at the end of the password.
            password.setSelection(password.text.length)
        }

        // Existing Firebase Login functionality
        loginButton.setOnClickListener {

            val studentEmail = email.text.toString().trim()
            val studentPassword = password.text.toString()

            if (studentEmail.isEmpty() || studentPassword.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter email and password",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(
                studentEmail,
                studentPassword
            ).addOnCompleteListener(this) { task ->

                loginButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Login Successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this,
                        DashboardActivity::class.java
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage
                            ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Open Register screen
        registerText.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }
}
