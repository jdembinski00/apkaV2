package com.example.myapplication

import android.app.DatePickerDialog
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.collections.ArrayList
import kotlin.math.abs

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    var baseCode = "USD"
    var convertedRate = "USD"

    private var startDate: LocalDate = LocalDate.now().minusDays(30)
    private var endDate: LocalDate = LocalDate.now()

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

        binding.buttonStartDate.setOnClickListener {
            showDatePickerDialog(true)
        }

        binding.buttonEndDate.setOnClickListener {
            showDatePickerDialog(false)
        }

        binding.buttonFetchData.setOnClickListener {
            getAPIResultHistory()
        }

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        updateSelectedDatesText()
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

                }
            }


        binding.spinnerConversionRate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    convertedRate = parent?.getItemAtPosition(position).toString()
                    if (baseCode != convertedRate)
                        getAPIResultHistory()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optional: Handle the case where nothing is selected, if needed
                }
            }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            if (isStartDate) {
                startDate = selectedDate
            } else {
                endDate = selectedDate
            }
            updateSelectedDatesText()
        }

        val initialDate = if (isStartDate) startDate else endDate
        DatePickerDialog(
            requireContext(), dateSetListener,
            initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth
        ).show()
    }

    private fun updateSelectedDatesText() {
        binding.textViewSelectedDates.text = "Selected Dates: $startDate to $endDate"
    }

    private fun getAPIResultHistory() {
        if (startDate.isAfter(endDate)) {
            binding.textViewSelectedDates.text = "Invalid date range. Please select again."
            return
        }

        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val historyDataList = ArrayList<Pair<LocalDate, Double>>()
        val apiService = RetrofitClient.instance

        lifecycleScope.launch {
            val requests = (0 until daysBetween).map { i ->
                async(Dispatchers.IO) {
                    val date = endDate.minusDays(i.toLong())
                    val url ="https://v6.exchangerate-api.com/v6/043cc968de6824d516f2433e/history/$baseCode/${date.year}/${date.monthValue}/${date.dayOfMonth}"
                    Log.d("API", "Requesting URL: $url")
                    try {
                        val response = apiService.getHistoricalData(baseCode, date.year, date.monthValue, date.dayOfMonth).execute()
                        if (response.isSuccessful) {
                            val conversionRates = response.body()?.conversionRates ?: return@async null
                            val result = conversionRates[convertedRate] ?: return@async null
                            Pair(date, result)
                        } else {
                            Log.e("API", "Failed to fetch historical data for date: $date, response: ${response.errorBody()?.string()}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("Main", "Error fetching historical data: $e")
                        null
                    }
                }
            }

            historyDataList.addAll(requests.awaitAll().filterNotNull())
            setLineChartData(historyDataList)
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
        configureLineDataSet(lineDataSet)

        val lineData = LineData(lineDataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate()
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
