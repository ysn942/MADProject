package com.example.myapplication.network

data class WeatherResponse(
    val list: List<WeatherItem>
)

data class WeatherItem(
    val name: String,
    val main: WeatherMain,
    val weather: List<WeatherCondition>
)

data class WeatherMain(
    val temp: Double,
    val humidity: Int
)

data class WeatherCondition(
    val description: String,
    val icon: String
)
