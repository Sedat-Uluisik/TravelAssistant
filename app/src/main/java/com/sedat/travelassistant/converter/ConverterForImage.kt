package com.sedat.travelassistant.converter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type

class ConverterForImage {  //room a resim kaydetmek için kullanıldı.
    /*@TypeConverter
    fun fromBitmap(bitmapList: List<Bitmap>): List<ByteArray>{
        val outputStream = ByteArrayOutputStream()
        val list = mutableListOf<ByteArray>()
        for (i in bitmapList){
            i.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            list.add(outputStream.toByteArray())
        }
        return list
    }
    @TypeConverter
    fun toBitmap(byteArrayList: List<ByteArray>): List<Bitmap>{
        val list = mutableListOf<Bitmap>()
        for (i in byteArrayList){
            BitmapFactory.decodeByteArray(i, 0, i.size)
        }
        return list
    }*/

    @TypeConverter
    fun fromBitmap(bitmapList: List<Bitmap>): String{
        val gson = Gson()
        val type = object :TypeToken<List<Bitmap>>() {}.type
        val json: String = gson.toJson(bitmapList, type)

        return json
    }
    @TypeConverter
    fun toBitmap(byteArrayList: String): List<Bitmap>{
        val gson = Gson()
        val type = object :TypeToken<List<Bitmap>>() {}.type
        val list = gson.fromJson<List<Bitmap>>(byteArrayList, type)

        return list
    }
}