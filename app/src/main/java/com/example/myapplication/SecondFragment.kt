package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentSecondBinding
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    var baseCode = "USD"
    var convertedRate = "USD"
    var numOfRequests = 15

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupDaysSpinner()

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSpinners() {
        // Dummy currency list
        val currencies = listOf("EUR", "GBP", "USD", "PLN", "SEK", "AUD", "CAD", "JPY")
        val baseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        baseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val convAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        convAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBaseChart.adapter = baseAdapter
        binding.spinnerConversionRate.adapter = convAdapter
        binding.spinnerConversionRate.setSelection(1)
        // Set item selected listener for the base currency spinner
        binding.spinnerBaseChart.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Update baseCode with the selected item
                    baseCode = parent?.getItemAtPosition(position).toString()
                    if (baseCode != convertedRate)
                        getAPIResultHistory()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optional: Handle the case where nothing is selected, if needed
                }
            }

        // Set item selected listener for the conversion rate spinner
        binding.spinnerConversionRate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Update convertedRate with the selected item
                    convertedRate = parent?.getItemAtPosition(position).toString()
                    if (baseCode != convertedRate)
                        getAPIResultHistory()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optional: Handle the case where nothing is selected, if needed
                }
            }
    }

    private fun setupDaysSpinner() {
        val daysOptions = arrayOf("5", "10", "15", "20", "25", "30")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDays.adapter = adapter
        binding.spinnerDays.setSelection(3)
        binding.spinnerDays.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                numOfRequests = parent.getItemAtPosition(position).toString().toInt()
                getAPIResultHistory()  // Optionally refresh data when selection changes
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun getAPIResultHistory() {
        val startDate = LocalDate.now().minusDays(1)
        val historyDataList = ArrayList<Pair<LocalDate, Double>>()

        val apiService = RetrofitClient.instance
        lifecycleScope.launch(Dispatchers.IO) {
            for (i in 0 until numOfRequests) {
                val date = startDate.minusDays(i.toLong())
                try {
                    val response = apiService.getHistoricalData(baseCode, date.year, date.monthValue, date.dayOfMonth).execute()
                    if (response.isSuccessful) {
                        val conversionRates = response.body()?.conversionRates ?: continue // If null, skip to next iteration
                        val result = conversionRates[convertedRate] ?: continue // If null, skip to next iteration
                        withContext(Dispatchers.Main) {
                            historyDataList.add(Pair(date, result))
                        }
                    } else {
                        Log.e("API", "Failed to fetch historical data for date: $date")
                    }
                } catch (e: Exception) {
                    Log.e("Main", "Error fetching historical data: $e")
                }

                // Update chart on the last fetch
                if (i == numOfRequests - 1) {
                    withContext(Dispatchers.Main) {
                        setLineChartData(historyDataList)
                    }
                }
            }
        }
    }

    private fun setLineChartData(historyDataList: ArrayList<Pair<LocalDate, Double>>) {
        val earliestDate = historyDataList.minByOrNull { it.first }?.first ?: return

        val lineValues = historyDataList.map { dataPoint ->
            val daysBetween = abs(ChronoUnit.DAYS.between(earliestDate, dataPoint.first)).toFloat()
            Entry(daysBetween, dataPoint.second.toFloat())
        }

        val lineDataSet =
            LineDataSet(lineValues.asReversed(), "History of $baseCode to $convertedRate")
        configureLineDataSet(lineDataSet) // Konfiguracja wyglądu DataSet

        val lineData = LineData(lineDataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate() // Odświeżenie wykresu
    }

    private fun configureLineDataSet(lineDataSet: LineDataSet) {
        lineDataSet.color = resources.getColor(R.color.purple_200, null)
        lineDataSet.circleRadius = 5f
        lineDataSet.setDrawCircles(true)
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawFilled(true)
        lineDataSet.fillColor = resources.getColor(R.color.green, null)
        lineDataSet.lineWidth = 2f
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineDataSet.setCircleColor(resources.getColor(R.color.purple_200, null))
    }
}