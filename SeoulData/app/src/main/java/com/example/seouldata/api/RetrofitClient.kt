package com.example.seouldata.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

object RetrofitClient {
    private const val BASE_URL = "http://openAPI.seoul.go.kr:8088/"

    val instance: SeoulApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SeoulApiService::class.java)
    }
}

interface SeoulApiService {
    @GET("{key}/json/ListPublicReservationSport/{start}/{end}/")
    suspend fun getFacilities(
        @Path("key") apiKey: String,
        @Path("start") startIndex: Int,
        @Path("end") endIndex: Int
    ): Response<JsonObject>
}
