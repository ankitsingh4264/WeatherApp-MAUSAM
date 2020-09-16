package com.example.whatstheweather

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.whatstheweather.models.WeatherResponse
import com.example.whatstheweather.network.WeatherService
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    lateinit var mfusedlocation:FusedLocationProviderClient
    var currLatitude:Double=0.0;
    var currLongitude:Double=0.0
    lateinit var networkCheck:NetworkCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mfusedlocation=LocationServices.getFusedLocationProviderClient(this)

        if (!islocationEnable()){
             val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent)
        }
        if (checkPermissions()){

            networkCheck= NetworkCheck()
            if (networkCheck.isNetworkConnected(this)){
                newLocationRequest()
            }else{
                Toast.makeText(this,"Oops! No Internet Connection",Toast.LENGTH_SHORT).show()
            }

        }

        









    }

    private fun getLocationWeather() {
        val retrofit=Retrofit.Builder().baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service:WeatherService=retrofit.create(WeatherService::class.java)

        var listcall:Call<WeatherResponse> = service.getWeather(lat = currLatitude,
                 lon = currLongitude,appid = Constants.API_KEY,units = Constants.METRIC_UNIT)
        listcall.enqueue(object:Callback<WeatherResponse>{
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful){
                    val currWeather: WeatherResponse? =response.body()
                    Log.i("response","${currWeather}")
                    setupUI(currWeather!!)
                }else{
                    Log.i("info","error with response not found")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
               Log.i("info",t.message)
            }

        })
        
    }


    private fun checkPermissions():Boolean{
        var check:Boolean=false
        Dexter.withContext(this).withPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION).withListener(object : MultiplePermissionsListener{
            override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                if (p0!!.areAllPermissionsGranted()){
                   check=true;
                }else{
                    Toast.makeText(this@MainActivity,"Plz Provide location permission it's necessary",Toast.LENGTH_SHORT).show()
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {
               p1!!.continuePermissionRequest()
            }

        }).check()

        return  check;

    }


     @SuppressLint("MissingPermission")
    private fun newLocationRequest(){
        var locationRequest=LocationRequest()
        locationRequest.priority=LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval=1000
        locationRequest.numUpdates=1;

        mfusedlocation.requestLocationUpdates(locationRequest,mlocationcallBack, Looper.myLooper())

    }
    private val mlocationcallBack=object : LocationCallback() {

        override fun onLocationResult(locationResult :LocationResult?) {
            val mlastLocation :Location=locationResult!!.lastLocation


            currLatitude=mlastLocation.latitude
            currLongitude=mlastLocation.longitude
            Log.i("location","lat=${currLatitude} long=${currLongitude}")
            Log.i("info","lat=${currLatitude} long=${currLongitude}")
            getLocationWeather()
        }



    }


    private fun setupUI(weatherres:WeatherResponse){
        var currweather=weatherres.weather[0].description
        var f=0;
      when(currweather){
          "clear sky"->{
              weatheriv.setImageResource(R.drawable.clearsky)
              tvweather.text="Clear sky"
              f=1;
          }
          "rain"->{
              weatheriv.setImageResource(R.drawable.rain)
              tvweather.text="Rain"
              f=1;
          }
          "thunderstorm"->{
              weatheriv.setImageResource(R.drawable.thunderstorm)
              tvweather.text="ThunderStorm"
              f=1;
          }
          "snow"->{
              weatheriv.setImageResource(R.drawable.snow)
              tvweather.text="Snow"
              f=1;
          }
          "mist"->{
              weatheriv.setImageResource(R.drawable.mist)
              tvweather.text="Mist"
              f=1;
          }
      }

        if (f==0){
            weatheriv.setImageResource(R.drawable.cloudsalot);
            tvweather.text=currweather
        }
        tvtemp.text= "${weatherres.main.temp} °C"

        tvwind.text="${weatherres.wind.speed} m/s"
        tvlocation.text=weatherres.name
        var sunrisetime=timestamp(weatherres.sys.sunrise)
        var sunsettime=timestamp(weatherres.sys.sunset)
        tvsunrise.text="Sunrise Time:-${sunrisetime}";
        tvsunset.text="Sunset Time :-${sunsettime}";

    }




    fun islocationEnable():Boolean{

        var locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun timestamp(timex:Long):String{
        var date=Date(timex*1000L)
        var sdf=SimpleDateFormat("HH:mm")
        sdf.timeZone=TimeZone.getDefault()
        return  sdf.format(date).toString()
    }

}