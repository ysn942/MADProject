package com.example.myapplication.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("find")
    fun getWeather(
        @Query("lat")   lat:    Double,
        @Query("lon")   lon:    Double,
        @Query("cnt")   count:  Int    = 1,
        @Query("APPID") apiKey: String
    ): Call<WeatherResponse>
}
