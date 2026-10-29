
package com.example.collegelostandfound

import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.tasks.await

object SupabaseManager {

    private const val SUPABASE_URL =
        "https://dflzoysmhkikdvsfwvux.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_D4Rbeb4vdvsmH7Ruqq3n5Q_qjVQQPTv"


    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Storage)

            accessToken = {
                FirebaseAuth.getInstance()
                    .currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
            }
        }
    }
}
