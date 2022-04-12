package com.example.exampleapp

import android.content.Context
import java.io.BufferedReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class PlacesAPI {

    public fun getPlaces (context: Context, lat: Double, lng:Double, radius: Int, type: String) : String{
        var result = ""

        try{
            val key: String = context.resources.getString(R.string.maps_api_key)
            val urlstr: String = "https://maps.googleapis.com/maps/api/places/nearbySearch/json?location=$lat,$lng&radius=$radius&type=$type&key=$key"
            val url : URL = URL(urlstr)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Content-Type", "application/json")
            connection.requestMethod = "GET"
            connection.doInput = true
            val br = connection.inputStream.bufferedReader()
            result = br.use { br.readText() }
            connection.disconnect()
        } catch (e: Exception){
            e.printStackTrace()
        }
        return result

    }
}