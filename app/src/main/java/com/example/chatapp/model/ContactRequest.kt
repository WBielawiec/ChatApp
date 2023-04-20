package com.example.chatapp.model

class ContactRequest(val fromUserId:String, val toUserId: String, val timestamp: Long, val isRead: Boolean) {
    constructor() : this("","",-1,false)
}