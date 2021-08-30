package com.jonathan.loginfuturo

import java.util.*

data class UserInformation(
    var username: String = "",
    var email: String = "",
    var photo: String = "",
    var password: String = "",
    var confirmPassword: String = "",
    var timeStamp: Date = Date())