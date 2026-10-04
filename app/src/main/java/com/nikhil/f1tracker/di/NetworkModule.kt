package com.nikhil.f1tracker.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nikhil.f1tracker.data.remote.JolpicaApiService
import com.nikhil.f1tracker.data.remote.RequestSpacer
import com.nikhil.f1tracker.data.remote.openf1.OpenF1ApiService
import com.nikhil.f1tracker.data.remote.openmeteo.OpenMeteoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

private const val USER_AGENT = "F1Tracker/1.0 (personal-use Android app)"

// Jolpica allows 4 requests/second.
private val JOLPICA_MIN_REQUEST_INTERVAL: Duration = Duration.ofMillis(250)

// OpenF1's free tier allows 30 requests/minute.
private val OPENF1_MIN_REQUEST_INTERVAL: Duration = Duration.ofSeconds(2)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JolpicaClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenF1Client

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Shared base: User-Agent and logging. Each API derives its own client with its rate limit. */
    @Provides
    @Singleton
    fun provideBaseOkHttpClient(): OkHttpClient {
        val userAgentInterceptor = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @JolpicaClient
    fun provideJolpicaOkHttpClient(base: OkHttpClient): OkHttpClient = base.newBuilder()
        .addInterceptor(RequestSpacer(JOLPICA_MIN_REQUEST_INTERVAL).asInterceptor())
        .build()

    @Provides
    @Singleton
    @OpenF1Client
    fun provideOpenF1OkHttpClient(base: OkHttpClient): OkHttpClient = base.newBuilder()
        .addInterceptor(RequestSpacer(OPENF1_MIN_REQUEST_INTERVAL).asInterceptor())
        .build()

    @Provides
    @Singleton
    fun provideJolpicaApiService(@JolpicaClient client: OkHttpClient, json: Json): JolpicaApiService =
        retrofit(JolpicaApiService.BASE_URL, client, json).create(JolpicaApiService::class.java)

    @Provides
    @Singleton
    fun provideOpenF1ApiService(@OpenF1Client client: OkHttpClient, json: Json): OpenF1ApiService =
        retrofit(OpenF1ApiService.BASE_URL, client, json).create(OpenF1ApiService::class.java)

    @Provides
    @Singleton
    fun provideOpenMeteoApiService(base: OkHttpClient, json: Json): OpenMeteoApiService =
        retrofit(OpenMeteoApiService.BASE_URL, base, json).create(OpenMeteoApiService::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
