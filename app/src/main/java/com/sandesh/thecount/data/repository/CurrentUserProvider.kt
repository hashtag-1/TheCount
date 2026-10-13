package com.sandesh.thecount.data.repository

interface CurrentUserProvider {
    val currentUserId: String
}

class LocalUserProvider : CurrentUserProvider {
    override val currentUserId: String = "local"
}