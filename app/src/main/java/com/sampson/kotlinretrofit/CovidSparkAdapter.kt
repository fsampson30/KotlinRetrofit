package com.sampson.kotlinretrofit

import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {

    override fun getCount() = dailyData.size

    override fun getItem(index: Int) = dailyData[index]

    override fun getY(index: Int): Float {
        val choseDayData = dailyData[index]
        return choseDayData.positiveIncrease.toFloat()
    }


}
