package com.example.chatapp.model

class ChatMessage(val id: String, val text: String, val fromId: String, val toId: String, val timestamp: Long, val attachment: Boolean = false, val group: Boolean = false){
    constructor() : this("","","","",-1)
}