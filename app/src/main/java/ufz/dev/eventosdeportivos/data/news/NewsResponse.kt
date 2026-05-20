package ufz.dev.eventosdeportivos.data.news

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("news") val news: List<News>?,
    @SerializedName("page") val page: Int?
)
