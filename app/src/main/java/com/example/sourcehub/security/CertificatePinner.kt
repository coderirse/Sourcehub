package com.example.sourcehub.security

import okhttp3.CertificatePinner as OkHttpCertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object CertificatePinner {

    fun buildPinnedClient(): OkHttpClient {
        val certificatePinner = OkHttpCertificatePinner.Builder()
            // MVP: placeholder pins — replace with real server pins before production
            .add("api.sourcehub.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
