package com.example.chatappwithfirebase.models

import java.util.*

data class ChatMessage(
    var senderId: String? = null,
    var receiverId: String? = null,
    var message: String? = null,
    var dateTime: String? = null,
    var dateObject: Date? = null,
    var conversionId: String? = null,
    var conversionName: String? = null,
    var conversionImage: String? = null,
)