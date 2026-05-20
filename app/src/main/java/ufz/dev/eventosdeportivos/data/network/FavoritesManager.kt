package ufz.dev.eventosdeportivos.data.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import ufz.dev.eventosdeportivos.data.sports.FavoriteItem

/**
 * Gestor de persistencia y sincronización de Favoritos en Cloud Firestore.
 */
object FavoritesManager {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    /**
     * Genera un identificador seguro para Firestore. Si el ID contiene diagonales (como una URL de noticia),
     * calcula su hash MD5 para evitar errores de segmentos en Firestore.
     */
    fun getSafeId(rawId: String): String {
        if (rawId.isEmpty()) return ""
        if (!rawId.contains("/")) return rawId
        return try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            val bytes = digest.digest(rawId.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            rawId.replace("/", "_").replace(":", "_").replace(".", "_")
        }
    }

    /**
     * Retorna la referencia al documento del usuario actual.
     */
    private fun getUserDocument(): DocumentReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid)
    }

    /**
     * Retorna si el usuario activo es un usuario registrado (no anónimo ni nulo).
     */
    fun isUserRegistered(): Boolean {
        val user = auth.currentUser
        return user != null && !user.isAnonymous
    }

    /**
     * Guarda o actualiza un favorito en Firestore.
     */
    fun addFavorite(
        item: FavoriteItem,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userDoc = getUserDocument()
        if (userDoc == null) {
            onFailure(Exception("Sesión no iniciada"))
            return
        }

        userDoc.collection("favorites")
            .document(item.id)
            .set(item)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Elimina un favorito de Firestore por su ID de elemento.
     */
    fun removeFavorite(
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userDoc = getUserDocument()
        if (userDoc == null) {
            onFailure(Exception("Sesión no iniciada"))
            return
        }

        userDoc.collection("favorites")
            .document(itemId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Verifica en tiempo real si un elemento está marcado como favorito.
     */
    fun checkIsFavoritedRealTime(
        itemId: String,
        onResult: (Boolean) -> Unit
    ): ListenerRegistration? {
        val userDoc = getUserDocument() ?: return null
        return try {
            userDoc.collection("favorites")
                .document(itemId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FavoritesManager", "Error en checkIsFavoritedRealTime para $itemId: ${error.message}", error)
                        onResult(false)
                        return@addSnapshotListener
                    }
                    onResult(snapshot != null && snapshot.exists())
                }
        } catch (e: Exception) {
            android.util.Log.e("FavoritesManager", "Fallo al registrar snapshot listener para $itemId: ${e.message}", e)
            onResult(false)
            null
        }
    }

    /**
     * Registra un escuchador en tiempo real para todos los favoritos de un usuario.
     */
    fun listenToFavorites(
        onUpdate: (List<FavoriteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        val userDoc = getUserDocument() ?: return null
        return try {
            userDoc.collection("favorites")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val favorites = ArrayList<FavoriteItem>()
                        for (doc in snapshot.documents) {
                            try {
                                val item = doc.toObject(FavoriteItem::class.java)
                                if (item != null) {
                                    favorites.add(item)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FavoritesManager", "Error al deserializar favorito individual ${doc.id}: ${e.message}", e)
                                // Fallback a mapeo manual robusto si la estructura de Firestore cambió
                                try {
                                    val item = FavoriteItem(
                                        id = doc.id,
                                        type = doc.getString("type") ?: "",
                                        title = doc.getString("title") ?: "",
                                        subtitle = doc.getString("subtitle") ?: "",
                                        imageUrl = doc.getString("imageUrl") ?: "",
                                        dataJson = doc.getString("dataJson") ?: "",
                                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                    )
                                    favorites.add(item)
                                } catch (e2: Exception) {
                                    android.util.Log.e("FavoritesManager", "Fallo total al parsear favorito manual ${doc.id}: ${e2.message}", e2)
                                }
                            }
                        }
                        onUpdate(favorites)
                    } else {
                        onUpdate(emptyList())
                    }
                }
        } catch (e: Exception) {
            onError(e)
            null
        }
    }
}
