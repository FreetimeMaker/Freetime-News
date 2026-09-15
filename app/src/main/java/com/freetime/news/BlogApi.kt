package com.freetime.news

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class BlogPostSummary(
    val slug: String,
    val title: String,
    val releaseTimestamp: Long = 0,
    val categories: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BlogPostsResponse(
    val posts: List<BlogPostSummary> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BlogCategoriesResponse(
    val categories: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BlogPost(
    val slug: String,
    val title: String,
    val releaseTimestamp: Long = 0,
    val categories: List<String> = emptyList(),
    val markdown: String = ""
)

interface BlogApi {
    @GET("v2/blog/posts")
    suspend fun getPosts(): BlogPostsResponse

    @GET("v2/blog/categories")
    suspend fun getCategories(): BlogCategoriesResponse

    @GET("v2/blog/posts/{slug}")
    suspend fun getPost(@Path("slug") slug: String): BlogPost
}
