package com.source1.deliveryagent

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EarningsSummaryActivity : AppCompatActivity(){

    private lateinit var lineChart: LineChart
    private lateinit var tvSummaryTotal: TextView
    private var earningsList = mutableListOf<Entry>()
    private var timeCounter = 0f

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_earnings_summary)

        lineChart = findViewById(R.id.earningsChart)
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        findViewById<ImageButton>(R.id.btnBackSummary).setOnClickListener { finish() }
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupChart()
        loadEarningsFromFirebase()

    }
    private fun loadEarningsFromFirebase() {

        val userId = auth.currentUser!!.uid

        database.child("DeliveryBoys").child(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val total = snapshot.child("earnings")
                        .getValue(Int::class.java) ?: 0

                    tvSummaryTotal.text = "Rs $total"

                    // graph update
                    earningsList.add(Entry(timeCounter++, total.toFloat()))

                    if (earningsList.size > 15) {
                        earningsList.removeAt(0)
                    }

                    updateChartData()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
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
