package com.example.escaperoomtimer.util

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

fun localIpv4Address(): String {
    return runCatching {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { Collections.list(it.inetAddresses).asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
            ?: "IP 확인 불가"
    }.getOrDefault("IP 확인 불가")
}
