package com.pontusasp.accelerometerserver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var square: TextView
    private var connected = false
    private var name = "Unknown"
    private var port = 1337
    private var host = "0.0.0.0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        square = findViewById(R.id.square)

        setUpSensorStuff()
        startServer()
    }

    private fun setUpSensorStuff() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun startServer() {
        thread {
            Log.d("ACC.SERV", "Starting server...")
            val server = ServerSocket(0)
            Log.d("ACC.SERV", "Waiting for connection...")
            val socket = server.accept()
            Log.d("ACC.SERV", "Connected!")
            port = server.localPort
            host = server.localSocketAddress.toString()

            val inputStreamReader = InputStreamReader(socket.getInputStream())
            val inStream = BufferedReader(inputStreamReader)
            val outStream = PrintWriter(socket.getOutputStream())

            name = inStream.readLine()
            Log.i("ACC.SERV", "Name: $name")

            //val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            //val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

            connected = true;
            outStream.println("quit")
            outStream.flush()

            inputStreamReader.close()
            inStream.close()
            outStream.close()
            socket.close()
            server.close()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val sides = event.values[0]
            val upDown = event.values[1]

            square.apply {
                rotationX = upDown * 3f
                rotationY = sides * 3f
            }

            square.text = (
                    (if (connected) "Connected to $name\n" else "Disconnected ($host:$port)\n")+
                    "X ${upDown.toInt()}\n"+
                    "Z ${sides.toInt()}"
                    )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}