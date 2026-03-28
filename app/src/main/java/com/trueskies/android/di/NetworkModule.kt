package com.trueskies.android.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.trueskies.android.config.ApiConfiguration
import com.trueskies.android.data.remote.api.OpenMeteoApi
import com.trueskies.android.data.remote.api.TrueSkiesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WeatherRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            // Add platform=android query parameter to every request
            val urlWithPlatform = original.url.newBuilder()
                .addQueryParameter("platform", "android")
                .build()
            val request = original.newBuilder()
                .url(urlWithPlatform)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", ApiConfiguration.userAgent)
                .header("X-Platform", "ANDROID")
                .header("X-Client-Version", "1.0.0")
                .header("X-Environment", ApiConfiguration.environment)
                .apply {
                    val key = ApiConfiguration.apiKey
                    if (key.isNotEmpty()) {
                        header("Authorization", "Bearer $key")
                    }
                }
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (ApiConfiguration.isDebug) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(ApiConfiguration.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideTrueSkiesApi(retrofit: Retrofit): TrueSkiesApi {
        return retrofit.create(TrueSkiesApi::class.java)
    }

    @Provides
    @Singleton
    @WeatherRetrofit
    fun provideWeatherRetrofit(): Retrofit {
        val contentType = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenMeteoApi(@WeatherRetrofit retrofit: Retrofit): OpenMeteoApi {
        return retrofit.create(OpenMeteoApi::class.java)
    }
}
