package com.example.chatappwithfirebase.models

import java.io.Serializable

data class User(
    val name: String? = null,
    val image: String? = null,
    val email: String? = null,
    val token: String? = null,
): Serializable