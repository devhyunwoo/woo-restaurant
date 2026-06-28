package com.example.wood_restaurant.data

import com.example.wood_restaurant.data.remote.RestaurantApi
import com.example.wood_restaurant.data.remote.dto.PostDto

class RestaurantRepository(
    private val api: RestaurantApi,
) {
    suspend fun getPosts(): List<PostDto> = api.getPosts()

    suspend fun getPost(id: Int): PostDto = api.getPost(id)
}
