package com.example.escaperoomtimer.util

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/** Returns the most useful private IPv4 address for local Wi-Fi access. */
fun localIpv4Address(): String {
    return runCatching {
        val candidates = Collections.list(NetworkInterface.getNetworkInterfaces())
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { network ->
                Collections.list(network.inetAddresses)
                    .asSequence()
                    .filterIsInstance<Inet4Address>()
                    .filter { !it.isLoopbackAddress && it.isSiteLocalAddress }
                    .map { address -> network.name to address.hostAddress.orEmpty() }
            }
            .filter { it.second.isNotBlank() }
            .toList()

        candidates.minByOrNull { (name, _) ->
            when {
                name.startsWith("wlan", ignoreCase = true) -> 0
                name.startsWith("wifi", ignoreCase = true) -> 1
                name.startsWith("eth", ignoreCase = true) -> 2
                name.startsWith("ap", ignoreCase = true) -> 3
                else -> 10
            }
        }?.second ?: "IP 확인 불가"
    }.getOrDefault("IP 확인 불가")
}
