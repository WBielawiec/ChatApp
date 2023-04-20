package com.example.chatapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
class User(val uid: String, val email: String, val number: String, val username: String, val profileImageUrl: String? = null, val description: String? = null, var status: String = "offline", val token: String = ""): Parcelable, Serializable {
    constructor() : this("","","","")
}
