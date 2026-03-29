package com.example.dogapp.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface GeoApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 25,
        @Query("countrycodes") countrycodes: String? = null,
        @Query("viewbox") viewbox: String? = null,
        @Query("bounded") bounded: Int? = null,
    ): List<NominatimPlaceDto>

    @GET("search")
    suspend fun searchStructured(
        @Query("street") street: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("countrycodes") countrycodes: String? = null,
    ): List<NominatimPlaceDto>
}
