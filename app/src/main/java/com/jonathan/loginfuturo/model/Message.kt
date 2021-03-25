package com.jonathan.loginfuturo.model

import java.util.*

data class Message(val authorId : String = "",
                   val message : String = "",
                   val profileImageUrl : String = "",
                   val sendAt : Date = Date()
)