package com.example.chatapp.model

import java.sql.Timestamp

class GroupChat(val groupId: String,val groupName: String, val description: String, val creatorId: String, val createDate: Long, val firstImage: String = "", val secondImage: String =  "") {
    constructor() : this("","","","",-1)
}