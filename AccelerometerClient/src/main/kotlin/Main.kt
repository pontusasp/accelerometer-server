import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

fun main(args: Array<String>) {
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    var host = "127.0.0.1"
    var port = 1337

    if (args.isNotEmpty()) {
        host = args[0].split(":")[0]
        port = args[0].split(":")[1].toInt()
    }

    val socket = Socket(host, port)
    val outStream = PrintWriter(socket.getOutputStream())
    val inStream = BufferedReader(InputStreamReader(socket.getInputStream()))

    outStream.println("Accelerometer Testing Client")
    outStream.flush()

    var message = ""
    while (message != "quit") {
        message = inStream.readLine()
        println(message)
    }

    println("Exiting...")
    outStream.close()
    inStream.close()
    socket.close()
}