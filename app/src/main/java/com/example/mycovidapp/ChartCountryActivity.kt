package com.example.mycovidapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.mycovidapp.model.InfoNegara
import com.example.mycovidapp.model.Negara
import com.example.mycovidapp.network.InfoService
import com.example.mycovidapp.network.RetrofitBuilder
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_chart_country.*
import kotlinx.android.synthetic.main.list_country.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class ChartCountryActivity : AppCompatActivity() {
    companion object{
        const val EXTRA_COUNTRY = "EXTRA_COUNTRY"
        lateinit var simpanDataNegara: String
        lateinit var simpanDataflag: String
    }
    private val sharePrefFile = "kotlinsharedprefence"
    private lateinit var sharedPreference: SharedPreferences
    private var dayCases = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart_country)

        sharedPreference = this.getSharedPreferences(sharePrefFile, Context.MODE_PRIVATE)

        val formatter: NumberFormat = DecimalFormat("#,###")
        val editor: SharedPreferences.Editor = sharedPreference.edit()

        val data = intent.getParcelableExtra<Negara>(EXTRA_COUNTRY)
        data?.let {
            txt_name_country.text = data.Country
            latest_update.text = data.Date
            hasil_total_death_currently.text = formatter.format(data.TotalDeaths?.toDouble())
            hasil_new_death_currently.text = formatter.format(data.NewDeaths?.toDouble())
            hasil_new_confirmed_currently.text = formatter.format(data.NewConfirmed?.toDouble())
            hasil_total_confirmed_currently.text = formatter.format(data.TotalConfirmed?.toDouble())
            hasil_total_recovered_currently.text = formatter.format(data.TotalRecovered?.toDouble())
            hasil_new_recovered_currently.text = formatter.format(data.NewRecovered?.toDouble())

            editor.putString(data.Country, data.Country)
            editor.apply()
            editor.commit()

            val simpanNegara = sharedPreference.getString(data.Country, data.Country)
            val simpanFlag =  sharedPreference.getString(data.CountryCode, data.CountryCode)
            simpanDataNegara = simpanNegara.toString()
            simpanDataflag = simpanFlag.toString()+"/flat/64.png"

            if (simpanFlag != null){
                Glide.with(this).load("https://www.countryflags.io/$simpanDataflag")
                    .into(img_flag_country)
            }else{
                Toast.makeText(this, "Image Not Found", Toast.LENGTH_SHORT).show()
            }

            getCountry()
        }
    }
    private fun getCountry(){
        val okhttp  = OkHttpClient().newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.covid19api.com/dayone/country/")
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(InfoService::class.java)
        api.getInforService(simpanDataNegara).enqueue(object : Callback<List<InfoNegara>>{
            @SuppressLint("SimpleDateFormat")
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<List<InfoNegara>>, response: Response<List<InfoNegara>>
            ) {
                val getListDataCorona: List<InfoNegara> =response.body()!!
                if (response.isSuccessful){
                    val barEnteries: ArrayList<BarEntry> = ArrayList()
                    val barEnteries2: ArrayList<BarEntry> = ArrayList()
                    val barEnteries3: ArrayList<BarEntry> = ArrayList()
                    val barEnteries4: ArrayList<BarEntry> = ArrayList()
                    var i = 0
                    while (i < getListDataCorona.size){
                        for (s in getListDataCorona){
                            val barEntry = BarEntry(i.toFloat(), s.Confirmed?.toFloat() ?: 0f)
                            val barEntry2 = BarEntry(i.toFloat(), s.Deaths?.toFloat()?: 0f)
                            val barEntry3 = BarEntry(i.toFloat(), s.Recovered?.toFloat()?: 0f)
                            val barEntry4 = BarEntry(i.toFloat(), s.Active?.toFloat()?: 0f)

                            val inputFormat= SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'")
                            val outputFormat = SimpleDateFormat("dd-MM-yyy")
                            val date: Date? = inputFormat.parse(s.Date)
                            val formatteDate: String = outputFormat.format(date!!)
                            dayCases.add(formatteDate)

                            barEnteries.add(barEntry)
                            barEnteries2.add(barEntry2)
                            barEnteries3.add(barEntry3)
                            barEnteries4.add(barEntry4)

                            i++

                        }

                        val xAxis: XAxis = barchardview.xAxis
                        xAxis.valueFormatter= IndexAxisValueFormatter(dayCases)
                        barchardview.axisLeft.axisMinimum = 0f
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.setCenterAxisLabels(true)
                        xAxis.isGranularityEnabled = true

                        val barDataSet = BarDataSet(barEnteries, "Confirmed")
                        val barDataSet2 = BarDataSet(barEnteries2, "Death")
                        val barDataSet3 = BarDataSet(barEnteries3, "Recovered")
                        val barDataSet4 = BarDataSet(barEnteries, "Activity")
                        barDataSet.setColor(Color.parseColor("#f44336"))
                        barDataSet2.setColor(Color.parseColor("#ffEB3B"))
                        barDataSet3.setColor(Color.parseColor("#03DAC5"))
                        barDataSet4.setColor(Color.parseColor("#2196F3"))

                        val data = BarData(barDataSet, barDataSet2, barDataSet3, barDataSet4)
                        barchardview.data = data

                        val barSpace= 0.02f
                        val groupSpace= 0.3f
                        val GroupCount= 4f

                        data.barWidth = 0.15f
                        barchardview.invalidate()
                        barchardview.setNoDataTextColor(R.color.black)
                        barchardview.setTouchEnabled(true)
                        barchardview.description.isEnabled = false
                        barchardview.xAxis.axisMinimum = 0f
                        barchardview.setVisibleXRangeMaximum(
                            0f+barchardview.barData.getGroupWidth(
                                groupSpace,
                                barSpace
                            )* GroupCount
                        )

                        barchardview.groupBars(0f, groupSpace, barSpace)

                    }
                }
            }

            override fun onFailure(call: Call<List<InfoNegara>>, t: Throwable) {
                Toast.makeText(this@ChartCountryActivity, "EROR re-enter to this country",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
}