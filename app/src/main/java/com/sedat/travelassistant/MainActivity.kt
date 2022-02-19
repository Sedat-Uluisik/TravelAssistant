package com.sedat.travelassistant

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.sedat.travelassistant.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var fragmentFactory: BaseFragmentFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        supportFragmentManager.fragmentFactory = fragmentFactory
        setContentView(view)

        val language = Locale.getDefault().language

        val sharedPref = this.getSharedPreferences("com.sedat.travelassistant", Context.MODE_PRIVATE) ?:
            return
        with(sharedPref.edit()){
            putString("TRAVEL_ASSISTANT_DEVICE_LANGUAGE", language)
            apply()
        }

        val navController = findNavController(R.id.fragment)
        binding.bottomMenu.setupWithNavController(navController)

    }
}

//Seçilen koordinata yakın müzeleri getirir.
//https://api.geoapify.com/v2/places?categories=entertainment.museum&lang=tr&filter=circle:28.966028,41.012983,1500&limit=20&apiKey=8ed01562dc5f4cbb892adda7cbe205ef
//seçili koordinata 1500 metre içindeki tarihi yerler
//https://api.geoapify.com/v2/places?categories=building.historic&lang=tr&filter=circle:28.966028,41.012983,1500&limit=20&apiKey=8ed01562dc5f4cbb892adda7cbe205ef

//iki nokta arasındaki yolun koordinatlarını verir. rota oluşturmak için bu noktalara marker konulabilir.
//https://api.geoapify.com/v1/routing?waypoints=41.01691905151969,28.984608070774584|41.00865651193891,28.981611730573263&mode=drive&lang=en&apiKey=YOUR_API_KEY
//mode=drive,bicycle,walk,transit

//categories
/*
    entertainment.museum
    entertainment.culture -> eğlence.kültür
    entertainment.culture.theatre -> tiyatro
    entertainment.zoo -> hayvanat bahçesi
    entertainment.aquarium -> akvaryum
    entertainment.cinema

    building.historic
    building.tourism

    tourism.attraction.artwork  ->turizm.cazibe.sanat
    tourism.sights ->turizm.görülecek yerler
    tourism.sights.place_of_worship -> ibadet yerleri
    tourism.sights.place_of_worship.church -> kilise
    tourism.sights.place_of_worship.mosque -> cami
    tourism.sights.place_of_worship.temple -> tapınak
    tourism.sights.place_of_worship.shrine -> türbe
    tourism.sights.monastery -> manastır
    tourism.sights.tower -> kule
    tourism.sights.fort -> kale, hisar
    tourism.sights.castle -> kale, şato
    tourism.sights.ruines -> harabeler
    tourism.sights.archaeological_site -> arkeolojik yerler
    tourism.sights.bridge -> köprü
    tourism.sights.memorial -> anıt

    ski.lift -> teleferik
 */