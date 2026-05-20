package com.zoya.ai.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore Structure:
 *
 * accessKeys/{accessKey}
 *   - name: String
 *   - deviceId: String (bound device)
 *   - enabled: Boolean
 *
 * chatHistory/{accessKey}/messages/{messageId}
 *   - text: String
 *   - isUser: Boolean
 *   - timestamp: Long
 *   - source: String ("TEXT" | "VOICE")
 *   - modelName: String
 */
class FirestoreRepo(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreRepo"
    private val HISTORY_24H = 24 * 60 * 60 * 1000L

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun validateAccessKey(key: String): Result<UserData> {
        return try {
            val doc = db.collection("accessKeys").document(key).get().await()
            if (!doc.exists()) return Result.failure(Exception("Invalid access key"))

            val enabled = doc.getBoolean("enabled") ?: false
            if (!enabled) return Result.failure(Exception("Access key is disabled"))

            val boundDevice = doc.getString("deviceId") ?: ""
            val thisDevice = getDeviceId()

            if (boundDevice.isNotEmpty() && boundDevice != thisDevice) {
                return Result.failure(Exception("This key is already used on another device"))
            }

            if (boundDevice.isEmpty()) {
                db.collection("accessKeys").document(key).update("deviceId", thisDevice).await()
            }

            Result.success(
                UserData(
                    name = doc.getString("name") ?: "User",
                    accessKey = key,
                    deviceId = thisDevice,
                    enabled = true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "validate error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Clears the deviceId binding for the given accessKey.
     * Called on logout so that when the app is reinstalled, 
     * the key can be used again on a new device.
     */
    suspend fun clearDeviceBinding(accessKey: String) {
        try {
            db.collection("accessKeys").document(accessKey)
                .update("deviceId", "").await()
            Log.d(TAG, "Device binding cleared for $accessKey")
        } catch (e: Exception) {
            Log.e(TAG, "clear binding error: ${e.message}")
        }
    }

    suspend fun saveMessage(accessKey: String, msg: ChatMessage) {
        try {
            db.collection("chatHistory").document(accessKey)
                .collection("messages").document(msg.id.toString())
                .set(
                    mapOf(
                        "text" to msg.text,
                        "isUser" to msg.isUser,
                        "timestamp" to msg.timestamp,
                        "source" to msg.source.name,
                        "modelName" to msg.modelName
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "save error: ${e.message}")
        }
    }

    suspend fun loadHistory(accessKey: String): List<ChatMessage> {
        return try {
            val snap = db.collection("chatHistory").document(accessKey)
                .collection("messages").orderBy("timestamp").get().await()
            snap.documents.mapNotNull { doc ->
                val sourceName = doc.getString("source") ?: MessageSource.TEXT.name
                val source = runCatching { MessageSource.valueOf(sourceName) }.getOrDefault(MessageSource.TEXT)
                ChatMessage(
                    id = doc.id.toIntOrNull() ?: doc.id.hashCode(),
                    text = doc.getString("text") ?: "",
                    isUser = doc.getBoolean("isUser") ?: true,
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    source = source,
                    modelName = doc.getString("modelName") ?: "Unknown Model"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "load error: ${e.message}")
            emptyList()
        }
    }

    suspend fun clearHistory(accessKey: String) {
        try {
            val snap = db.collection("chatHistory").document(accessKey)
                .collection("messages").get().await()
            for (doc in snap.documents) doc.reference.delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "clear error: ${e.message}")
        }
    }

    suspend fun deleteExpiredMessages(accessKey: String) {
        val cutoff = System.currentTimeMillis() - HISTORY_24H
        try {
            val snap = db.collection("chatHistory").document(accessKey)
                .collection("messages").whereLessThan("timestamp", cutoff).get().await()
            for (doc in snap.documents) doc.reference.delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "expire error: ${e.message}")
        }
    }
}
