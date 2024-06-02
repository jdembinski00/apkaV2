package com.example.myapplication

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {
    @GET("pair/{baseCurrency}/{convertedCurrency}")
    fun getConversionRate(
        @Path("baseCurrency") baseCurrency: String,
        @Path("convertedCurrency") convertedCurrency: String
    ): Call<CurrencyResponse>

    @GET("history/{baseCurrency}/{year}/{month}/{day}")
    fun getHistoricalData(
        @Path("baseCurrency") baseCurrency: String,
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Path("day") day: Int
    ): Call<HistoricalDataResponse>
}

data class CurrencyResponse(
    @SerializedName("conversion_rate")
    val conversionRate: Double
)
data class HistoricalDataResponse(
    @SerializedName("conversion_rates")
    val conversionRates: Map<String, Double>
)