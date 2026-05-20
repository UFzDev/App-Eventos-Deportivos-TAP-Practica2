package ufz.dev.eventosdeportivos.data.news

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrentsApiService {
    @GET("search")
    fun getLatestSportsNews(
        @Query("category") category: String = "sports",
        @Query("language") language: String = "es",
        @Query("keywords") keywords: String,
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>
}
