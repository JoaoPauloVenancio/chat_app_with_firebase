package com.example.chatappwithfirebase.models

import java.io.Serializable

data class User(
    var name: String? = null,
    var image: String? = null,
    var email: String? = null,
    var token: String? = null,
    var id: String? = null,
): Serializable