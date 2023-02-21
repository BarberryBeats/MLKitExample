package com.example.tesseracttestik

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tesseracttestik.databinding.ActivityMainBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    companion object {
        const val CAMERA_REQUEST_CODE = 100
        const val STORAGE_REQUEST_CODE = 101
    }

    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var textRecognizer: TextRecognizer


    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        translateText()
        cameraPermission = arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(WRITE_EXTERNAL_STORAGE)
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        initListener()
    }

    private fun initListener() {
        binding.btnTakeImage.setOnClickListener {
            showInputImageDialog()
        }
        binding.btnTranslate.setOnClickListener {
            if (imageUri == null) {

                Toast.makeText(this, "Pick Image First...", Toast.LENGTH_SHORT).show()

            } else {
                recogniseTextFromImage()
            }
        }
    }

    private fun recogniseTextFromImage() {

        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            val textTaskResult = textRecognizer.process(inputImage)
            .addOnSuccessListener {text->

                val recognizedText = text.text
                binding.tvResult.text = recognizedText
            }.addOnFailureListener {
                Toast.makeText(this, "Failed recognize ${it.message}", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "ERRROR ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun showInputImageDialog() {

        val popUpMenu = PopupMenu(this, binding.btnTakeImage)
        popUpMenu.menu.add(Menu.NONE, 1, 1, "CAMERA")
        popUpMenu.menu.add(Menu.NONE, 2, 2, "GALLERY")
        popUpMenu.show()
        popUpMenu.setOnMenuItemClickListener { menuItem ->
            val id = menuItem.itemId
            if (id == 1) {
                if (checkCameraPermission()) {
                    pickImageCamera()
                } else {
                    requestCameraPermission()
                }
            } else if (id == 2) {
                if (checkStoragePermission()) {
                    pickImageGallery()
                } else {
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true
        }

    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private fun pickImageCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                binding.imgResult.setImageURI(imageUri)
            }
        }


    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data!!.data
                binding.imgResult.setImageURI(imageUri)
            }
        }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission(): Boolean {
        val cameraResult =
            ContextCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(
            this,
            WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted) {
                        pickImageCamera()
                    } else {
                        Toast.makeText(this, "Camera& storage perm not", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted) {
                        pickImageGallery()
                    } else {
                        Toast.makeText(this, " storage perm not", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun translateText(){
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.RUSSIAN)
            .build()
        val englishRussianTranslator = Translation.getClient(options)
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        englishRussianTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
               translate(englishRussianTranslator)
                Log.d("Ray", "success")
            }
            .addOnFailureListener { exception ->
                Log.d("Ray", "failure")

            }
    }

    private fun translate(englishRussianTranslator: Translator) {
        englishRussianTranslator.translate("Hello World")
            .addOnSuccessListener {
                Log.d("Ray", it.toString())
                Log.d("Ray", "Russia ${getCurrentLanguageFromText(it)}")
                Log.d("Ray", "English ${getCurrentLanguageFromText("Hello")}")
            }
            .addOnFailureListener {
                Log.d("Ray", "failure translate")

            }
    }

    private fun getCurrentLanguageFromText(text: String): String{
        var result = "und"
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                } else {
                    result = languageCode
                    Log.d("Ray", "langCode $languageCode")
                }
            }
            .addOnFailureListener {
            }
        return result
    }


/*    private fun requestPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 100)
            return false
        }
        return true
    }*/
}

