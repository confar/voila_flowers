package com.example.flowerrecognition


import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val BASE_URL = "https://flower-recogn.herokuapp.com/"
private const val UPLOAD_TIMEOUT = 10000


data class Prediction(val prediction: String,
                      val probability: Float) {

    class Deserializer : ResponseDeserializable<Prediction> {
        override fun deserialize(content: String) = Gson().fromJson(content, Prediction::class.java)
    }

}


class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var button: Button
    lateinit var flowerNameView: TextView
    lateinit var accuracyView: TextView


    private val pickImage = 100
    private var imageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Flower Recognition"
        imageView = findViewById(R.id.imageView)
        flowerNameView = findViewById(R.id.flowerNameTextView)
        flowerNameView.setVisibility(View.GONE);

        accuracyView = findViewById(R.id.AccuracyTextView)
        accuracyView.setVisibility(View.GONE);
        button = findViewById(R.id.buttonLoadPicture)
        button.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            imageView.setImageURI(imageUri)
            val uri = imageUri
            if (uri != null) {
                val parcelFileDescriptor = this.contentResolver.openFileDescriptor(uri, "r", null)

                parcelFileDescriptor?.let {
                    val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                    val file = File(this.cacheDir, this.contentResolver.getFileName(uri))
                    val outputStream = FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    Fuel.upload(BASE_URL + "files/")
                            .add { FileDataPart(file, name="file", filename="oh_boy.jpg") }
                            .timeout(UPLOAD_TIMEOUT)
                            .responseObject(Prediction.Deserializer()) { result ->
                                val (prediction, error) = result
                                prediction?.let {
                                    val flower = getString(R.string.flower_string, prediction.prediction)
                                    flowerNameView.setText(flower)
                                    flowerNameView.setVisibility(View.VISIBLE);

                                    val probability = getString(R.string.probability_string, prediction.probability)
                                    accuracyView.setText(probability)
                                    accuracyView.setVisibility(View.VISIBLE);
                                }
                                println(error)
                            }
                }
            }
        }
    }

    fun ContentResolver.getFileName(fileUri: Uri): String {

        var name = ""
        val returnCursor = this.query(fileUri, null, null, null, null)
        if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            name = returnCursor.getString(nameIndex)
            returnCursor.close()
        }

        return name
    }
}