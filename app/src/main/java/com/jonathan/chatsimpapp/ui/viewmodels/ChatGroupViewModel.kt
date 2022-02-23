package com.jonathan.chatsimpapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.jonathan.chatsimpapp.data.model.ChatGroupModel
import com.jonathan.chatsimpapp.data.model.providers.ChatGroupProvider
import java.util.*
import kotlin.collections.ArrayList

class ChatGroupViewModel: ViewModel() {

    //Providers
    private val chatGroupProvider = ChatGroupProvider()

    //Models
    val chatGroupModel = ChatGroupModel()

    var ids: ArrayList<String> = ArrayList()
    var idGroup: String = ""
    var idAdmin: String = ""
    var email: String = ""

    private var idNotificationChatGroup: Long? = 0

    fun checkChatGroupExist() {
        chatGroupProvider.getChatGroup(ids).get()
            .addOnSuccessListener { querySnapshot ->
                val size: Int = querySnapshot.size()
                if (size == 0) {
                    createChatGroup()
                } else {
                    idGroup = querySnapshot.documents[0].id
                    idNotificationChatGroup = querySnapshot.documents[0].getLong("idNotification")
                    //TODO implementar updateViewed()
                }
            }
    }

    private fun createChatGroup() {
        val random = Random()
        val sizeIdNotification: Int = random.nextInt(1000000)
        chatGroupModel.setIdGroup(idGroup)
        chatGroupModel.setIdAdmin(idAdmin)
        chatGroupModel.setIdNotification(sizeIdNotification)
        chatGroupModel.setTimeStamp(Date())
        chatGroupModel.setEmail(email)
        idNotificationChatGroup = sizeIdNotification.toLong()

        val idUsers: ArrayList<String> = ArrayList()
        for (i in 0..idUsers.size) {
            idUsers.add(ids.toString())
            chatGroupModel.setIdUsers(idUsers)
        }
        chatGroupProvider.createChatGroup(chatGroupModel)
    }
}