package com.jonathan.loginfuturo

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import java.time.Duration
import java.util.regex.Pattern

fun Activity.toast(message : CharSequence, duration: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, message, duration).show()

fun EditText.validate(validation : (String) -> Unit) {
    this.addTextChangedListener(object  : TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            validation(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

    })
}

/** Caracteres permitidos en el email**/

 fun Fragment.isValidEmail(email: String) : Boolean {
    val patterns = Patterns.EMAIL_ADDRESS
    return patterns.matcher(email).matches()
}

/** Caracteres permitidos en la password **/

 fun Fragment.isValidPassword(password: String) : Boolean {
    val passwordPattern =  "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    val pattern = Pattern.compile(passwordPattern)
    return pattern.matcher(password).matches()

}

/** Se iguala el confirmPassword al password **/

 fun Fragment.isValidConfirmPassword(password: String, confirmPassword: String) : Boolean {
    return password == confirmPassword
}