package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.internal.ManufacturerUtils
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.android.bind
import org.koin.android.ext.android.inject
import java.lang.Exception

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        const val LOCATION_REQUEST = 1000
        const val FOREGROUND_THEN_BACKGROUND_REQUEST = 1001
        const val BACKGROUND_LOCATION_REQUEST = 1002

        const val TAG = "SelectLocationFragment"
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentSelectedPoi: PointOfInterest? = null
    private var currentMarker: Marker? = null
    private var zoomLevel = 17.0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        //setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment

        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        onLocationSelected()

        binding.saveButton.setOnClickListener {
            // Changed to currentMarker Due To Testing
            if(currentMarker != null) {
                //_viewModel.setSelectedPOI(currentSelectedPoi!!)     // Not used due to testing issues (can't select a POI with testing!)
                    _viewModel.setSelectedMarker(currentMarker!!)
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), getString(R.string.no_poi_selected_toast), Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }


    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let { googleMap ->

            map = googleMap

            map.uiSettings.isZoomControlsEnabled = true

            // Use Long Click For Testing Purposes
            map.setOnMapLongClickListener { latLng ->
                if(currentMarker != null) {
                    map.clear()
                }
                val addedMarker = map.addMarker(MarkerOptions().position(latLng))
                currentMarker = addedMarker
            }

//            map.setOnPoiClickListener { pointOfInterest ->
//
//                currentSelectedPoi = pointOfInterest
//
//                val poiMarker = map.addMarker(
//                    MarkerOptions().position(pointOfInterest.latLng).title(pointOfInterest.name)
//                )
//
//                poiMarker.showInfoWindow()
//            }

            setMapStyle(map)

            enableLocation()

        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
            if(!success) {
                Log.e(TAG, "Error parsing style")
            }
        } catch(e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style file: " +e.message)
        }
    }

    /**
     * For Android Q and above we need ACCESS_BACKGROUND_LOCATION too. This could be handled elsewhere I guess but will handle it here.
     */
    private fun isPermissionGranted(): Boolean {
        return if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * For Android Q we can request ACCESS_BACKGROUND_LOCATION at the same time as ACCESS_COARSE_LOCATION & ACCESS_FINE_LOCATION
     * For Android R+ we must request ACCESS_BACKGROUND_LOCATION after being granted permission to ACCESS_COARSE_LOCATION & ACCESS_FINE_LOCATION
     */
    class PermissionsHelper(val permissions: Array<String>, val requestCode: Int)

    private fun getLocationRequestDetails(): PermissionsHelper {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                PermissionsHelper(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST
                )
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                PermissionsHelper(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    LOCATION_REQUEST
                )
            }
            else -> {
                PermissionsHelper (
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    FOREGROUND_THEN_BACKGROUND_REQUEST
                )
            }
        }
    }


    @SuppressLint("MissingPermission") // We do check for the permission! Lint doesn't see it in the function
    private fun enableLocation() {
        if(isPermissionGranted()) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    var currentLatLng = LatLng(location.latitude, location.longitude)
                    var cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel)
                    map.animateCamera(cameraUpdate)
                }
            }
        } else {
            // Request Location Permissions
            val requestDetails = getLocationRequestDetails()
            requestPermissions(
                requestDetails.permissions,
                requestDetails.requestCode
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == LOCATION_REQUEST) {
            // ACCESS_FINE_LOCATION
            if(grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation()
            }
        } else if(requestCode == FOREGROUND_THEN_BACKGROUND_REQUEST) {
            // Launch ACCESS_BACKGROUND_REQUEST
            if(grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_REQUEST
                )
            }
        } else if(requestCode == BACKGROUND_LOCATION_REQUEST) {
            // ACCESS_BACKGROUND_LOCATION
            if(grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation()
            }
        }
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


}
