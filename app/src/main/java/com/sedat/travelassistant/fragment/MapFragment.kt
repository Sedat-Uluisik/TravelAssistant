package com.sedat.travelassistant.fragment

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.sedat.travelassistant.R
import com.sedat.travelassistant.adapter.SelectedRouteAdapter
import com.sedat.travelassistant.converter.ConverterForImage
import com.sedat.travelassistant.databinding.*
import com.sedat.travelassistant.listener.CustomClickListener
import com.sedat.travelassistant.model.Datasource
import com.sedat.travelassistant.model.Feature
import com.sedat.travelassistant.model.Properties
import com.sedat.travelassistant.model.room.SavedPlace
import com.sedat.travelassistant.model.selectedroute.SelectedRoute
import com.sedat.travelassistant.util.CalculateDistance
import com.sedat.travelassistant.viewmodel.MapFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback, CustomClickListener {    //mor reng kodu  912cba

    private var DEFAULT_CATEGORY = "tourism.sights"
    private var DEFAULT_PROFILE = "car"
    private var maxRouteSize = 4
    private var pointLimit: Int = 50
    private var distanceLimit: Int = 1500
    private var mapLayer: Int = 0

    private var fragmentBinding: FragmentMapBinding? = null
    private val binding get() = fragmentBinding!!

    private var infoWindowBinding: MarkerInfoWindowBinding ?= null

    private lateinit var mMap: GoogleMap
    private lateinit var polyLine: Polyline
    private lateinit var userLocation: LatLng
    private lateinit var viewModel: MapFragmentViewModel
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient //Bilinen son konumu almak için kullanıldı.

    @Inject
    lateinit var glide: RequestManager
    @Inject
    lateinit var selectedRouteAdapter: SelectedRouteAdapter
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var placeList = mutableListOf<Properties>() //Konumun detaylarına gitmek için kullanıldı.
    private var placeListForSaved = mutableListOf<SavedPlace>() //Kaydedilen konumun detaylarına gitmek için kullanıldı.

    private var dialog: AlertDialog ?= null
    private var isTraffic: Boolean = false
    private var gpsEnabled: Boolean = false
    private var drawCircle: Boolean = false
    private var getOncePlaces: Boolean = true  //konum değiştiğinde sürekli veri çekmemek için kullanıldı.
    private var userLocationPoint: Location ?= null
    private var userLocationIsAdded: Boolean = false  //kullanıcı konumunu rotaya bir defaya mahsus eklemek için kullanılıyor.
    private var getBothLocations: Boolean = false  //internetten alınan ve kaydedilen konumları göstermek için kullanılıyor.
    private var selectedRouteForDetailsFabCreateRouteButton: SelectedRoute ?= null //detaylardan direk rota oluştur butonuna tıklanınca oradaki koordinatlar buraya gönderilir, onu almak için kullanılıyor.

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentMapBinding.inflate(inflater, container, false)
        val view = binding.root

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapF) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerLauncher()

        fakeAlertDialog()

        viewModel = ViewModelProvider(requireActivity()).get(MapFragmentViewModel::class.java)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        pointLimit = sharedPreferences.getInt("pointLimitTravelAssistant", 50)
        distanceLimit = sharedPreferences.getInt("distanceLimitTravelAssistant", 1500)
        mapLayer = sharedPreferences.getInt("mapTypeTravelAssistant", 0)
        drawCircle = sharedPreferences.getBoolean("drawCircleTravelAssistant", false)
        isTraffic = sharedPreferences.getBoolean("isTrafficTravelAssistant", false)

        arguments?.let {
            val category = MapFragmentArgs.fromBundle(it).category
            if (category != DEFAULT_CATEGORY) {
                DEFAULT_CATEGORY = category
            }

            val selectedRoute = MapFragmentArgs.fromBundle(it).selectedRoute
            if(selectedRoute != null && viewModel.routeStarted.value != 1){ //0: false, 1: true
                selectedRouteForDetailsFabCreateRouteButton = selectedRoute
                //arguments?.clear()
                arguments?.remove("selectedRoute")
            }
        }
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! hatanın sebebi ?
        //java.lang.RuntimeException: Parcelable encountered IOException writing serializable object (name = com.sedat.travelassistant.model.selectedroute.SelectedRoute)

        if(viewModel.selectedRouteCoordinates.size > 0)
            binding.fabCreateRoute.visibility = View.VISIBLE

        binding.fabCreateRoute.setOnClickListener {

            if(viewModel.routeStarted.value == 0){  //rota oluşturulur.
                showSelectedRouteView()
            }
            else{  //oluşturulan rotalar iptal edilir.
                if(mMap != null){
                    viewModel.routeStarted.postValue(0)
                    polyLine.remove()
                    viewModel.clearRoutePoints()
                    userLocationIsAdded = false
                    binding.fabCreateRoute.setImageResource(R.drawable.route_64)
                    maxRouteSize = 4
                    it.visibility = View.GONE
                }
            }
        }

        binding.fabSettings.setOnClickListener {
            fabSettingsButtonClick()
        }
        binding.fabEditLocation.setOnClickListener {
            fabEditLocationButtonClick()
        }

        viewModel.getVisitedLocationPoints()

        binding.zoomUp.setOnClickListener {
            var newZoom = viewModel.mapZoom.value!!
            newZoom += 1
            if(newZoom in 5.0f..20.0f){
                viewModel.mapZoom.value = newZoom
            }
            else
                newZoom = viewModel.mapZoom.value!!

            if(mMap != null)
                mMap.animateCamera(CameraUpdateFactory.zoomTo(newZoom))

        }
        binding.zoomDown.setOnClickListener {
            var newZoom = viewModel.mapZoom.value!!
            newZoom -= 1
            if(newZoom in 5.0f..20.0f)
                viewModel.mapZoom.value = newZoom
            else
                newZoom = viewModel.mapZoom.value!!

            if(mMap != null)
                mMap.animateCamera(CameraUpdateFactory.zoomTo(newZoom))
        }
    }

    private fun observePlaces(googleMap: GoogleMap) {

        /*glide.asBitmap().load("data:image/png;base64,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").into(object : CustomTarget<Bitmap>(){
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {



            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })*/

        viewModel.places.observe(viewLifecycleOwner, {
            it?.let {

                viewModel.getVisitedLocationPoints()
                placeList.clear()
                placeList.addAll(it.features.map { feature ->
                    feature.properties
                })  //marker a tıklanıldığında bilgi ekranı göstermek için kullanılıyor.

                for (place in it.features) {
                    val location = LatLng(place.properties.lat, place.properties.lon)
                    var markerColor = 0

                    if(getBothLocations){  //marker lar içinde kaydedilen konumlardan varsa rengini mavi yapmak için kullanıldı.
                        if(viewModel.savedPlaces2.size > 0){
                            for (k in viewModel.savedPlaces2){
                                if(k.lat == place.properties.lat && k.lon == place.properties.lon){
                                    markerColor = 3
                                    break
                                }
                            }
                        }
                    }

                    //ziyaret edilen yerlerin marker ları gezilen tarihe göre yeşil/kırmızı yapıldı.
                    if(viewModel.visitedLocationList.size > 0){
                        for (i in viewModel.visitedLocationList){
                            val latLng = LatLng(i.latitude, i.longitude)
                            if(latLng == location){
                                markerColor = if(checkDate(i.date)) {  //Aynı tarihte içinde gezilmiş ise marker yeşil
                                    1
                                }
                                else { //önceki tarihlerde gezilmiş ise kırmızı
                                    2
                                }
                            }
                        }
                    }

                    val marker = mMap.addMarker(
                            MarkerOptions()
                                    .position(location)
                                    .title(place.properties.name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            when (markerColor) {
                                                0 -> BitmapDescriptorFactory.HUE_VIOLET
                                                1 -> BitmapDescriptorFactory.HUE_GREEN
                                                2 -> BitmapDescriptorFactory.HUE_RED
                                                else -> BitmapDescriptorFactory.HUE_BLUE
                                            }
                                    ))
                    )

                    marker?.let {
                        marker.tag = "${location.latitude},${location.longitude}"
                        viewModel.markersOnMap.add(marker)
                    }
                }

                /*if(getBothLocations && viewModel.savedPlaces2.size > 0){ //Her iki konumu(internet ve room dan) da getirirken kullanıcının kendi eklediği konumu haritada göstermek için kullanılıyor.
                    for (i in viewModel.savedPlaces2){
                        val marker = mMap.addMarker(
                                MarkerOptions()
                                        .position(LatLng(i.lat, i.lon))
                                        .title(i.name)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                        //kayıtlı yerleri gösterirken onlarada rota oluşturma eklenecek
                        marker?.let {
                            marker.tag = "${i.lat},${i.lon}"
                            viewModel.markersOnMap.add(marker)
                        }
                    }

                    placeListForSaved.addAll(viewModel.savedPlaces2)
                }*/
            }
        })

        viewModel.pointListForRoute.observe(viewLifecycleOwner, Observer {
            it?.let { list ->
                if(list.isNotEmpty() && viewModel.routeStarted.value == 0){

                    val options = PolylineOptions()
                    options.width(9f)
                    options.color(Color.parseColor("#912cba"))
                    for (i in list){
                        val latLong = LatLng(i[1], i[0])
                        options.add(latLong)
                    }

                    println("--------------")

                    if(googleMap != null)
                        polyLine = googleMap.addPolyline(options)

                    viewModel.routeStarted.postValue(1)
                    binding.fabCreateRoute.setImageResource(R.drawable.cancel_64)
                }else
                    binding.fabCreateRoute.setImageResource(R.drawable.cancel_64)
            }
        })

        viewModel.savedPlaces.observe(viewLifecycleOwner, {
            placeListForSaved.clear()
            placeListForSaved.addAll(it)



            for (i in it){

                val location = LatLng(i.lat, i.lon)
                var markerColor = 0

                if(viewModel.visitedLocationList.size > 0){
                    for (j in viewModel.visitedLocationList){
                        val latLng = LatLng(j.latitude, j.longitude)
                        if(latLng == location){
                            markerColor = if(checkDate(j.date)) {  //Aynı tarihte içinde gezilmiş ise marker yeşil
                                1
                            }
                            else { //önceki tarihlerde gezilmiş ise kırmızı
                                2
                            }
                            //break
                        }
                    }
                }

                val marker = mMap.addMarker(
                        MarkerOptions()
                                .position(LatLng(i.lat, i.lon))
                                .title(i.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                       when(markerColor){
                                           0 -> BitmapDescriptorFactory.HUE_BLUE
                                           1 -> BitmapDescriptorFactory.HUE_GREEN
                                           else -> BitmapDescriptorFactory.HUE_RED
                                       }
                                ))
                )

                //kayıtlı yerleri gösterirken onlarada rota oluşturma eklenecek
                marker?.let {
                    marker.tag = "${i.lat},${i.lon}"
                    viewModel.markersOnMap.add(marker)
                }
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        locationManager = requireContext().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        mMap = googleMap
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if(!gpsEnabled){  //konum kapalı
            if(isOnline(requireContext())){ //internet açık
                binding.errorLinearLayout.visibility = View.VISIBLE
                binding.errorLocation.visibility = View.VISIBLE
                binding.errorNetwork.visibility = View.GONE
            }else{  //internet kapalı
                binding.errorLinearLayout.visibility = View.VISIBLE
                binding.errorLocation.visibility = View.VISIBLE
                binding.errorNetwork.visibility = View.VISIBLE
            }
        }else{ //konum açık
            if(isOnline(requireContext())){ //internet açık
                binding.errorLinearLayout.visibility = View.GONE
            }else{  //internet kapalı
                binding.errorLinearLayout.visibility = View.VISIBLE
                binding.errorLocation.visibility = View.GONE
                binding.errorNetwork.visibility = View.VISIBLE
            }
        }

        when (mapLayer) {
            0 -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            1 -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            else -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }

        mMap.setMapStyle(MapStyleOptions(resources.getString(R.string.remove_google_map_icons)))

        observePlaces(googleMap)

        mMap.isTrafficEnabled = isTraffic
        mMap.uiSettings.isZoomControlsEnabled = false

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if(location != null && gpsEnabled){

                    if(getOncePlaces){
                        CameraPosition.builder().zoom(viewModel.mapZoom.value!!)
                    }
                    else{
                        CameraPosition.builder().zoom(viewModel.mapZoom.value!!)
                    }

                    userLocation = LatLng(location.latitude, location.longitude)
                    userLocationPoint = location

                    val cameraPosition = CameraPosition(
                            userLocation,
                            viewModel.mapZoom.value!!,
                            30f,  //harita 30 derece eğik gösterilir.
                            location.bearing
                    )
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                    CoroutineScope(Dispatchers.Main).launch {
                        val lastLocation = viewModel.firstLocationTakenFromUser.value

                        val distance = CalculateDistance().calculateDistanceTwoPoint(location, lastLocation ?: location)

                        if(distance > distanceLimit){

                            drawCircle = true
                            getPlaces()
                            viewModel.firstLocationTakenFromUser.value = location

                        }else{
                            if(getOncePlaces){ //Bu kontrol ile verilerin tekrar indirilimesi engellendi ve veriler livedata dan tekrar alındı..
                                getPlaces()
                                getOncePlaces = false  //konum her değişitiğinde verileri tekrar indirmek engelleniyor.
                            }
                        }
                    }

                    if(viewModel.routeStarted.value == 1){  //kullanıcı rota oluşturduysa çalışır.
                        //Kullanıcı seçtiği rotadaki noktalara geldiğinde, orayı gezmiş sayıp marker ın rengi değiştirilir.
                        lifecycleScope.launch {
                            for (i in viewModel.selectedRouteCoordinates){
                                val distance = CalculateDistance().calculateDistanceTwoPoint(location, i.location) //kullanıcı ile seçili rota noktaları arasındaki mesafe ölçülür.
                                if(distance < 50f){
                                    val point = "${i.location.latitude},${i.location.longitude}"
                                    for (j in viewModel.markersOnMap){
                                        if(point == j.tag){  //haritadaki marker lardan bizim rotaya eklediğimiz marker bulunup rengi değiştirilir.
                                            j.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                        }
                                    }
                                    viewModel.saveLocationForVisited(LatLng(i.location.latitude, i.location.longitude))
                                }
                            }
                        }
                    }
                }else
                    Toast.makeText(requireContext(), "konum açık değil", Toast.LENGTH_LONG).show()
            }

            override fun onProviderEnabled(provider: String) {

            }
            override fun onProviderDisabled(provider: String) {

            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }
        }

        mMap.setOnMarkerClickListener(object :GoogleMap.OnMarkerClickListener{
            override fun onMarkerClick(p0: Marker): Boolean {
                for (place in placeList){ //internetten alınan bilgileri gösterir.
                    if(p0.tag == "${place.lat},${place.lon}"){
                        showInfoWindow(place)
                    }
                }
                for (place in placeListForSaved){  //kaydedilen bilgileri gösterir.
                    if(p0.tag == "${place.lat},${place.lon}"){
                        val newPlace = convertSavedPlaceToProperties(place)
                        showInfoWindow(newPlace)
                    }
                }
                return true
            }

        })

        //Konum izni verilmediyse
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Konum izni iste
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.permission_needed_for_location),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(getString(R.string.give_permission)) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            } else
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            //kullanıcı konumunu al
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10,
                15f,  //konum değişikliği 15m ve üzeri ise onLocationChanged fonk çalışır.
                locationListener
            )

            //son bilinen konuma git ve oradaki yerleri getir.
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                    userLocationPoint = it
                    if(getOncePlaces){
                        getPlaces()
                        getOncePlaces = false
                    }
                    viewModel.firstLocationTakenFromUser.value = location

                    val cameraPosition = CameraPosition(
                            userLocation,
                            viewModel.mapZoom.value!!,
                            30f,  //harita 30 derece eğik gösterilir.
                            it.bearing
                    )
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    CameraPosition.builder().zoom(viewModel.mapZoom.value!!)

                    if(selectedRouteForDetailsFabCreateRouteButton != null){
                        binding.fabCreateRoute.visibility = View.VISIBLE
                        viewModel.selectedRouteCoordinates.clear()
                        viewModel.selectedRouteCoordinates.add(
                                SelectedRoute("user_location",   //kullanıcı konumu başlangıç konumu olarak ekleniyor.
                                        Location("").also { location ->
                                            location.latitude = it.latitude
                                            location.longitude = it.longitude
                                        }
                                )
                        )
                        viewModel.selectedRouteCoordinates.add(selectedRouteForDetailsFabCreateRouteButton!!)

                        showSelectedRouteView()
                    }
                }
            }

            mMap.isMyLocationEnabled = true
        }
    }

    private fun convertSavedPlaceToProperties(savedPlace: SavedPlace): Properties{
        return Properties(
                "",
                "",
                listOf(),
                savedPlace.city,
                "",
                "",
                "",
                Datasource("", "", "", ""),
                listOf(),
                savedPlace.district,
                savedPlace.address,
                "",
                savedPlace.lat,
                savedPlace.lon,
                savedPlace.name,
                "",
                "",
                "",
                savedPlace.state,
                savedPlace.street,
                savedPlace.suburb,
                ""
        )
    }

    private fun getPlaces(){
        if(viewModel.routeStarted.value == 0)
            mMap.clear()
        viewModel.getPlaces(
                DEFAULT_CATEGORY,
                "circle:${userLocation.longitude},${userLocation.latitude},$distanceLimit",
                pointLimit
        )

        if(drawCircle)
            drawCircle()
    }

    private fun fabEditLocationButtonClick(){
        val view = FabEditLocationButtonAlertDialogBinding.inflate(LayoutInflater.from(requireContext()))
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setCancelable(true)
        alertDialog.setView(view.root)

        val  dialog: AlertDialog = alertDialog.create()
        if(dialog.window != null)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        dialog.show()

        view.onlyInternet.setOnClickListener {
            dialog.dismiss()
            getBothLocations = false
            getPlaces()
        }
        view.onlySaved.setOnClickListener {
            dialog.dismiss()
            getBothLocations = false
            if(viewModel.routeStarted.value == 0)
                mMap.clear()
            if(drawCircle)
                drawCircle()
            viewModel.getPlacesForRoom()
        }
        view.bothOfThem.setOnClickListener {
            dialog.dismiss()
            getBothLocations = true
            if(viewModel.routeStarted.value == 0)
                mMap.clear()
            if(drawCircle)
                drawCircle()
            //roomdaki verileri al.
            viewModel.getPlacesForRoom()
            //internetteki veri al.
            getPlaces()
        }
    }

    private fun fakeAlertDialog(){
        /*
        alert dialog ile başka fragment e gidip geri tuşuna basılınca haritada donmalar kasmalar oluyor,
        bunu düzetmek için tekrar alrtDialog oluşturup kapatmak sorunu şimdilik çözüyor.
         */
        lifecycleScope.launchWhenStarted {
            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setCancelable(false)
            alertDialog.setView(R.layout.fab_settings_button_alert_dialog)

            dialog = alertDialog.create()
            if(dialog != null){
                if(dialog!!.window != null)
                    dialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
                dialog!!.show()
            }

            dialog?.dismiss()
            dialog = null
        }
    }

    private fun fabSettingsButtonClick(){
        val view = FabSettingsButtonAlertDialogBinding.inflate(LayoutInflater.from(requireContext()))
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setCancelable(true)
        alertDialog.setView(view.root)

        //val dialog: AlertDialog = alertDialog.create()
        dialog = alertDialog.create()
        if(dialog != null){
            if (dialog!!.window != null)
                dialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
            dialog!!.show()
        }

        view.settingsButton.setOnClickListener {
            getOncePlaces = true  //tekrar sayfaya dönüldüğünde ayarların uygulanması için true yapıldı.
            findNavController().navigate(MapFragmentDirections.actionMapFragmentToSettingsFragment())
        }
        view.addLocationButton.setOnClickListener {
            findNavController().navigate(MapFragmentDirections.actionMapFragmentToSavedDetailsFragment(
                    null,
                    isUpdate = false,
                    isAddNewLocation = true,
                    if(userLocationPoint != null) userLocationPoint else null
            ))
        }
    }

    private fun showInfoWindow(place: Properties){
        lifecycleScope.launchWhenStarted {
            if(dialog == null){  //iki konuma aynı anda tıklanma engellendi.
                infoWindowBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.marker_info_window, null, false)
                if(infoWindowBinding != null){
                    val alertDialog = AlertDialog.Builder(requireContext())
                    alertDialog.setCancelable(false)
                    alertDialog.setView(infoWindowBinding!!.root)

                    infoWindowBinding!!.properties = place
                    infoWindowBinding!!.addToRouteButtonText = "${getString(R.string.add_to_route)} ($maxRouteSize)"
                    infoWindowBinding!!.clickListener = this@MapFragment

                    dialog = alertDialog.create()
                    if(dialog != null){
                        if(dialog!!.window != null)
                            dialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
                        dialog!!.show()

                        checkIsFavorite(infoWindowBinding!!.favoriteButton, place.lat, place.lon)
                    }
                }
            }
        }
    }

    private suspend fun checkIsFavorite(imageView: ImageView, lat: Double, lon: Double){
        val isFavorite = viewModel.isFavoritePlace(lat, lon)

        if(isFavorite)
            imageView.setImageResource(R.drawable.favorite_on_24)
        else
            imageView.setImageResource(R.drawable.favorite_off_24)
    }

    private fun drawCircle(){
        if(userLocation != null){
            val circleOptions = CircleOptions()
                    .center(LatLng(userLocation.latitude, userLocation.longitude))
                    .radius(distanceLimit.toDouble())
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.parseColor("#500084d3"))
                    .strokeWidth(5f)
            val circle: Circle = mMap.addCircle(circleOptions)
        }
    }

    private val swipeRecyclerItemForDelete = object :ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT){
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.layoutPosition
            val selectedPoint = selectedRouteAdapter.selectedRouteList[position]
            viewModel.selectedRouteCoordinates.remove(selectedPoint)
        }

    }

    private fun showSelectedRouteView(){ //Seçilen rotaları alert dialog içinde gösterme, rota başlatma, iptal etme işlemleri.
        if(dialog == null){
            val selectedRouteView = SelectedRoutesViewBinding.inflate(LayoutInflater.from(requireContext()))
            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setCancelable(false)
            alertDialog.setView(selectedRouteView.root)

            dialog = alertDialog.create()
            if(dialog != null){
                if(dialog!!.window != null)
                    dialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
                dialog!!.show()
            }

            selectedRouteView.recyclerSelectedRoutes.layoutManager = LinearLayoutManager(requireContext())
            selectedRouteView.recyclerSelectedRoutes.adapter = selectedRouteAdapter
            ItemTouchHelper(swipeRecyclerItemForDelete).attachToRecyclerView(selectedRouteView.recyclerSelectedRoutes)

            //asenkron olarak seçilen noktalar yakınlık mesafesine göre yeniden sıralandı.
            runBlocking {
                val sortedList = CalculateDistance().calculate(viewModel.selectedRouteCoordinates)
            }

            val forAdapterList = mutableListOf<SelectedRoute>()
            forAdapterList.addAll(viewModel.selectedRouteCoordinates)
            forAdapterList.removeFirst() //ilk rota kullanıcının olduğu için adapter de göstermemesi için silindi.
            selectedRouteAdapter.selectedRouteList = forAdapterList

            selectedRouteView.cancelButton.setOnClickListener {
                dialog?.dismiss()
                dialog = null

                viewModel.routeStarted.postValue(0)
                viewModel.clearRoutePoints()
                userLocationIsAdded = false
                maxRouteSize = 4

                binding.fabCreateRoute.visibility = View.GONE
            }
            selectedRouteView.startButton.setOnClickListener {
                if(viewModel.routeStarted.value == 0){ //rota oluşturulur.
                    getRoutePoints()
                }

                selectedRouteForDetailsFabCreateRouteButton = null

                dialog?.dismiss()
                dialog = null
            }
            selectedRouteView.addRouteButton.setOnClickListener {
                dialog?.dismiss()
                dialog = null
            }
            //profile buttons
            val profileCar = selectedRouteView.profileCar
            val profileBike = selectedRouteView.profileBike
            val profileWalk = selectedRouteView.profileWalk
            profileCar.setOnClickListener {
                it.setBackgroundResource(R.drawable.button_back_3)
                profileBike.setBackgroundColor(Color.TRANSPARENT)
                profileWalk.setBackgroundColor(Color.TRANSPARENT)
                DEFAULT_PROFILE = "car"
            }
            profileBike.setOnClickListener {
                it.setBackgroundResource(R.drawable.button_back_3)
                profileCar.setBackgroundColor(Color.TRANSPARENT)
                profileWalk.setBackgroundColor(Color.TRANSPARENT)
                DEFAULT_PROFILE = "bike"
            }
            profileWalk.setOnClickListener {
                it.setBackgroundResource(R.drawable.button_back_3)
                profileBike.setBackgroundColor(Color.TRANSPARENT)
                profileCar.setBackgroundColor(Color.TRANSPARENT)
                DEFAULT_PROFILE = "foot"
            }
        }
    }


    private fun registerLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    //konum izni verildi
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                10,
                                15f, //konum değişikliği 15m ve üzeri ise onLocationChanged fonk çalışır.
                                locationListener
                        )
                    }

                    //imleci  son bilinen konuma taşı.
                    /*val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null) {
                        val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(lastUserLocation, viewModel.mapZoom.value!!)))
                        viewModel.firstLocationTakenFromUser.value = lastLocation
                        userLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        userLocationPoint = lastLocation
                    }*/

                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                        location?.let { loc ->
                            userLocation = LatLng(loc.latitude, loc.longitude)
                            userLocationPoint = loc
                            if(getOncePlaces){
                                getPlaces()
                                getOncePlaces = false
                            }
                            viewModel.firstLocationTakenFromUser.value = location

                            val cameraPosition = CameraPosition(
                                    userLocation,
                                    viewModel.mapZoom.value!!,
                                    30f,  //harita 30 derece eğik gösterilir.
                                    loc.bearing
                            )
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                            CameraPosition.builder().zoom(viewModel.mapZoom.value!!)
                        }
                    }

                    mMap.isMyLocationEnabled = true
                } else {
                    //İzin verilmedi
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.permission_needed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun getRoutePoints(){
        viewModel.getRoute(requireContext(), DEFAULT_PROFILE)
    }

    private fun checkDate(timeInMillis: Long): Boolean{ //marker rengini değiştirmek için kullanılıyor.(gezilmemiş ise mor, bugün gezilmiş ise yeşil gibi)
        val visitedTime = DateFormat.format("dd/MM/yyyy", timeInMillis)
        val nowTime = DateFormat.format("dd/MM/yyyy", System.currentTimeMillis())
        return visitedTime == nowTime
    }

    private fun isOnline(context: Context): Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(connectivityManager != null){
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if(capabilities != null){
                return true
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
        dialog?.dismiss()
        dialog = null
    }

    override fun onCloseButtonClick() {
        dialog?.dismiss()
        dialog = null

        if(viewModel.selectedRouteCoordinates.size > 1){
            binding.fabCreateRoute.setImageResource(R.drawable.route_64)
            binding.fabCreateRoute.visibility = View.VISIBLE
        }
    }

    override fun onDetailsButtonClick(place: Properties) {
        if(!(place.name.isNullOrEmpty())){
            dialog?.dismiss()
            dialog = null
            getOncePlaces = false
            findNavController().navigate(MapFragmentDirections.actionMapFragmentToDetailsFragment(place))
        }else
            Toast.makeText(requireContext(), getString(R.string.not_found_info), Toast.LENGTH_LONG).show()
    }

    override fun onAddToRouteButtonClick(lat: Double, lon: Double, name: String?) {
        val location = Location("route")
        location.latitude = lat
        location.longitude = lon
        val selectedRoute = SelectedRoute(if(name.isNullOrEmpty()) "-" else name, location)

        if(!userLocationIsAdded && userLocationPoint != null){
            viewModel.selectedRouteCoordinates.add(SelectedRoute("User Location", userLocationPoint!!)) //başlangıç olarak kullanıcı konumu bir defa eklendi.
            userLocationIsAdded = true
        }

        var check = false
        if(viewModel != null){
            for (i in viewModel.selectedRouteCoordinates.map { it.location }){  //Seçilen rotanın birden fazla eklenmesi önlendi.
                if(i == location){
                    check = true
                }
            }
            if(!check){
                if(maxRouteSize > 0){
                    viewModel.selectedRouteCoordinates.add(selectedRoute)
                    Toast.makeText(requireContext(), getString(R.string.route_added), Toast.LENGTH_SHORT).show()
                    maxRouteSize--
                }else
                    Toast.makeText(requireContext(), getString(R.string.max_4_routes_can_be_added), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onFavoriteButtonClick(properties: Properties) {
        lifecycleScope.launch {

            val isFavorite = viewModel.isFavoritePlace(properties.lat, properties.lon)

            if(isFavorite){  //delete from room
                viewModel.deletePlaceFromRoom(properties.lat, properties.lon)
                infoWindowBinding!!.favoriteButton.setImageResource(R.drawable.favorite_off_24)
            }else{  //save from room
                viewModel.savePlaceForRoom(properties)
                infoWindowBinding!!.favoriteButton.setImageResource(R.drawable.favorite_on_24)
            }
        }
    }
}