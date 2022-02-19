package com.sedat.travelassistant.fragment

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.sedat.travelassistant.R
import com.sedat.travelassistant.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var fragmentBinding: FragmentSettingsBinding ?= null
    private val binding get() = fragmentBinding!!

    private var mapLayer: Int = 0
    private var pointLimit: Int = 50
    private var distanceLimit: Int = 1500

    private var drawCircle: Boolean = false
    private var isTraffic: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fragmentBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapLayer = sharedPreferences.getInt("mapTypeTravelAssistant", 0)
        pointLimit = sharedPreferences.getInt("pointLimitTravelAssistant", 50)
        distanceLimit = sharedPreferences.getInt("distanceLimitTravelAssistant", 1500)
        drawCircle = sharedPreferences.getBoolean("drawCircleTravelAssistant", false)
        isTraffic = sharedPreferences.getBoolean("isTrafficTravelAssistant", false)

        changePointLimit(binding.seekbarPoint, binding.seekbarPointText)

        changeDistanceLimit(binding.seekbarDistance, binding.seekbarDistanceText)

        changeMapLayer()

        binding.switchShowCircle.isChecked = drawCircle
        binding.switchShowCircle.setOnClickListener {
            drawCircle = binding.switchShowCircle.isChecked
            sharedPreferences.edit().putBoolean("drawCircleTravelAssistant", drawCircle).apply()
        }

        binding.switchIsTraffic.isChecked = isTraffic
        binding.switchIsTraffic.setOnClickListener {
            isTraffic = binding.switchIsTraffic.isChecked
            sharedPreferences.edit().putBoolean("isTrafficTravelAssistant", isTraffic).apply()
        }

    }

    private fun changePointLimit(seekBar: SeekBar, seekBarTextView: TextView){

        seekBar.progress = pointLimit
        seekBarTextView.text = "$pointLimit"

        seekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBarTextView.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if(seekBar != null)
                    sharedPreferences.edit().putInt("pointLimitTravelAssistant", seekBar.progress).apply()
            }

        })
    }

    private fun changeDistanceLimit(seekBar: SeekBar, seekBarTextView: TextView){
        seekBar.progress = distanceLimit
        seekBarTextView.text = "$distanceLimit"
        seekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBarTextView.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if(seekBar != null)
                    sharedPreferences.edit().putInt("distanceLimitTravelAssistant", seekBar.progress).apply()
            }

        })
    }

    private fun changeMapLayer(){
        val typeDefaultButton = binding.typeDefaultButton
        val typeTerrainButton = binding.typeTerrainButton
        val typeSatelliteButton = binding.typeSatelliteButton

        if(mapLayer == 0){
            typeDefaultButton.setBackgroundResource(R.drawable.button_back_3)
            typeTerrainButton.setBackgroundColor(Color.TRANSPARENT)
            typeSatelliteButton.setBackgroundColor(Color.TRANSPARENT)
        }
        else if(mapLayer == 1){
            typeTerrainButton.setBackgroundResource(R.drawable.button_back_3)
            typeDefaultButton.setBackgroundColor(Color.TRANSPARENT)
            typeSatelliteButton.setBackgroundColor(Color.TRANSPARENT)
        }
        else
        {
            typeSatelliteButton.setBackgroundResource(R.drawable.button_back_3)
            typeTerrainButton.setBackgroundColor(Color.TRANSPARENT)
            typeDefaultButton.setBackgroundColor(Color.TRANSPARENT)
        }

        typeDefaultButton.setOnClickListener {
            it.setBackgroundResource(R.drawable.button_back_3)
            typeTerrainButton.setBackgroundColor(Color.TRANSPARENT)
            typeSatelliteButton.setBackgroundColor(Color.TRANSPARENT)
            sharedPreferences.edit().putInt("mapTypeTravelAssistant", 0).apply()
        }
        typeTerrainButton.setOnClickListener {
            it.setBackgroundResource(R.drawable.button_back_3)
            typeDefaultButton.setBackgroundColor(Color.TRANSPARENT)
            typeSatelliteButton.setBackgroundColor(Color.TRANSPARENT)
            sharedPreferences.edit().putInt("mapTypeTravelAssistant", 1).apply()
        }
        typeSatelliteButton.setOnClickListener {
            it.setBackgroundResource(R.drawable.button_back_3)
            typeTerrainButton.setBackgroundColor(Color.TRANSPARENT)
            typeDefaultButton.setBackgroundColor(Color.TRANSPARENT)
            sharedPreferences.edit().putInt("mapTypeTravelAssistant", 2).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }
}