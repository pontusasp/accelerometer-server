package com.pontusasp.accelerometerserver

import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var square: TextView
    private lateinit var server: ServerSocket
    private var connected = false
    private var crashed = false
    private var name = "Unknown"
    private var port = 1337
    private var host = "6.9.0.0"
    private var fails = 0
    private val maxFails = 1000

    private var horizontalRotation = 0f
    private var verticalRotation = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        square = findViewById(R.id.square)
        supportActionBar?.hide()

        setUpSensorStuff()
        startServer()
    }

    private fun setUpSensorStuff() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return "0.0.0.0"
    }

    var isRunning = true

    private fun startServer() {
        Log.d("ACC.SERV", "Starting creating server thread...")
        thread {
            Log.d("ACC.SERV", "Starting server...")
            server = ServerSocket(port)
            port = server.localPort
            host = getLocalIpAddress()
            Log.d("ACC.SERV", "Waiting for connection...")
            var retry = true
            while (retry) {
                retry = false
                try {
                    while (isRunning) {
                        val socket = server.accept()
                        Log.d("ACC.SERV", "Connected!")

                        val inputStreamReader = InputStreamReader(socket.getInputStream())
                        val inStream = BufferedReader(inputStreamReader)
                        val outStream = PrintWriter(socket.getOutputStream())

                        name = inStream.readLine()
                        connected = true
                        Log.i("ACC.SERV", "Name: $name")

                        //val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        //val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

                        while (!inStream.readLine().equals("quit")) {
                            outStream.println("${horizontalRotation}:${verticalRotation}")
                            outStream.flush()
                        }

                        outStream.println("quit")
                        outStream.flush()

                        inputStreamReader.close()
                        inStream.close()
                        outStream.close()
                        socket.close()
                        connected = false
                        name = "Unknown"
                        port = server.localPort
                        host = getLocalIpAddress()
                    }
                } catch (e: Exception) {
                    square.text = "Crash! Stacktrace: ${e.printStackTrace()}"
                    fails += 1
                    retry = fails < maxFails
                    crashed = !retry
                }
                connected = false
                name = "Unknown"
                port = server.localPort
                host = getLocalIpAddress()
            }
            server.close()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            verticalRotation = event.values[0]
            horizontalRotation = event.values[1]

            square.apply {
                rotationY = horizontalRotation * -3f
                rotationX = verticalRotation * 3f
            }

            if (!crashed)
                square.text = (
                        (if (connected) "Connected to $name\n" else "Disconnected ($host:$port)\n")+
                        "X ${horizontalRotation.toInt()}\n"+
                        "Z ${verticalRotation.toInt()}"
                        )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        if (!crashed) server.close()
        super.onDestroy()
    }
}