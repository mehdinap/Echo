package com.example.echo.data.remote.api

import com.example.echo.data.remote.dto.*
import retrofit2.http.*

interface EchoApi {

    // ---- auth ----
    @POST("api/auth/register") suspend fun register(@Body body: AuthRequest): AuthResponse
    @POST("api/auth/login") suspend fun login(@Body body: AuthRequest): AuthResponse
    @GET("api/auth/me") suspend fun me(): UserDto
    @POST("api/auth/me/premium") suspend fun buyPremium(): UserDto
    @PATCH("api/auth/me") suspend fun updateProfile(@Body body: ProfileUpdateDto): UserDto

    // ---- catalog ----
    @GET("api/songs") suspend fun songs(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String = "popular",
    ): PageDto<SongDto>

    @GET("api/songs/carousel") suspend fun carousel(): List<SongDto>
    @GET("api/songs/{id}") suspend fun song(@Path("id") id: Long): SongDto

    @GET("api/search") suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDto<SongDto>

    // ---- library ----
    @GET("api/me/likes") suspend fun likes(): List<SongDto>
    @PUT("api/me/likes/{id}") suspend fun like(@Path("id") songId: Long)
    @DELETE("api/me/likes/{id}") suspend fun unlike(@Path("id") songId: Long)
    @GET("api/me/recent") suspend fun recent(): List<SongDto>
    @POST("api/me/recent/{id}") suspend fun markPlayed(@Path("id") songId: Long)

    // ---- playlists ----
    @GET("api/playlists") suspend fun playlists(@Query("kind") kind: String? = null): List<PlaylistDto>
    @GET("api/me/playlists") suspend fun myPlaylists(): List<PlaylistDto>
    @GET("api/playlists/{id}") suspend fun playlist(@Path("id") id: Long): PlaylistDto
    @PUT("api/playlists/{id}/songs/{songId}") suspend fun addToPlaylist(@Path("id") id: Long, @Path("songId") songId: Long)
    @DELETE("api/playlists/{id}/songs/{songId}") suspend fun removeFromPlaylist(@Path("id") id: Long, @Path("songId") songId: Long)

    // ---- social ----
    @GET("api/users") suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDto<UserDto>

    @GET("api/users/{id}") suspend fun user(@Path("id") id: Long): UserDto
    @PUT("api/users/{id}/follow") suspend fun follow(@Path("id") id: Long)
    @DELETE("api/users/{id}/follow") suspend fun unfollow(@Path("id") id: Long)
    @GET("api/me/following") suspend fun following(): List<UserDto>
    @GET("api/artists/top") suspend fun topArtists(): List<ArtistDto>

    // ---- chat ----
    @GET("api/conversations") suspend fun conversations(): List<ConversationDto>
    @POST("api/conversations/{peerId}") suspend fun openConversation(@Path("peerId") peerId: Long): ConversationIdDto
    @GET("api/conversations/{id}/messages") suspend fun messages(
        @Path("id") conversationId: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDto<MessageDto>
}
