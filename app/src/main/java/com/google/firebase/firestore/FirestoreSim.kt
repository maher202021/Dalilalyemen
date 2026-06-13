package com.google.firebase.firestore

import android.content.Context
import android.util.Log
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

// Simulation of FirebaseFirestoreSettings
class FirebaseFirestoreSettings {
    var isPersistenceEnabled = true
    var cacheSizeBytes = CACHE_SIZE_UNLIMITED

    companion object {
        const val CACHE_SIZE_UNLIMITED = -1L
    }

    class Builder {
        private var settings = FirebaseFirestoreSettings()

        fun setPersistenceEnabled(enabled: Boolean): Builder {
            settings.isPersistenceEnabled = enabled
            return this
        }

        fun setCacheSizeBytes(size: Long): Builder {
            settings.cacheSizeBytes = size
            return this
        }

        fun build(): FirebaseFirestoreSettings {
            return settings
        }
    }
}

// Interface for ListenerRegistration
interface ListenerRegistration {
    fun remove()
}

// Document Snapshot simulation
class DocumentSnapshot(val id: String, private val data: Map<String, Any>?) {
    fun exists(): Boolean = data != null
    fun getString(key: String): String? = data?.get(key) as? String
    fun getDouble(key: String): Double? = data?.get(key) as? Double
    fun getLong(key: String): Long? = (data?.get(key) as? Number)?.toLong()
    fun getBoolean(key: String): Boolean? = data?.get(key) as? Boolean
    fun getData(): Map<String, Any>? = data
}

// Query Snapshot simulation
class QuerySnapshot(val documents: List<DocumentSnapshot>) {
    fun isEmpty(): Boolean = documents.isEmpty()
}

// Network connection monitoring
object NetworkState {
    var isConnected = true
    private val listeners = mutableListOf<(Boolean) -> Unit>()

    fun setConnection(connected: Boolean) {
        if (isConnected != connected) {
            isConnected = connected
            listeners.forEach { it(connected) }
        }
    }

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }
}

// Simulated FirebaseFirestore Engine
class FirebaseFirestore private constructor() {
    var firestoreSettings: FirebaseFirestoreSettings = FirebaseFirestoreSettings()
    
    // In-memory collections data
    private val dataLock = Any()
    private val collections = mutableMapOf<String, MutableMap<String, Map<String, Any>>>()
    private val listeners = mutableMapOf<String, MutableList<(QuerySnapshot?, Exception?) -> Unit>>()

    init {
        // Initialize basic templates
        synchronized(dataLock) {
            collections["service_providers"] = mutableMapOf()
            collections["chats"] = mutableMapOf()
            collections["messages"] = mutableMapOf()
            collections["ad_banners"] = mutableMapOf()
            collections["notification_logs"] = mutableMapOf()
            collections["bookings"] = mutableMapOf()
            collections["settings"] = mutableMapOf()
        }
    }

    companion object {
        private var instance: FirebaseFirestore? = null

        fun getInstance(): FirebaseFirestore {
            synchronized(FirebaseFirestore::class.java) {
                if (instance == null) {
                    instance = FirebaseFirestore()
                }
                return instance!!
            }
        }
    }

    // Helper to load/save offline data to JSON file
    fun configureOfflinePersistence(context: Context) {
        synchronized(dataLock) {
            try {
                val cacheFile = File(context.filesDir, "firestore_cache.json")
                if (cacheFile.exists()) {
                    val jsonStr = cacheFile.readText()
                    val jsonObj = JSONObject(jsonStr)
                    jsonObj.keys().forEach { colKey ->
                        val colJson = jsonObj.getJSONObject(colKey)
                        val colMap = collections.getOrPut(colKey) { mutableMapOf() }
                        colJson.keys().forEach { docKey ->
                            val docJson = colJson.getJSONObject(docKey)
                            val docMap = mutableMapOf<String, Any>()
                            docJson.keys().forEach { fieldKey ->
                                docMap[fieldKey] = docJson.get(fieldKey)
                            }
                            colMap[docKey] = docMap
                        }
                    }
                    Log.d("FirestoreSim", "Offline cache loaded successfully")
                } else {
                    Log.d("FirestoreSim", "No offline cache found, starting fresh")
                }
            } catch (e: Exception) {
                Log.e("FirestoreSim", "Error loading offline cache", e)
            }
        }
    }

    fun saveOfflinePersistence(context: Context) {
        synchronized(dataLock) {
            try {
                val cacheFile = File(context.filesDir, "firestore_cache.json")
                val jsonObj = JSONObject()
                collections.forEach { (colKey, colMap) ->
                    val colJson = JSONObject()
                    colMap.forEach { (docKey, docMap) ->
                        val docJson = JSONObject()
                        docMap.forEach { (fieldKey, value) ->
                            docJson.put(fieldKey, value)
                        }
                        colJson.put(docKey, docJson)
                    }
                    jsonObj.put(colKey, colJson)
                }
                cacheFile.writeText(jsonObj.toString())
                Log.d("FirestoreSim", "Offline cache saved successfully")
            } catch (e: Exception) {
                Log.e("FirestoreSim", "Error saving offline cache", e)
            }
        }
    }

    fun collection(collectionPath: String): CollectionReference {
        return CollectionReference(this, collectionPath)
    }

    // Internal methods for simulated mutations
    internal fun getDocuments(collectionPath: String): List<DocumentSnapshot> {
        return synchronized(dataLock) {
            val colMap = collections[collectionPath] ?: emptyMap()
            colMap.map { DocumentSnapshot(it.key, it.value) }
        }
    }

    internal fun getDocument(collectionPath: String, documentPath: String): DocumentSnapshot {
        return synchronized(dataLock) {
            val colMap = collections[collectionPath]
            val docData = colMap?.get(documentPath)
            DocumentSnapshot(documentPath, docData)
        }
    }

    internal fun setDocument(collectionPath: String, documentPath: String, data: Map<String, Any>): Void? {
        synchronized(dataLock) {
            val colMap = collections.getOrPut(collectionPath) { mutableMapOf() }
            colMap[documentPath] = data
            triggerListeners(collectionPath)
        }
        return null
    }

    internal fun deleteDocument(collectionPath: String, documentPath: String): Void? {
        synchronized(dataLock) {
            val colMap = collections[collectionPath]
            colMap?.remove(documentPath)
            triggerListeners(collectionPath)
        }
        return null
    }

    internal fun triggerListeners(collectionPath: String) {
        val docs = getDocuments(collectionPath)
        val snapshot = QuerySnapshot(docs)
        val list = listeners[collectionPath] ?: return
        // Copy list to avoid concurrent override
        val targets = synchronized(dataLock) { list.toList() }
        targets.forEach { listener ->
            try {
                listener(snapshot, null)
            } catch (e: Exception) {
                Log.e("FirestoreSim", "Listener error", e)
            }
        }
    }

    internal fun addListener(collectionPath: String, listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        val list = listeners.getOrPut(collectionPath) { mutableListOf() }
        synchronized(dataLock) {
            list.add(listener)
        }
        
        // Immediate trigger
        try {
            val docs = getDocuments(collectionPath)
            listener(QuerySnapshot(docs), null)
        } catch (e: Exception) {
            Log.e("FirestoreSim", "Immediate listener trigger error", e)
        }

        return object : ListenerRegistration {
            override fun remove() {
                synchronized(dataLock) {
                    list.remove(listener)
                }
            }
        }
    }

    internal fun clearCollection(collectionPath: String) {
        synchronized(dataLock) {
            collections[collectionPath]?.clear()
            triggerListeners(collectionPath)
        }
    }
}

// Collection Reference
class CollectionReference(private val firestore: FirebaseFirestore, val path: String) {
    fun document(documentPath: String): DocumentReference {
        return DocumentReference(firestore, path, documentPath)
    }

    fun document(): DocumentReference {
        val randomId = java.util.UUID.randomUUID().toString()
        return DocumentReference(firestore, path, randomId)
    }

    fun addSnapshotListener(listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        return firestore.addListener(path, listener)
    }

    fun get(callback: (QuerySnapshot?, Exception?) -> Unit) {
        val documents = firestore.getDocuments(path)
        callback(QuerySnapshot(documents), null)
    }
}

// Document Reference
class DocumentReference(
    private val firestore: FirebaseFirestore,
    val collectionPath: String,
    val id: String
) {
    fun set(data: Map<String, Any>, callback: ((Exception?) -> Unit)? = null) {
        firestore.setDocument(collectionPath, id, data)
        callback?.invoke(null)
    }

    fun update(data: Map<String, Any>, callback: ((Exception?) -> Unit)? = null) {
        val currentDoc = firestore.getDocument(collectionPath, id)
        val currentData = currentDoc.getData() ?: emptyMap()
        val newData = currentData.toMutableMap()
        newData.putAll(data)
        firestore.setDocument(collectionPath, id, newData)
        callback?.invoke(null)
    }

    fun delete(callback: ((Exception?) -> Unit)? = null) {
        firestore.deleteDocument(collectionPath, id)
        callback?.invoke(null)
    }

    fun get(callback: (DocumentSnapshot?, Exception?) -> Unit) {
        val doc = firestore.getDocument(collectionPath, id)
        callback(doc, null)
    }
}
