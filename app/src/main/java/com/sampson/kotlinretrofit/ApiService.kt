package com.sampson.kotlinretrofit

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {

    @GET("us/daily.json")
    fun getNationalData(): Call<List<CovidData>>

    @GET("states/daily.json")
    fun getStatesData(): Call<List<CovidData>>

}