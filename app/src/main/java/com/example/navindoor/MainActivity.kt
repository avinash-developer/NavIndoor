package com.example.navindoor

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageButton
import java.util.Locale
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.RelativeLayout
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mapImageView: ImageView
    private lateinit var markerImageView: ImageView
    private lateinit var muteButton: ImageButton
    private lateinit var searchbutton: ImageButton
    private lateinit var infobutton: ImageButton
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var minScale = 1f
    private var maxScale = 2f
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var textToSpeech: TextToSpeech
    private var isMuted = false
    private val handler = Handler()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapImageView = findViewById(R.id.mapImageView)
        markerImageView = findViewById(R.id.markerImageView)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        muteButton = findViewById(R.id.muteButton)
        searchbutton=findViewById(R.id.searchbutton)
        infobutton=findViewById(R.id.infobutton)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        gestureDetector = GestureDetector(this, GestureListener())

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {

                textToSpeech.language = Locale.US
            }
        }


        searchbutton.setOnClickListener{
            showSearchDialog()
        }

        muteButton.setOnClickListener {
            if (isMuted) {
                // Unmute
                textToSpeech.speak("Unmuted", TextToSpeech.QUEUE_FLUSH, null, null)
                muteButton.setImageResource(R.drawable.volume)
                isMuted = false
            } else {
                // Mute
                textToSpeech.speak("Muted", TextToSpeech.QUEUE_FLUSH, null, null)
                muteButton.setImageResource(R.drawable.volume_unmute)
                isMuted = true
            }
        }

        mapImageView.setOnTouchListener { _, event ->
            scaleGestureDetector!!.onTouchEvent(event)
            gestureDetector!!.onTouchEvent(event)

            if (event.action == MotionEvent.ACTION_DOWN) {

                val x = event.x.toDouble()
                val y = event.y.toDouble()

                val pixelmessage = "Longitude: $x\nLatitude: $y"
                Toast.makeText(this@MainActivity, pixelmessage, Toast.LENGTH_SHORT).show()


                val coordinates = MapCoordinateConverter().pixelToCoordinate(PixelPoint(x, y))


                val name = mapCoordinatesToName(coordinates[0], coordinates[1])


                Toast.makeText(this@MainActivity, name, Toast.LENGTH_SHORT).show()
                val longitudeFormatted = String.format("%.3f", coordinates[0])
                val latitudeFormatted = String.format("%.3f", coordinates[1])
                val message =
                    "Longitude: $longitudeFormatted\nLatitude: $latitudeFormatted\nLocation: $name"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()



                return@setOnTouchListener true
            }

            return@setOnTouchListener true
        }

        infobutton.setOnClickListener {
            startActivity(Intent(this@MainActivity, BlockDetails::class.java))
            finish()
        }


        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Check and request necessary permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        }

        // Check if Bluetooth is enabled, if not, request to turn it on
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
        }


        // Register the BluetoothReceiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)

        // Start scanning for Bluetooth devices
        startBluetoothScanContinuously()
    }


    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = mapImageView.scaleX * scaleFactor


            if (newScale >= minScale && newScale <= maxScale) {
                mapImageView.scaleX = newScale
                mapImageView.scaleY = newScale
            }

            return true
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            mapImageView.translationX -= distanceX
            mapImageView.translationY -= distanceY
            return true
        }
    }

    override fun onPause() {
        super.onPause()
        stopBluetoothScan()
    }

    override fun onResume() {
        super.onResume()
        startBluetoothScanContinuously()
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }



    data class LocationRange(
        val lonRange: ClosedFloatingPointRange<Double>,
        val latRange: ClosedFloatingPointRange<Double>,
        val name: String
    )

    private fun mapCoordinatesToName(longitude: Double, latitude: Double): String {

        val ranges = listOf(
            LocationRange(1.920..2.250, 2.130..2.300, "Beacon-1 at Block 36"),
            LocationRange(2.200..2.400, 2.150..2.250, "Beacon 2 at Block 37"),
            LocationRange(2.400..2.650, 2.200..2.300, "Beacon 3 at Block 38")

        )


        for (range in ranges) {
            if (longitude in range.lonRange && latitude in range.latRange) {
                return range.name
            }
        }


        return "Unknown Location"
    }

    private fun setMarkerToCoordinate(x: Double, y: Double, scale: Float) {

        val MAP_IMAGE_WIDTH = 2179
        val MAP_IMAGE_HEIGHT = 4669


        val REAL_WORLD_LONGITUDE_A = 0.0
        val REAL_WORLD_LATITUDE_A = 0.0
        val REAL_WORLD_LONGITUDE_B = 10.0
        val REAL_WORLD_LATITUDE_B = 10.0


        val markerX =
            ((x - REAL_WORLD_LONGITUDE_A) / (REAL_WORLD_LONGITUDE_B - REAL_WORLD_LONGITUDE_A)) * MAP_IMAGE_WIDTH
        val markerY =
            ((y - REAL_WORLD_LATITUDE_A) / (REAL_WORLD_LATITUDE_B - REAL_WORLD_LATITUDE_A)) * MAP_IMAGE_HEIGHT


        val adjustedMarkerX = markerX * scale
        val adjustedMarkerY = markerY * scale


        val translationX = mapImageView.translationX
        val translationY = mapImageView.translationY


        val adjustedX = adjustedMarkerX + translationX
        val adjustedY = adjustedMarkerY + translationY


        val layoutParams = markerImageView.layoutParams as RelativeLayout.LayoutParams
        layoutParams.leftMargin = (adjustedX.toInt() - markerImageView.width / 2).coerceAtLeast(0)
        layoutParams.topMargin = (adjustedY.toInt() - markerImageView.height / 2).coerceAtLeast(0)
        markerImageView.layoutParams = layoutParams


        markerImageView.visibility = View.VISIBLE


    }

    private fun showSearchDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.search_dialog_layout)

        val searchButton = dialog.findViewById<Button>(R.id.searchButton)
        searchButton?.setOnClickListener {

            val from = dialog.findViewById<EditText>(R.id.fromEditText)?.text.toString()
            val to = dialog.findViewById<EditText>(R.id.toEditText)?.text.toString()

            performSearch(from, to)

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performSearch(fromLocation: String, toLocation: String) {
        val normalizedFromLocation = normalizeLocation(fromLocation)
        val normalizedToLocation = normalizeLocation(toLocation)


        when {
            normalizedFromLocation == "block25" && normalizedToLocation == "block36" ||
                    normalizedFromLocation == "block36" && normalizedToLocation == "block25" -> {
                mapImageView.setImageResource(R.drawable.b36_b25)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)

            }
            normalizedFromLocation == "block25" && normalizedToLocation == "block34" ||
                    normalizedFromLocation == "block34" && normalizedToLocation == "block25" -> {
                mapImageView.setImageResource(R.drawable.b34_b25)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)
            }
            normalizedFromLocation == "block34" && normalizedToLocation == "block36" ||
                    normalizedFromLocation == "block36" && normalizedToLocation == "block34" -> {
                mapImageView.setImageResource(R.drawable.b36_b34)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)

            }

            normalizedFromLocation == "maingate" && normalizedToLocation == "admissionblock" ||
                    normalizedFromLocation == "admissionblock" && normalizedToLocation == "maingate"
                    ||  normalizedFromLocation == "maingate" && normalizedToLocation == "block30"
                    || normalizedFromLocation == "block30" && normalizedToLocation == "maingate" -> {
                mapImageView.setImageResource(R.drawable.main_30)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)


                }
            normalizedFromLocation == "maingate" && normalizedToLocation == "bh1" ||
                    normalizedFromLocation == "bh1" && normalizedToLocation == "maingate" -> {
                mapImageView.setImageResource(R.drawable.gate_bh1)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)

            }

        }
    }

    private fun normalizeLocation(location: String): String {
        return location.toLowerCase().replace("\\s".toRegex(), "")
    }


    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    Log.d("BluetoothDevice", "Name: ${device.name}, Address: ${device.address}")
                    if (device.name == "Block25") {
                        mapImageView.setImageResource(R.drawable.block_25)
                        Toast.makeText(this@MainActivity, "Block 25", Toast.LENGTH_SHORT).show()
                        if(!isMuted) {
                            textToSpeech.speak(
                                "You are near Block 25 and Beacon-1 highlighted.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }
                    if (device.name == "Block30") {
                        mapImageView.setImageResource(R.drawable.block_30)
                        Toast.makeText(this@MainActivity, "Block 30", Toast.LENGTH_SHORT).show()
                        if(!isMuted) {
                            textToSpeech.speak(
                                "You are near Admission Block and Beacon-2 highlighted.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }
                    if (device.name == "Block34") {
                        mapImageView.setImageResource(R.drawable.block_34)
                        Toast.makeText(this@MainActivity, "Block 34 highlighted", Toast.LENGTH_SHORT).show()
                        if(!isMuted) {
                            textToSpeech.speak(
                                "You are near Block 34 and Beacon-3.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }
                    if (device.name == "Block36") {
                        mapImageView.setImageResource(R.drawable.block_36)
                        Toast.makeText(this@MainActivity, "Block 36 highlighted", Toast.LENGTH_SHORT).show()
                        if(!isMuted) {
                            textToSpeech.speak(
                                "You are near Block 36 and Beacon-4.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startBluetoothScanContinuously() {
        // Start Bluetooth scan in a loop with a delay
        handler.postDelayed(scanRunnable, SCAN_INTERVAL)
    }

    private fun stopBluetoothScan() {
        // Remove pending callbacks from the handler
        handler.removeCallbacks(scanRunnable)
        // Unregister the Bluetooth receiver
        unregisterReceiver(bluetoothReceiver)
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            // Start Bluetooth scan
            startBluetoothScan()
            // Schedule the next scan
            handler.postDelayed(this, SCAN_INTERVAL)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothScan() {
        bluetoothAdapter.startDiscovery()
    }




    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val REQUEST_ENABLE_BT = 101
        private const val SCAN_INTERVAL: Long = 3000
    }

}



