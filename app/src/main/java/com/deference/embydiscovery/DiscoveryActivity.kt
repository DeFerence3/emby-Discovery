package com.deference.embydiscovery

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

class DiscoveryActivity : ComponentActivity() {

    private var found by mutableStateOf(false)
    private var isSearching by mutableStateOf(true)
    private var statusMessage by mutableStateOf("Searching local network...")
    private var serverIp by mutableStateOf("")
	private var discoveryManager: NsdDiscoveryManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
		initializeDiscoveryManager()
		discoveryManager?.startDiscovery()
        setContent {
            EmbyDiscoveryTheme {
                DiscoveryScreen()
            }
        }
    }

    private fun discoverService() {
        stopDiscovery()
        found = false
        isSearching = true
        statusMessage = "Searching local network..."
        serverIp = ""
		initializeDiscoveryManager()
		discoveryManager?.startDiscovery()
    }

    private fun stopDiscovery() {
        discoveryManager?.stopDiscovery()
		discoveryManager = null
    }
	
	private fun initializeDiscoveryManager() {
		try {
			discoveryManager = NsdDiscoveryManager(
				context = this,
				listener = object : NsdDiscoveryManager.DiscoveryListener {
					override fun onServerDiscovered(name: String, address: String) {
						runOnUiThread {
							serverIp = address
							found = true
							isSearching = false
							statusMessage = "Emby server found!"
						}
					}
					
					override fun onServerLost(name: String) { /* Ignore */ }
					
					override fun onDiscoveryStarted() {
						runOnUiThread {
							statusMessage = "Network scan active..."
						}
					}
					
					override fun onDiscoveryStopped() {
						runOnUiThread {
							isSearching = false
						}
					}
					
					override fun onDiscoveryError(error: String) {
						runOnUiThread {
							isSearching = false
							statusMessage = "Resolve failed ($error)"
						}
					}
				}
			)
			runOnUiThread {
				Log.d(TAG, "NsdDiscoveryManager initialized")
			}
		} catch (e: Exception) {
			Log.e(TAG, "Failed to initialize discovery manager", e)
		}
	}

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager?.stopDiscovery()
		discoveryManager?.cleanup()
    }

    private fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Failed to open browser: ${e.message}")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun DiscoveryScreen() {
        val context = LocalContext.current
        val clipboardManager = LocalClipboard.current
        var manualIp by remember { mutableStateOf("") }
        var manualPort by remember { mutableStateOf("8096") }
		val scope = rememberCoroutineScope()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "EMBY DISCOVERY",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color(0xFF52B54B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isSearching) {
                            CircularProgressIndicator(
                                color = Color(0xFF52B54B),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        if (found) {
                            val resolvedUrl = "http://$serverIp"
							SelectionContainer {
								Text(
									text = resolvedUrl,
									style = MaterialTheme.typography.titleLarge.copy(
										fontWeight = FontWeight.SemiBold
									),
									color = Color(0xFF52B54B),
									textAlign = TextAlign.Center
								)
							}

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { openBrowser(resolvedUrl) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF52B54B),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open in Browser")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
											val clipData = ClipData.newRawUri("Emby Ip", resolvedUrl.toUri())
											scope.launch {
												clipboardManager.setClipEntry(ClipEntry(clipData))
											}
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF52B54B)
                                        )
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy URL")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, resolvedUrl)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share URL"))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF52B54B)
                                        )
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }

                        if (!isSearching) {
                            Button(
                                onClick = { discoverService() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF52B54B),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Network")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Manual Connection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Server IP Address") },
                            placeholder = { Text("e.g. 192.168.1.10") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port") },
                            placeholder = { Text("8096") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (manualIp.isNotBlank()) {
                                    val port = manualPort.toIntOrNull() ?: 8096
                                    openBrowser("http://$manualIp:$port")
                                } else {
                                    showToast("Enter IP address")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF52B54B)
                            )
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
	
	companion object {
		const val TAG = "DiscoveryActivity"
	}
}

@Composable
fun EmbyDiscoveryTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF52B54B),
        secondary = Color(0xFF52B54B),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF2A2A2A),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFFE5E5E5),
        onSurface = Color(0xFFE5E5E5),
        onSurfaceVariant = Color(0xFFC5C5C5)
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}