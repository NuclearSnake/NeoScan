package com.neoproductionco.neoscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.TextView
import android.widget.Toast

/**
 * Created by Neo on 18.02.2017.
 */
class TextOutputActivity : Activity() {
    private lateinit var tvValue: TextView
    private lateinit var tvType: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.text_output_layout)
        tvValue = findViewById(R.id.tvValue) as TextView
        tvType = findViewById(R.id.tvType) as TextView

        tvValue.text = intent.getStringExtra("value")
        tvType.text = intent.getStringExtra("name")
        val action = ScannerActivity.ACTION_TYPE.values()[intent.getIntExtra("action", 0)]
        when (action) {
            ScannerActivity.ACTION_TYPE.CONTACT_MECARD, ScannerActivity.ACTION_TYPE.CONTACT_VCARD, ScannerActivity.ACTION_TYPE.TEXT -> {
            }
            ScannerActivity.ACTION_TYPE.GEO_COORDINATES -> {
                tvValue.setTextColor(Color.rgb(0, 0, 255))
                tvValue.setOnClickListener {
                    val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra("value")))
                    startActivity(geoIntent)
                }
            }
            ScannerActivity.ACTION_TYPE.TELEPHONE -> {
                tvValue.setTextColor(Color.rgb(0, 0, 255))
                tvValue.setOnClickListener(View.OnClickListener {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + intent.getStringExtra("value")))
                    if (ActivityCompat.checkSelfPermission(this@TextOutputActivity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this@TextOutputActivity, "Please grant the permission to make calls first", Toast.LENGTH_SHORT).show()
                        ActivityCompat.requestPermissions(this@TextOutputActivity,
                                arrayOf(Manifest.permission.CALL_PHONE),
                                2163)
                        return@OnClickListener
                    }
                    startActivity(intent)
                })
            }
            ScannerActivity.ACTION_TYPE.WEB -> {
                tvValue.setTextColor(Color.rgb(0, 0, 255))
                tvValue.setOnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra("value")))
                    startActivity(browserIntent)
                }
            }
        }// do nothing, just output text
    }
}
