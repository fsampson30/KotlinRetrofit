package com.sampson.kotlinretrofit

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getCount() = dailyData.size

    override fun getItem(index: Int) = dailyData[index]

    override fun getY(index: Int): Float {
        val choseDayData = dailyData[index]
        return when (metric) {
            Metric.NEGATIVE -> choseDayData.negativeIncrease.toFloat()
            Metric.POSITIVE -> choseDayData.positiveIncrease.toFloat()
            Metric.DEATH -> choseDayData.deathIncrease.toFloat()

        }
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.MAX){
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }
}
