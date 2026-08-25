package com.deference.embydiscovery

import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import com.deference.embydiscovery.Const.SERVICE_TYPE

class DiscoveryActivity : ComponentActivity() {
    private var nsdManager: NsdManager? = null
    private var discoveryListener: DiscoveryListener? = null

    private var found = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        discoverService()
		setContent{
		
		}
    }

    private fun discoverService() {
        nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        discoveryListener = object : DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String?) {
                showToast("Discovery started")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                nsdManager!!.resolveService(serviceInfo, resolveListener)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) { /* ignore */
            }

            override fun onDiscoveryStopped(serviceType: String?) {}
            override fun onStartDiscoveryFailed(s: String?, error: Int) {
                fail()
            }

            override fun onStopDiscoveryFailed(s: String?, error: Int) {
                fail()
            }
        }

        nsdManager!!.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        Handler().postDelayed({
            if (!found) {
                showToast("Server not found")
            }
        }, 5000)
    }

    private val resolveListener: NsdManager.ResolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            fail()
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val ip = serviceInfo.host.hostAddress
            val port = serviceInfo.port
            found = true
            showToast("Server found: $ip:$port")
            openBrowser(ip, port)
        }
    }

    private fun openBrowser(ip: String?, port: Int) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            ("http://$ip:$port").toUri()
        )
        startActivity(intent)
        finish()
    }

    private fun fail() {
        showToast("Discovery failed")
        finish()
    }

    private fun showToast(msg: String?) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}