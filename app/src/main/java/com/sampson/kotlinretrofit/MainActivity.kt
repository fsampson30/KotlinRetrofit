package com.sampson.kotlinretrofit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.TextView
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>
    private lateinit var tvMetric : TextView
    private lateinit var tvDate : TextView
    private lateinit var rdButtonPositive:  RadioButton
    private lateinit var rdButtonNegative: RadioButton
    private lateinit var rdButtonDeath: RadioButton
    private lateinit var rdButtonWeek: RadioButton
    private lateinit var rdButtonMonth: RadioButton
    private lateinit var rdButtonMax: RadioButton
    private lateinit var sparkView: SparkView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMetric = findViewById(R.id.tvMetricLabel)
        tvDate = findViewById(R.id.tvDateLabel)
        rdButtonPositive = findViewById(R.id.rbButtonPositive)
        rdButtonNegative = findViewById(R.id.rdButtonNegative)
        rdButtonDeath = findViewById(R.id.rdButtonDeath)
        rdButtonWeek = findViewById(R.id.rdButtonWeek)
        rdButtonMonth = findViewById(R.id.rbButtonMonth)
        rdButtonMax = findViewById(R.id.rdButtonMax)
        sparkView = findViewById(R.id.graphSpark)

        val gson = GsonBuilder().setDateFormat(getString(R.string.date_format)).create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        apiService.getNationalData().enqueue(object : Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })

        apiService.getStatesData().enqueue(object : Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG, "Update spinner with state names")
            }
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })





    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        val adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        rdButtonPositive.isChecked = true
        rdButtonMax.isChecked = true
        updateInfoForDate(dailyData.last())
    }

    private fun updateInfoForDate(covidData: CovidData) {
        tvMetric.text = NumberFormat.getInstance().format(covidData.positiveIncrease)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDate.text = outputDateFormat.format(covidData.dateChecked)

    }
}