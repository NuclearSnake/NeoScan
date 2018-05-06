package com.neoproductionco.neoscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.ImageView

/**
 * Created by Neo on 05.05.2018.
 */

class PermissionsActivity: Activity() {

    private val PERMISSIONS_REQUEST_CAMERA: Int = 14324
    private lateinit var clickableImageView: ImageView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        clickableImageView = findViewById(R.id.clickableImageView)
        clickableImageView.setOnClickListener { request() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
        } else {
            goOn()
        }
    }

    private fun request() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_REQUEST_CAMERA)
    }

    private fun goOn() {
        startActivity(Intent(this, ScannerActivity::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            PERMISSIONS_REQUEST_CAMERA -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    goOn()
                }
            }
        }
    }
}