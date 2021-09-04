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

/** REGULAR EXPRESION
 1. Solo contiene caracteres alfanuméricos , subrayado y punto
 2. El subrayado y el punto no pueden estar al final o al comienzo de un nombre de usuario (por ejemplo, _username/ username_/ .username/ username.).
 3. El subrayado y el punto no pueden estar uno al lado del otro (p user_.name. Ej .).
 4. El subrayado o el punto no se pueden usar varias veces seguidas (por ejemplo, user__name/ user..name).
 5. El número de caracteres debe estar entre 8 y 20.
 **/

fun Fragment.isValidUsername(username: String) : Boolean {
    val usernamePattern = "^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$"
    val pattern = Pattern.compile(usernamePattern)
    return pattern.matcher(username).matches()
}

/** Caracteres permitidos en el email**/

 fun Fragment.isValidEmail(email: String) : Boolean {
    val patterns = Patterns.EMAIL_ADDRESS
    return patterns.matcher(email).matches()
}

fun Fragment.isValidPhoneNumber(phone: String): Boolean {
    val phonePatterns = "^\\+(?:[0-9] ?){6,14}[0-9]\$"
    val pattern = Pattern.compile(phonePatterns)
    return pattern.matcher(phone).matches()
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