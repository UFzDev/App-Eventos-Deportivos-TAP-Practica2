package ufz.dev.eventosdeportivos.data.news

import com.google.gson.annotations.SerializedName

data class News(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("author") val author: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("category") val category: List<String>?,
    @SerializedName("published") val published: String?
)
