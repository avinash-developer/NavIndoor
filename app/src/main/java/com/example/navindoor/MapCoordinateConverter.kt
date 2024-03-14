package com.example.navindoor

class PixelPoint(val x: Double, val y: Double)

class MapCoordinateConverter {
    // Define the dimensions of the map image in pixels
    private val MAP_IMAGE_WIDTH = 2179
    private val MAP_IMAGE_HEIGHT = 4669

    // Define the real-world coordinates corresponding to the top-left and bottom-right corners of the map image
    private val REAL_WORLD_LONGITUDE_A = 0.0
    private val REAL_WORLD_LATITUDE_A = 0.0
    private val REAL_WORLD_LONGITUDE_B = 10.0
    private val REAL_WORLD_LATITUDE_B = 10.0

    fun pixelToCoordinate(pixelPoint: PixelPoint): DoubleArray {
        val longitudeRange = REAL_WORLD_LONGITUDE_B - REAL_WORLD_LONGITUDE_A
        val latitudeRange = REAL_WORLD_LATITUDE_B - REAL_WORLD_LATITUDE_A

        val pixelX = pixelPoint.x.toDouble()
        val pixelY = pixelPoint.y.toDouble()

        val longitude = REAL_WORLD_LONGITUDE_A + (pixelX / MAP_IMAGE_WIDTH) * longitudeRange
        val latitude = REAL_WORLD_LATITUDE_A + (pixelY / MAP_IMAGE_HEIGHT) * latitudeRange

        return doubleArrayOf(longitude, latitude)
    }
}

