package com.source1.deliveryagent

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class EarningsSummaryActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var lineChart: LineChart
    private lateinit var tvSummaryTotal: TextView
    private var earningsList = mutableListOf<Entry>()
    private var timeCounter = 0f
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_earnings_summary)

        lineChart = findViewById(R.id.earningsChart)
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        findViewById<ImageButton>(R.id.btnBackSummary).setOnClickListener { finish() }

        sharedPref = getSharedPreferences("DeliveryAgentPrefs", Context.MODE_PRIVATE)
        
        setupChart()
        loadInitialData()
    }

    override fun onResume() {
        super.onResume()
        // Register listener to update graph when earnings change
        sharedPref.registerOnSharedPreferenceChangeListener(this)
        // Refresh display in case data changed while paused
        refreshData()
    }

    override fun onPause() {
        super.onPause()
        // Unregister to avoid memory leaks
        sharedPref.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "earnings") {
            runOnUiThread {
                refreshData()
            }
        }
    }

    private fun refreshData() {
        val total = sharedPref.getInt("earnings", 960)
        tvSummaryTotal.text = "Rs $total"
        
        // Add new point to the graph whenever earnings update
        earningsList.add(Entry(timeCounter++, total.toFloat()))
        
        // Keep only last 15 points for better visibility
        if (earningsList.size > 15) {
            earningsList.removeAt(0)
        }
        
        updateChartData()
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            isScaleXEnabled = true
            isScaleYEnabled = true
            setPinchZoom(true)
            setBackgroundColor(Color.WHITE)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                textColor = Color.BLACK
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun loadInitialData() {
        val total = sharedPref.getInt("earnings", 960)
        tvSummaryTotal.text = "Rs $total"
        
        // Initial simulated historical points to make the graph look populated
        earningsList.add(Entry(timeCounter++, total.toFloat() - 200))
        earningsList.add(Entry(timeCounter++, total.toFloat() - 100))
        earningsList.add(Entry(timeCounter++, total.toFloat()))
        
        updateChartData()
    }

    private fun updateChartData() {
        val dataSet = LineDataSet(earningsList, "Earnings History").apply {
            color = Color.parseColor("#4CAF50") // Material Green
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = Color.parseColor("#4CAF50")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.data = LineData(dataSet)
        lineChart.notifyDataSetChanged()
        lineChart.invalidate()
    }
}
