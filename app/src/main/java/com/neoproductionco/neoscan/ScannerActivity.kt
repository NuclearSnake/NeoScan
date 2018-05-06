package com.neoproductionco.neoscan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast

import net.sourceforge.zbar.Image
import net.sourceforge.zbar.ImageScanner
import net.sourceforge.zbar.Symbol
import net.sourceforge.zbar.SymbolSet

import java.io.IOException

class ScannerActivity : Activity(), SurfaceHolder.Callback {
    private var camera: Camera? = null
    private var cameraId = 0
    private val scan = true

    private lateinit var svPreview: SurfaceView
    private lateinit var previewcallback: MyPreview

    private var cameraManager: CameraManager? = null // Camera2 API

    private val cameraIDsArray: Array<String>?
        get() {
            try {
                val cameraList = cameraManager!!.cameraIdList
                for (cameraID in cameraList) {
                    Log.i(TAG, "cameraID: " + cameraID)
                }

                return cameraList
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.message)
                e.printStackTrace()
            }

            return null
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // do we have a camera?
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "No camera found!", Toast.LENGTH_LONG).show()
            finish()
        }

        if (ContextCompat.checkSelfPermission(this@ScannerActivity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            startActivity(Intent(this@ScannerActivity, PermissionsActivity::class.java))
            finish()
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        svPreview = findViewById(R.id.svPreview) as SurfaceView

        previewcallback = MyPreview()
    }

    override fun onResume() {
        super.onResume()
        cameraId = findFrontFacingCamera()
        if (cameraId < 0) {
            Toast.makeText(this, "No front facing camera found.",
                    Toast.LENGTH_LONG).show()
        } else {
            camera = Camera.open()
            val parameters = camera!!.parameters
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            parameters.sceneMode = Camera.Parameters.SCENE_MODE_AUTO
            parameters.whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO
            parameters.pictureFormat = ImageFormat.JPEG
            parameters.jpegQuality = 100
            val sizes = parameters.supportedPictureSizes
            var size: Camera.Size = sizes[0]
            for (i in sizes.indices) {
                if (sizes[i].width > size.width)
                    size = sizes[i]
            }
            parameters.setPictureSize(size.width, size.height)
            camera!!.parameters = parameters
        }
        svPreview.holder.addCallback(this)
    }

    private fun findFrontFacingCamera(): Int {
        var cameraId = -1
        // Search for the front facing camera
        val numberOfCameras = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(DEBUG_TAG, "Camera found")
                cameraId = i
                break
            }
        }
        return cameraId
    }

    override fun onPause() {
        if (camera != null) {
            svPreview.holder.removeCallback(this)
            camera!!.setPreviewCallback(null)
            camera!!.stopPreview()
            camera!!.release()
            camera = null
        }
        super.onPause()
    }

    override fun surfaceCreated(holder: SurfaceHolder){}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera!!.setPreviewCallback(previewcallback)
        try {
            camera!!.setPreviewDisplay(svPreview.holder)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        camera!!.startPreview()

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    private inner class MyPreview : Camera.PreviewCallback {

        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            val parameters = camera.parameters
            val size = parameters.previewSize

            System.loadLibrary("iconv")
            val barcode = Image(size.width, size.height, "Y800")
            barcode.data = data

            val scanner = ImageScanner()
            val result = scanner.scanImage(barcode)

            if (result != 0) {
                //            tvScanned.setText("YES");
                val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                camera.cancelAutoFocus()
                camera.setPreviewCallback(null)
                camera.stopPreview()
                //mPreviewing = false;
                val syms = scanner.results
                for (sym in syms) {
                    val symData = sym.data
                    if (!TextUtils.isEmpty(symData)) {
                        Log.d("LOGS", "Result = " + symData)
                        processBarcode(sym)
                        Toast.makeText(this@ScannerActivity, symData, Toast.LENGTH_SHORT).show()
                        //                    Intent dataIntent = new Intent();
                        //dataIntent.putExtra(SCAN_RESULT, symData);
                        //dataIntent.putExtra(SCAN_RESULT_TYPE, sym.getType());
                        //setResult(Activity.RESULT_OK, dataIntent);
                        //finish();
                        break
                    }
                }
            }
            //            tvScanned.setText("NO");
        }

        private fun processBarcode(symbol: Symbol) {
            val intent: Intent
            when (symbol.type) {
                Symbol.QRCODE -> {
                    if (symbol.data.trim { it <= ' ' }.length < 7)
                        textOutput(symbol.data, ACTION_TYPE.TEXT, "QR:text")
                    val prefix = symbol.data.trim { it <= ' ' }.substring(0, 7).toLowerCase()
                    if (prefix.startsWith("mecard"))
                        textOutput(symbol.data, ACTION_TYPE.CONTACT_MECARD, "QR:mecard")
                    else if (prefix.startsWith("mailto")) {
                        textOutput(symbol.data, ACTION_TYPE.MAILTO, "QR.mailto")
                        intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/html"
                        intent.putExtra(Intent.EXTRA_EMAIL, symbol.data.substring(6))
                        this@ScannerActivity.startActivity(intent)

                        //                        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                        //                        intent.putExtra(Intent.EXTRA_TEXT, "I'm email body.");
                    } else if (prefix.startsWith("tel"))
                        textOutput(symbol.data, ACTION_TYPE.TELEPHONE, "QR:tel")
                    else if (prefix.startsWith("geo"))
                        textOutput(symbol.data, ACTION_TYPE.GEO_COORDINATES, "QR:geo")
                    else if (prefix.startsWith("vcard"))
                        textOutput(symbol.data, ACTION_TYPE.CONTACT_VCARD, "QR:vcard")
                    else if (prefix.startsWith("http"))
                        textOutput(symbol.data, ACTION_TYPE.WEB, "QR:http")
                    else
                        textOutput(symbol.data, ACTION_TYPE.TEXT, "QR:text")
                }
                else -> textOutput(symbol.data, ACTION_TYPE.TEXT, getReadableTypeName(symbol.type))
            }
        }

        private fun textOutput(value: String, action: ACTION_TYPE, name: String) {
            val intent = Intent(this@ScannerActivity, TextOutputActivity::class.java)
            intent.putExtra("value", value)
            intent.putExtra("action", action.ordinal)
            intent.putExtra("name", name)
            this@ScannerActivity.startActivity(intent)
        }

        private fun getReadableTypeName(type: Int): String {
            return when (type) {
                Symbol.CODABAR -> "CODABAR"
                Symbol.CODE39 -> "CODE39"
                Symbol.CODE93 -> "CODE93"
                Symbol.CODE128 -> "CODE128"
                Symbol.DATABAR -> "DATABAR"
                Symbol.DATABAR_EXP -> "DATABAR_EXP"
                Symbol.EAN8 -> "EAN8"
                Symbol.EAN13 -> "EAN13"
                Symbol.I25 -> "I25"
                Symbol.ISBN10 -> "ISBN10"
                Symbol.ISBN13 -> "ISBN13"
                Symbol.NONE -> "NONE"
                Symbol.PARTIAL -> "PARTIAL"
                Symbol.PDF417 -> "PDF417"
                Symbol.QRCODE -> "QR"
                Symbol.UPCA -> "UPCA"
                Symbol.UPCE -> "UPCE"
                else -> "UNKNOWN TYPE"
            }
        }
    }

    enum class ACTION_TYPE {
        TEXT, WEB, GEO_COORDINATES, TELEPHONE, CONTACT_VCARD, CONTACT_MECARD, MAILTO
    }

    companion object {
        private val DEBUG_TAG = "MakePhotoActivity"
        private val TAG = "myTag"
    }
}