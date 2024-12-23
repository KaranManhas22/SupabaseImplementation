package com.karan.supabaseimplementation

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

class MyApplication : Application() {
    lateinit var supabaseClient: SupabaseClient
    override fun onCreate() {
        super.onCreate()
        supabaseClient = createSupabaseClient(
            "https://nbkuxhqvpppsakymdhpj.supabase.co",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ia3V4aHF2cHBwc2FreW1kaHBqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzQ1OTk0ODIsImV4cCI6MjA1MDE3NTQ4Mn0._yo4mlQ8wHPKhcK0_sdxNSl73z0CIZM1lpg0w1cAbik"

        )
        {
            install(Storage)
        }
        val bucket = supabaseClient.storage.from("Test")
    }

}