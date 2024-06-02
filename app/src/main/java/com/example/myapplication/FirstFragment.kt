package com.example.myapplication

import android.os.Bundle
import retrofit2.Call
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentFirstBinding


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    var baseCurrency = "PLN"
    var convertedToCurrency = "USD"
    private val editFromCurrency get() = binding.editFromCurrency
    private val editToCurrency get() = binding.editToCurrency

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spinnerSetup()
        textChangedStuff()
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        //Dot filter on the EditText
        val filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())
        binding.editFromCurrency.filters = filters

        // Setup clear button
        binding.buttonClear.setOnClickListener {
            binding.editFromCurrency.setText("")
            binding.editToCurrency.setText("")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getAPIResult() {
        if (editFromCurrency.text.isNotEmpty() && editFromCurrency.text.isNotBlank()) {
            if (baseCurrency == convertedToCurrency) {
                Toast.makeText(requireContext(), "Cannot convert to the same currency!", Toast.LENGTH_SHORT).show()
            } else {
                val apiService = RetrofitClient.instance
                apiService.getConversionRate(baseCurrency, convertedToCurrency).enqueue(object : retrofit2.Callback<CurrencyResponse> {
                    override fun onResponse(call: Call<CurrencyResponse>, response: retrofit2.Response<CurrencyResponse>) {
                        if (response.isSuccessful) {
                            val conversionRate = response.body()?.conversionRate ?: return
                            val text = ((editFromCurrency.text.toString().toFloat()) * conversionRate).toString()
                            activity?.runOnUiThread {
                                editToCurrency.setText(text)
                            }
                        } else {
                            Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<CurrencyResponse>, t: Throwable) {
                        Toast.makeText(requireContext(), "Error fetching data: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
    }

    private fun spinnerSetup() {
        val spinner: Spinner = binding.spinnerFromCurrency
        val spinner2: Spinner = binding.spinnerToCurrency

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.fromCurrency,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner2.adapter = adapter
            spinner2.setSelection(1)
        }

        spinner.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                baseCurrency = parent?.getItemAtPosition(position).toString()
                getAPIResult()
            }
        })

        spinner2.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                convertedToCurrency = parent?.getItemAtPosition(position).toString()
                getAPIResult()
            }
        })
    }

    private fun textChangedStuff() {
        editFromCurrency.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s.toString().count { it == '.' } <= 1) {
                        getAPIResult()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Type a value", Toast.LENGTH_SHORT).show()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("Main", "Before Text Changed")
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("Main", "OnTextChanged")
            }
        })
    }
}