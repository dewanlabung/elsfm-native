package com.elsfm.mobile.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkInfo

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkMonitorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val shadowConnectivityManager: ShadowConnectivityManager = shadowOf(connectivityManager)

    @Test
    fun isOnlineEmitsFalseWhenNoActiveNetwork() = runTest {
        val monitor = DefaultNetworkMonitor(context)

        assertFalse(monitor.isOnline.first())
    }

    @Test
    fun isOnlineEmitsTrueWhenAnActiveNetworkWithInternetCapabilityExists() = runTest {
        // ShadowConnectivityManager.getActiveNetwork() resolves via
        // netIdToNetwork[activeNetworkInfo.type] - the shadow network's netId
        // must therefore match the active NetworkInfo's type for the two to
        // line up, which is why both use TYPE_WIFI below.
        val networkInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED,
            ConnectivityManager.TYPE_WIFI,
            0,
            true,
            true,
        )
        val network: Network = ShadowNetwork.newInstance(ConnectivityManager.TYPE_WIFI)
        val capabilities: NetworkCapabilities = Shadow.newInstanceOf(NetworkCapabilities::class.java)
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        shadowConnectivityManager.addNetwork(network, networkInfo)
        shadowConnectivityManager.setNetworkCapabilities(network, capabilities)
        shadowConnectivityManager.setActiveNetworkInfo(networkInfo)
        shadowConnectivityManager.setDefaultNetworkActive(true)

        val monitor = DefaultNetworkMonitor(context)

        assertTrue(monitor.isOnline.first())
    }
}
