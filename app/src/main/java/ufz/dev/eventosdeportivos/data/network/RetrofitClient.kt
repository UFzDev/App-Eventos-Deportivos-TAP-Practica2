package ufz.dev.eventosdeportivos.data.network

import ufz.dev.eventosdeportivos.BuildConfig
import ufz.dev.eventosdeportivos.data.news.CurrentsApiService
import ufz.dev.eventosdeportivos.data.sports.FootballApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.currentsapi.services/v1/"
    private const val FOOTBALL_BASE_URL = "https://v3.football.api-sports.io/"

    val instance: CurrentsApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(CurrentsApiService::class.java)
    }

    val footballInstance: FootballApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-apisports-key", BuildConfig.API_FOOTBALL_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(FOOTBALL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(FootballApiService::class.java)
    }
}
