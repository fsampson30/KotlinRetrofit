package com.sampson.kotlinretrofit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
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
private const val ALL_STATES: String = "All States"

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    private lateinit var tvMetric : TickerView
    private lateinit var tvDate : TextView
    private lateinit var rdButtonPositive:  RadioButton
    private lateinit var rdButtonNegative: RadioButton
    private lateinit var rdButtonDeath: RadioButton
    private lateinit var rdButtonWeek: RadioButton
    private lateinit var rdButtonMonth: RadioButton
    private lateinit var rdButtonMax: RadioButton
    private lateinit var sparkView: SparkView
    private lateinit var spinner: Spinner

    private lateinit var rgTimeSelection: RadioGroup
    private lateinit var rgMetricSelection: RadioGroup

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
        rgTimeSelection  = findViewById(R.id.rgTimeSelection)
        rgMetricSelection = findViewById(R.id.rgCasesSelection)
        spinner = findViewById(R.id.spinnerEstates)

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
                setupEventListeners()
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
                updateSpinnerWithStateData(perStateDailyData.keys)
            }
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)

        val arrayAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, stateAbbreviationList)
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = this
    }

    private fun setupEventListeners() {

        tvMetric.setCharacterLists(TickerUtils.provideNumberList())

        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        rgTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.rdButtonWeek -> TimeScale.WEEK
                R.id.rbButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }

        rgMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.rdButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.rdButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        val colorRes = when  (metric) {
            Metric.NEGATIVE -> ContextCompat.getColor(this,R.color.colorNegative)
            Metric.POSITIVE -> ContextCompat.getColor(this, R.color.colorPositive)
            Metric.DEATH -> ContextCompat.getColor(this,R.color.colorDeath)
        }

        sparkView.lineColor = colorRes
        tvMetric.setTextColor(colorRes)

        adapter.metric = metric
        adapter.notifyDataSetChanged()

        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        rdButtonPositive.isChecked = true
        rdButtonMax.isChecked = true
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        tvMetric.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDate.text = outputDateFormat.format(covidData.dateChecked)

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedState = parent?.getItemAtPosition(position) as String
        val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
        updateDisplayWithData(selectedData)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG,"Nothing Selected")
    }
}
