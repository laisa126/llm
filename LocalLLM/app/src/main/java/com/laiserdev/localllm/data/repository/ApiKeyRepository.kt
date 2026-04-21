package com.laiserdev.localllm.data.repository

import android.util.Log
import com.laiserdev.localllm.BuildConfig
import com.laiserdev.localllm.data.model.ApiKeyRecord
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiKeyRepository {

    private val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
    }

    // ─── Validate API key from incoming requests ───────────────────────────────

    suspend fun validateKey(key: String): Result<ApiKeyRecord> = withContext(Dispatchers.IO) {
        try {
            val records = supabase.postgrest["api_keys"]
                .select {
                    filter {
                        eq("key", key)
                        eq("is_active", true)
                    }
                }
                .decodeList<ApiKeyRecord>()

            if (records.isEmpty()) {
                Result.failure(Exception("Invalid or inactive API key"))
            } else {
                // Increment usage count
                supabase.postgrest["api_keys"]
                    .update({ set("usage_count", records[0].usageCount + 1) }) {
                        filter { eq("key", key) }
                    }
                Result.success(records[0])
            }
        } catch (e: Exception) {
            Log.e("ApiKeyRepo", "Validation error", e)
            Result.failure(e)
        }
    }

    // ─── Check if any keys exist (for onboarding) ──────────────────────────────

    suspend fun hasAnyKeys(): Boolean = withContext(Dispatchers.IO) {
        try {
            val records = supabase.postgrest["api_keys"].select().decodeList<ApiKeyRecord>()
            records.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
