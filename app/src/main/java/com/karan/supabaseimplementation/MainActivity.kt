package com.karan.supabaseimplementation

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.karan.supabaseimplementation.databinding.ActivityMainBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 100
    private val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 101
    private lateinit var supabaseClient: SupabaseClient
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supabaseClient = (applicationContext as MyApplication).supabaseClient
        checkAndRequestPermissions()
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {

                } else {
                    requestManageExternalStoragePermission()
                }
            } else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestManageExternalStoragePermission()
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent,PERMISSION_REQUEST_CODE)
            } catch (e:ActivityNotFoundException) {
                Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {                          //Android 10 below
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                }
            }

            MANAGE_EXTERNAL_STORAGE_REQUEST_CODE -> {             //Android 10 above
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Full storage granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Full storage not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Toast.makeText(this, "result code: ${requestCode}", Toast.LENGTH_SHORT).show()
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            data?.data?.let { uri ->
                binding.imgView.setImageURI(uri)
                uploadImageToSupabase(uri)
            }
        }

    }


    private fun uploadImageToSupabase(uri: Uri) {
        val byteArray = uriToByteArray(this, uri)
        val fileName = "uploads/${System.currentTimeMillis()}.jpg"
        val bucket = supabaseClient.storage.from("Test")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                bucket.uploadAsFlow(fileName, byteArray).collect { status ->
                    withContext(Dispatchers.Main)
                    {
                        when (status) {
                            is UploadStatus.Progress -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Upload in Progress",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            is UploadStatus.Success -> {
                                Log.d("Upload", "Upload Success")
                                val imageUrl = bucket.publicUrl(fileName)
                                var img = binding.imgView
                                Glide.with(this@MainActivity)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_launcher_background)
                                    .into(img);
                                Toast.makeText(
                                    this@MainActivity,
                                    "Upload Success",
                                    Toast.LENGTH_SHORT
                                ).show()

//                                Glide.with(this@MainActivity)
//                                    .load(imageUrl)
//                                    .placeholder(R.drawable.ic_launcher_background)
//                                    .into(img);
//                                Toast.makeText(
//                                    this@MainActivity,
//                                    "Upload Success",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                            }
                        }
                    }
                }
            }
            catch (e:Exception) {
                withContext(Dispatchers.Main)
                {
                    Toast.makeText(
                        this@MainActivity,
                        "Error in uploading Image:${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri) ///f
        return inputStream?.readBytes()?: ByteArray(0)
    }
}