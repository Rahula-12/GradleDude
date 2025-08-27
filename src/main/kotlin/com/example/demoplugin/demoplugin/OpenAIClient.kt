package com.example.demoplugin.demoplugin

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object OpenAIClient {

    private val httpClient by lazy{
        getUnsafeOkHttpClient()
    }
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        // Build the OkHttpClient
        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
    fun generateJoke(callback:(String)->Unit) {
        val request = Request.Builder()
            .url("https://icanhazdadjoke.com")
            .addHeader("Accept", "application/json")
            .addHeader("Connection","keep-alive")
            .build()

        httpClient.newCall(request).enqueue(responseCallback = object:Callback{
            override fun onFailure(call: Call, e: IOException) {
                callback("Network Call failed with ${e.message} exception.")
            }

            override fun onResponse(call: Call, response: Response) {
                when(response.code) {
                    in 200..299->{
                        val jsonObject= JSONObject(response.body?.string())
                        println(jsonObject.toString())
                        callback(jsonObject.getString("joke"))
                    }
                    else ->callback("Network Call failed with ${response.code}")
                }
            }

        })
    }

}