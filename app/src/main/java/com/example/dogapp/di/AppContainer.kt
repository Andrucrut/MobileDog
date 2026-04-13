package com.example.dogapp.di

import android.content.Context
import com.example.dogapp.BuildConfig
import com.example.dogapp.data.api.ApiService
import com.example.dogapp.data.api.GeoApiService
import com.example.dogapp.data.local.DogPhotoStorage
import com.example.dogapp.data.local.SettingsStore
import com.example.dogapp.data.local.TokenStore
import com.example.dogapp.data.repository.AppRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppContainer {
    /** База API: `BuildConfig.API_BASE_URL` (задаётся в `app/build.gradle.kts`). */
    private val baseUrl: String get() = BuildConfig.API_BASE_URL
    private const val GEO_BASE_URL = "https://nominatim.openstreetmap.org/"

    private fun api(): ApiService {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logger).build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun geoApi(context: Context): GeoApiService {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val headers = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "DogApp/1.0 (Android; ${context.packageName})")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.5")
                .build()
            chain.proceed(req)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(headers)
            .addInterceptor(logger)
            .build()
        return Retrofit.Builder()
            .baseUrl(GEO_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoApiService::class.java)
    }

    fun repository(context: Context): AppRepository = AppRepository(
        api(),
        geoApi(context),
        TokenStore(context),
        SettingsStore(context),
        DogPhotoStorage(context.applicationContext),
    )
}
