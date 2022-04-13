package com.example.exampleapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.exampleapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.gson.GsonBuilder

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var placesClient: PlacesClient
    private lateinit var lastLocation : Location
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)
    private lateinit var fusedLocationClient : FusedLocationProviderClient


    private lateinit var context : Context
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5

        const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


     binding = ActivityMapsBinding.inflate(layoutInflater)
     setContentView(binding.root)

        context = this@MapsActivity
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Construct a PlacesClient
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val placeFields: List<Place.Field> = listOf(Place.Field.NAME)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)


    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)
        setUpMap()

    }

    private inner class HitApi(
        context: Context,
        lat: Double,
        lng: Double,
        radius: Int,
        type: String
    ) : AsyncTask<Void, Void, String>() {
        var context: Context? = context
        var lat: Double? = lat
        var lng: Double? = lng
        var radius: Int? = radius
        var type: String? = type

        override fun doInBackground(vararg p0: Void?): String {
            return PlacesAPI().getPlaces(context as Context, lat as Double, lng as Double, radius as Int, type as String)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val gson = GsonBuilder().create()

        }

    }

    @SuppressLint("MissingPermission")
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

                if(location != null){
                    lastLocation = location
                    val currentLatLong = LatLng(location.latitude, location.longitude)
                    placeMarkerOnMap(currentLatLong)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f))
                }
            }
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        }
    }


    private fun placeMarkerOnMap(currentLatLong : LatLng){
        val markerOptions = MarkerOptions().position(currentLatLong)
        MarkerOptions().title("$currentLatLong")
        mMap.addMarker(markerOptions)
    }

    override fun onMarkerClick(p0: Marker) = false



    @SuppressLint("MissingPermission")
    private fun showCurrentPlace() {
        if (mMap == null) {
            return
        }
            // Use fields to define the data types to return.
            val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)

            // Use the builder to create a FindCurrentPlaceRequest.
            val request = FindCurrentPlaceRequest.newInstance(placeFields)

            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            val placeResult = placesClient.findCurrentPlace(request)
            placeResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val likelyPlaces = task.result

                    // Set the count, handling cases where less than 5 entries are returned.
                    val count = if (likelyPlaces != null && likelyPlaces.placeLikelihoods.size < M_MAX_ENTRIES) {
                        likelyPlaces.placeLikelihoods.size
                    } else {
                        M_MAX_ENTRIES
                    }
                    var i = 0
                    likelyPlaceNames = arrayOfNulls(count)
                    likelyPlaceAddresses = arrayOfNulls(count)
                    likelyPlaceAttributions = arrayOfNulls<List<*>?>(count)
                    likelyPlaceLatLngs = arrayOfNulls(count)
                    for (placeLikelihood in likelyPlaces?.placeLikelihoods ?: emptyList()) {
                        // Build a list of likely places to show the user.
                        likelyPlaceNames[i] = placeLikelihood.place.name
                        likelyPlaceAddresses[i] = placeLikelihood.place.address
                        likelyPlaceAttributions[i] = placeLikelihood.place.attributions
                        likelyPlaceLatLngs[i] = placeLikelihood.place.latLng
                        i++
                        if (i > count - 1) {
                            break
                        }
                    }

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    openPlacesDialog()
                } else {
                    Log.e(TAG, "Exception: %s", task.exception)
                }

        }
    }

    private fun openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        val listener = DialogInterface.OnClickListener { dialog, which -> // The "which" argument contains the position of the selected item.
            val markerLatLng = likelyPlaceLatLngs[which]
            var markerSnippet = likelyPlaceAddresses[which]
            if (likelyPlaceAttributions[which] != null) {
                markerSnippet = """
                $markerSnippet
                ${likelyPlaceAttributions[which]}
                """.trimIndent()
            }

            if (markerLatLng == null) {
                return@OnClickListener
            }

            // Add a marker for the selected place, with an info window
            // showing information about that place.
            mMap?.addMarker(MarkerOptions()
                .title(likelyPlaceNames[which])
                .position(markerLatLng)
                .snippet(markerSnippet))

            // Position the map's camera at the location of the marker.
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                DEFAULT_ZOOM.toFloat()))
        }

        // Display the dialog.
        AlertDialog.Builder(this)
            .setTitle(R.string.pick_place)
            .setItems(likelyPlaceNames, listener)
            .show()
    }


}

