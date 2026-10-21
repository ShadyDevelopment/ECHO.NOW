package com.echonow.platform

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.echonow.domain.repository.PairedDevice
import com.echonow.domain.repository.PlaybackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.HttpURLConnection
import java.util.UUID
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

internal object AndroidDevicePairing {
    private const val TAG = "EchoPairing"
    private const val SERVICE_TYPE = "_echonow._tcp."
    private const val PORT = 45876
    private const val PREFS = "echo_device_pairing"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_PAIRED = "paired"

    private lateinit var context: Context
    private lateinit var nsd: NsdManager
    private val endpoints = ConcurrentHashMap<String, InetAddress>()
    private val paired = MutableStateFlow<List<PairedDevice>>(emptyList())
    private val online = MutableStateFlow<List<PairedDevice>>(emptyList())
    private val incoming = MutableSharedFlow<PlaybackRequest>(extraBufferCapacity = 8)
    private val secureRandom = SecureRandom()
    private var deviceId = ""
    @Volatile private var pairingCode: String? = null
    @Volatile private var localName = "ECHO NOW"
    @Volatile private var serverStarted = false
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun initialize(appContext: Context, deviceName: String) {
        if (::context.isInitialized) {
            updateName(deviceName)
            return
        }
        context = appContext
        nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
        localName = deviceName.ifBlank { "ECHO NOW" }
        loadPaired()
        startServer()
        discoverServices()
    }

    fun pairedDevices(): StateFlow<List<PairedDevice>> = paired
    fun onlinePairedDevices(): StateFlow<List<PairedDevice>> = online
    fun incomingPlayback(): SharedFlow<PlaybackRequest> = incoming

    fun setPairingMode(code: String?, name: String) {
        pairingCode = code?.takeIf { it.length == 6 && it.all(Char::isDigit) }
        updateName(name)
    }

    fun updateName(name: String) {
        localName = name.trim().ifBlank { "ECHO NOW" }.take(40)
        if (paired.value.isNotEmpty() || pairingCode != null) registerService() else unregisterService()
    }

    fun unpair(id: String) {
        persist(paired.value.filterNot { it.id == id })
        refreshOnline()
    }

    suspend fun pairWithCode(code: String): String? = withContext(Dispatchers.IO) {
        require(code.length == 6 && code.all(Char::isDigit)) { "Enter all six digits of the pairing code." }
        val targets = endpoints.keys.toList()
        if (targets.isEmpty()) return@withContext "No ECHO NOW devices are currently discoverable. Keep the other device's Pairing settings open and try again."
        var lastError = "The pairing code was not accepted by any available device."
        for (targetId in targets) {
            val address = endpoints[targetId] ?: continue
            try {
                val response = request(
                    address,
                    "/pair",
                    JSONObject()
                        .put("code", code)
                        .put("deviceId", deviceId)
                        .put("name", localName),
                )
                if (response.optBoolean("ok")) {
                    val targetName = response.optString("name").ifBlank { "ECHO NOW device" }
                    val token = response.optString("token")
                    if (token.isBlank()) {
                        lastError = "The remote device returned an invalid pairing response."
                        continue
                    }
                    persist(paired.value.filterNot { it.id == targetId } + PairedDevice(targetId, targetName, token))
                    refreshOnline()
                    return@withContext null
                }
                lastError = response.optString("error").ifBlank { lastError }
            } catch (error: Exception) {
                lastError = error.message ?: "Could not connect to the selected device."
            }
        }
        lastError
    }

    suspend fun cast(deviceId: String, mediaId: String, season: Int, episode: Int) = withContext(Dispatchers.IO) {
        val address = endpoints[deviceId] ?: error("That paired device is offline.")
        val response = request(
            address,
            "/play",
            JSONObject()
                .put("senderId", this@AndroidDevicePairing.deviceId)
                .put("token", paired.value.firstOrNull { it.id == deviceId }?.pairingToken.orEmpty())
                .put("mediaId", mediaId)
                .put("season", season)
                .put("episode", episode),
        )
        check(response.optBoolean("ok")) {
            response.optString("error").ifBlank { "The remote device rejected the playback request." }
        }
    }

    private fun startServer() {
        if (serverStarted) return
        serverStarted = true
        thread(name = "echo-device-pairing-server", isDaemon = true) {
            try {
                ServerSocket(PORT, 16, InetAddress.getByName("0.0.0.0")).use { server ->
                    while (!server.isClosed) {
                        val socket = server.accept()
                        thread(name = "echo-device-pairing-client", isDaemon = true) { handle(socket) }
                    }
                }
            } catch (error: Exception) {
                serverStarted = false
                Log.e(TAG, "Local pairing server stopped.", error)
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = 5_000
                val input = BufferedInputStream(client.getInputStream())
                val header = StringBuilder()
                var matched = 0
                while (header.length < 8192 && matched < 4) {
                    val next = input.read()
                    if (next < 0) break
                    header.append(next.toChar())
                    matched = when {
                        matched == 0 && next == 13 -> 1
                        matched == 1 && next == 10 -> 2
                        matched == 2 && next == 13 -> 3
                        matched == 3 && next == 10 -> 4
                        next == 13 -> 1
                        else -> 0
                    }
                }
                val headerText = header.toString()
                val requestLine = headerText.lineSequence().firstOrNull().orEmpty()
                val method = requestLine.substringBefore(' ')
                val path = requestLine.split(' ').getOrNull(1).orEmpty()
                val contentLength = headerText.lineSequence()
                    .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                    ?.substringAfter(':')?.trim()?.toIntOrNull()?.coerceIn(0, 16_384) ?: 0
                val body = ByteArray(contentLength)
                var offset = 0
                while (offset < body.size) {
                    val read = input.read(body, offset, body.size - offset)
                    if (read < 0) break
                    offset += read
                }
                val json = if (offset == body.size && body.isNotEmpty()) JSONObject(body.decodeToString()) else JSONObject()
                val response = when {
                    method == "POST" && path == "/pair" -> handlePair(json)
                    method == "POST" && path == "/play" -> handlePlayback(json)
                    else -> JSONObject().put("ok", false).put("error", "Unsupported device request.")
                }
                val bytes = response.toString().encodeToByteArray()
                val output = BufferedOutputStream(client.getOutputStream())
                output.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".encodeToByteArray())
                output.write(bytes)
                output.flush()
            } catch (error: Exception) {
                Log.w(TAG, "Failed to process a local device request.", error)
            }
        }
    }

    private fun handlePair(json: JSONObject): JSONObject {
        if (pairingCode == null) return JSONObject().put("ok", false).put("error", "Pairing is not active on this device. Open Settings > Device > Pairing.")
        if (json.optString("code") != pairingCode) return JSONObject().put("ok", false).put("error", "That one-time pairing code is incorrect or has expired.")
        val remoteId = json.optString("deviceId")
        val remoteName = json.optString("name").ifBlank { "ECHO NOW device" }
        if (remoteId.isBlank() || remoteId == deviceId) return JSONObject().put("ok", false).put("error", "Invalid device identity.")
        val token = ByteArray(32).also(secureRandom::nextBytes).joinToString("") { "%02x".format(it) }
        persist(paired.value.filterNot { it.id == remoteId } + PairedDevice(remoteId, remoteName, token))
        return JSONObject().put("ok", true).put("name", localName).put("token", token)
    }

    private fun handlePlayback(json: JSONObject): JSONObject {
        val senderId = json.optString("senderId")
        if (paired.value.none { it.id == senderId && it.pairingToken.isNotBlank() && it.pairingToken == json.optString("token") }) {
            return JSONObject().put("ok", false).put("error", "This device is not paired with the sender.")
        }
        val mediaId = json.optString("mediaId")
        if (mediaId.isBlank()) return JSONObject().put("ok", false).put("error", "The playback request did not include a title.")
        incoming.tryEmit(
            PlaybackRequest(
                senderId = senderId,
                mediaId = mediaId,
                season = json.optInt("season"),
                episode = json.optInt("episode"),
            ),
        )
        return JSONObject().put("ok", true)
    }

    private fun discoverServices() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.trimEnd('.') != SERVICE_TYPE.trimEnd('.')) return
                try {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            Log.w(TAG, "Could not resolve a nearby ECHO NOW device: $errorCode")
                        }
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val id = info.attributes["id"]?.decodeToString().orEmpty()
                            val name = info.attributes["name"]?.decodeToString().orEmpty()
                            if (id.isBlank() || id == deviceId) return
                            endpoints[id] = info.host
                            if (paired.value.none { it.id == id }) return
                            online.value = paired.value.filter { endpoints.containsKey(it.id) }
                                .map { paired -> paired.copy(name = name.ifBlank { paired.name }) }
                        }
                    })
                } catch (error: Exception) {
                    Log.w(TAG, "Could not resolve nearby service.", error)
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val id = serviceInfo.attributes["id"]?.decodeToString()
                if (id != null) endpoints.remove(id)
                refreshOnline()
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Device discovery failed to start: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Device discovery failed to stop: $errorCode")
            }
        }
        discoveryListener = listener
        try {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (error: Exception) {
            Log.e(TAG, "Could not start local device discovery.", error)
        }
    }

    private fun registerService() {
        val name = localName.take(50)
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = name
            serviceType = SERVICE_TYPE
            port = PORT
            setAttribute("id", deviceId)
            setAttribute("name", name)
        }
        try {
            registrationListener?.let { runCatching { nsd.unregisterService(it) } }
            val listener = object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Could not advertise this device: $errorCode")
                }
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Could not stop advertising this device: $errorCode")
                }
            }
            registrationListener = listener
            nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (error: Exception) {
            Log.e(TAG, "Could not register local device service.", error)
        }
    }

    private fun unregisterService() {
        val listener = registrationListener ?: return
        try {
            nsd.unregisterService(listener)
        } catch (error: Exception) {
            Log.w(TAG, "Could not unregister local device service.", error)
        } finally {
            registrationListener = null
        }
    }

    private fun request(address: InetAddress, path: String, body: JSONObject): JSONObject {
        val connection = URL("http://${address.hostAddress}:$PORT$path").openConnection() as HttpURLConnection
        connection.connectTimeout = 4_000
        connection.readTimeout = 4_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        try {
            connection.outputStream.use { it.write(body.toString().encodeToByteArray()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun loadPaired() {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PAIRED, "[]").orEmpty()
        paired.value = runCatching {
            val array = org.json.JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                PairedDevice(item.getString("id"), item.getString("name"), item.optString("token"))
            }
        }.onFailure { Log.e(TAG, "Stored paired devices could not be read.", it) }.getOrDefault(emptyList())
        refreshOnline()
    }

    private fun persist(devices: List<PairedDevice>) {
        paired.value = devices.distinctBy(PairedDevice::id)
        val array = org.json.JSONArray()
        paired.value.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name).put("token", it.pairingToken))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PAIRED, array.toString())
            .apply()
        refreshOnline()
        if (paired.value.isNotEmpty() || pairingCode != null) registerService() else unregisterService()
    }

    private fun refreshOnline() {
        online.value = paired.value.filter { endpoints.containsKey(it.id) }
    }
}
