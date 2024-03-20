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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.UUID
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var mapImageView: ImageView
    private lateinit var markerImageView: ImageView
    private lateinit var muteButton: ImageButton
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var minScale = 1f
    private var maxScale = 2f
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_LOCATION_PERMISSION = 2
    private val REQUEST_BLUETOOTH_SCAN_PERMISSION = 3
    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val BLUETOOTH_SCAN_PERMISSION = Manifest.permission.BLUETOOTH_SCAN
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var textToSpeech: TextToSpeech
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapImageView = findViewById(R.id.mapImageView)
        markerImageView = findViewById(R.id.markerImageView)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        muteButton = findViewById(R.id.muteButton)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        gestureDetector = GestureDetector(this, GestureListener())

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {

                textToSpeech.language = Locale.US
            }
        }

        val searchAutoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.searchAutoCompleteTextView)
        searchAutoCompleteTextView.setOnClickListener {
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



        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            checkLocationAndBluetoothPermissions()
            
        }

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


    private fun checkLocationAndBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_LOCATION_PERMISSION

            )

        } else {
            startScan()
        }
    }


    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                BLUETOOTH_SCAN_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                BLUETOOTH_SCAN_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            if (device.name?.equals("ESP32") == true) {
                connectToDevice(device)


            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {


                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {


                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(ESP32_1_SERVICE_UUID)
            var characteristic = service?.getCharacteristic(ESP32_1_CHARACTERISTIC_UUID)

            if (characteristic == null) {

                val secondService = gatt.getService(ESP32_2_SERVICE_UUID)
                characteristic = secondService?.getCharacteristic(ESP32_2_CHARACTERISTIC_UUID)
            }

            if (characteristic == null) {

                val thirdService = gatt.getService(ESP32_3_SERVICE_UUID)
                characteristic = thirdService?.getCharacteristic(ESP32_3_CHARACTERISTIC_UUID)
            }

            if (characteristic != null) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    return
                }
                gatt.readCharacteristic(characteristic)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val data = characteristic.value
            val dataString = data?.toString(Charsets.UTF_8)
            runOnUiThread {
                Toast.makeText(applicationContext, "Received data: $dataString", Toast.LENGTH_SHORT)
                    .show()
                if (dataString == "Block_36") {
                    val coordinates = MapCoordinateConverter().pixelToCoordinate(
                        PixelPoint(
                            431.0,
                            1026.0
                        )
                    )
                    setMarkerToCoordinate(
                        coordinates[0],
                        coordinates[1],
                        mapImageView.scaleX
                    )
                    if(!isMuted) {
                        textToSpeech.speak(
                            "You are near Block 36 and Beacon-1.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                }
                else if (dataString == "Block_37") {
                    val coordinates = MapCoordinateConverter().pixelToCoordinate(
                        PixelPoint(
                            484.0,
                            1034.0
                        )
                    )
                    setMarkerToCoordinate(
                        coordinates[0],
                        coordinates[1],
                        mapImageView.scaleX
                    )
                    if(!isMuted) {
                        textToSpeech.speak(
                            "You are near Block 37 and Beacon-2.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                }
                else if (dataString=="Block_38"){
                    val coordinates = MapCoordinateConverter().pixelToCoordinate(
                        PixelPoint(
                            541.0,
                            1072.0
                        )
                    )


                    setMarkerToCoordinate(
                        coordinates[0],
                        coordinates[1],
                        mapImageView.scaleX
                    )
                    if(!isMuted) {
                        textToSpeech.speak(
                            "You are near Block 38 and Beacon-3.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                }



            }


            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            gatt.disconnect()
            gatt.close()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startScan()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission required for Bluetooth scanning",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

            REQUEST_BLUETOOTH_SCAN_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startScan()
                } else {
                    Toast.makeText(
                        this,
                        "Bluetooth scan permission required for Bluetooth scanning",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopScan()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    companion object {
        private val ESP32_1_SERVICE_UUID =
            UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        private val ESP32_1_CHARACTERISTIC_UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        private val ESP32_2_SERVICE_UUID =
            UUID.fromString("932520fb-68f3-4d89-ba7b-f8d3c6b22772")
        private val ESP32_2_CHARACTERISTIC_UUID =
            UUID.fromString("3693d5c0-f076-4d82-ac97-19f6139e64ce")

        private val ESP32_3_SERVICE_UUID =
            UUID.fromString("0f22311f-2a20-4fe5-b0f2-beb6b8ccab49")
        private val ESP32_3_CHARACTERISTIC_UUID =
            UUID.fromString("4ab015d4-bf71-4aac-9d93-23e2b151c746")
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


        val layoutParams = markerImageView.layoutParams as FrameLayout.LayoutParams
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
                mapImageView.setImageResource(R.drawable.block25)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)

            }
            normalizedFromLocation == "block25" && normalizedToLocation == "block34" ||
                    normalizedFromLocation == "block34" && normalizedToLocation == "block25" -> {
                mapImageView.setImageResource(R.drawable.block34)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)
            }
            normalizedFromLocation == "block34" && normalizedToLocation == "block36" ||
                    normalizedFromLocation == "block36" && normalizedToLocation == "block34" -> {
                mapImageView.setImageResource(R.drawable.block36)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)

            }
            normalizedFromLocation == "maingate" && normalizedToLocation == "bh1" ||
                    normalizedFromLocation == "bh1" && normalizedToLocation == "maingate" -> {
                mapImageView.setImageResource(R.drawable.bh1)
                textToSpeech.speak("Your Path is highlighted", TextToSpeech.QUEUE_FLUSH, null, null)

            }

        }
    }

    private fun normalizeLocation(location: String): String {
        return location.toLowerCase().replace("\\s".toRegex(), "")
    }


}




