package com.example.slazzerkotkinapi


import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.slazzerkotkinapi.networking.ApiConfig
import com.example.slazzerkotkinapi.networking.AppConfig
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.Okio
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*


class ImageActivity : AppCompatActivity(), View.OnClickListener,EasyPermissions.PermissionCallbacks {
    private lateinit var imageView: ImageView
    private lateinit var pickImage: Button
    private lateinit var upload: Button
    private var mediaPath: String? = null
    private lateinit var chronometer: Chronometer
    private lateinit var pDialog: ProgressDialog
    private var postPath: String? = null
    private var API_KEY:String ="9fd6e94f3dd******7961cae090cdac6"
    private val READ_STORAGE_PERMISSION_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_layout)

        chronometer = findViewById<Chronometer>(R.id.chronometer)
        chronometer.onChronometerTickListener =
            OnChronometerTickListener { chronometerChanged -> chronometer = chronometerChanged }
        imageView = findViewById<ImageView>(R.id.preview)
        pickImage = findViewById<Button>(R.id.pickImage)
        upload = findViewById<Button>(R.id.upload)
        pickImage.setOnClickListener(this)
        upload.setOnClickListener(this)
        initDialog()

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.pickImage -> {
                val permission = Manifest.permission.READ_EXTERNAL_STORAGE


                if (EasyPermissions.hasPermissions(this, permission)) {
                    if (isExternalStorageAvailable) {

                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        startActivityForResult(galleryIntent, REQUEST_PICK_PHOTO)
                    }
                } else {

                    EasyPermissions.requestPermissions(
                        this,
                        "Our App Requires a permission to access your storage",
                        READ_STORAGE_PERMISSION_REQUEST,
                        permission
                    );
                }


            }


            R.id.upload -> uploadFile(postPath)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_PHOTO) {
                if (data != null) {
                    // Get the Image from data
                    val selectedImage = data.data
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor = contentResolver.query(
                        selectedImage!!,
                        filePathColumn,
                        null,
                        null,
                        null
                    )
                    assert(cursor != null)
                    cursor!!.moveToFirst()

                    val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                    mediaPath = cursor.getString(columnIndex)
                    // Set the Image in ImageView for Previewing the Media
                    Glide.with(this).load(mediaPath).into(imageView)

                    imageView.setImageURI(selectedImage)
                    cursor.close()


                    postPath = mediaPath
                }


            }

        } else if (resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Sorry, there was an error!", Toast.LENGTH_LONG).show()
        }
    }

    protected fun initDialog() {
        pDialog = ProgressDialog(this)
        pDialog.setMessage(getString(R.string.msg_loading))
        pDialog.setCancelable(true)
    }

    private fun showpDialog() {
        if (!pDialog.isShowing) pDialog.show()
    }

    private fun hidepDialog() {
        if (pDialog.isShowing) pDialog.dismiss()
    }

    // Uploading Image/Video
    private fun uploadFile(photoPath:String?) {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start()
        if (photoPath == null || photoPath == "") {
            Toast.makeText(this, "please select an image ", Toast.LENGTH_LONG).show()
            return
        } else {
            showpDialog()
            val map = HashMap<String, RequestBody>()
            val file = File(photoPath!!)
            val requestBody = RequestBody.create(MediaType.parse("*/*"), file)
            map.put("source_image_file\"; filename=\"" + file.name + "\"", requestBody)
            val getResponse = AppConfig.getRetrofit().create(ApiConfig::class.java)
            val call = getResponse.upload(API_KEY, map)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            hidepDialog()
                            try {
                                val downloadedFile: File = File(
                                    this@ImageActivity.getCacheDir(),
                                    "opt-put-results.png"
                                )
                                val sink: BufferedSink = Okio.buffer(Okio.sink(downloadedFile))
                                sink.writeAll(response.body().source())
                                sink.close()
                                chronometer.stop()
                                Glide.with(this@ImageActivity).load(downloadedFile).into(imageView)
                            } catch (ex: Exception) {
                                print(ex.toString())
                            }
                        }
                    } else {
                        hidepDialog()
                        Toast.makeText(
                            applicationContext,
                            "problem uploading image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    hidepDialog()
                    t.message?.let { Log.v("Response gotten is", it) }
                }
            })
        }
    }

    companion object {
        private val REQUEST_PICK_PHOTO = 2

    }

    private val isExternalStorageReadOnly: Boolean get() {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)
    }
    private val isExternalStorageAvailable: Boolean get() {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED.equals(extStorageState)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }
}



