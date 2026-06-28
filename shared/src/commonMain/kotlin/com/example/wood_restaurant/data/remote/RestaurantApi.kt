package com.example.wood_restaurant.data.remote

import com.example.wood_restaurant.data.remote.dto.PostDto
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

interface RestaurantApi {
    @GET("posts")
    suspend fun getPosts(): List<PostDto>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto
}
