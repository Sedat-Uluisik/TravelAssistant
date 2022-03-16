package com.sedat.travelassistant.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class SaveImageToFile() {
    fun save(context: Context, bitmap: Bitmap): Uri {  //galeriden alınan resmi kaydetmek için kullanılıyor.
        val dir = File(context.getExternalFilesDir("/"), "pictures")
        if(!dir.exists())
            dir.mkdir()

        val file = File(dir, "${randomUid()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        }catch (e: Exception){
            println(e.message)
        }

        return Uri.parse(file.path)
    }

    fun delete(context: Context, uri: String){
        val file = File(Uri.parse(uri).path.toString())
        if(file.exists())
            if(file.delete())
                Toast.makeText(context, "Resim Silindi", Toast.LENGTH_SHORT).show()
    }

    fun randomUid(): String{
        val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val randomUid: String = List(5) { alphabet.random() }.joinToString("")

        return randomUid
    }
}