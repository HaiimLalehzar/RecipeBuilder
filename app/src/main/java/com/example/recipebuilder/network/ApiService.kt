package com.example.recipebuilder.network

import com.example.recipebuilder.ResponseModel.recipeResponse
import com.example.recipebuilder.model.Recipe
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface ApiService {


    @GET("recipes/random")
    suspend fun getRandomRecipe(
        @Query("apiKey") apiKey: String = "d6339f7b440e41efac07448ca05a22eb"
        , @Query("number") number: Int = 10

    ): recipeResponse
}
    val service: ApiService = Retrofit.Builder()
        .baseUrl("https://api.spoonacular.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)


