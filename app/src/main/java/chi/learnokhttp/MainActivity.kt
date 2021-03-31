package chi.learnokhttp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"

        val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
        val MEDIA_TYPE_MARKDOWN = "text/x-markdown; charset=utf-8".toMediaType()
        val MEDIA_TYPE_PNG = "image/png".toMediaType()
    }

    private val client = OkHttpClient()

    private val callback = object : Callback {

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (response.isSuccessful) {
                    Log.i(TAG, "Response body: ${response.body!!.string()}")
                } else {
                    Log.i(TAG, "Response code: ${response.code}")
                }
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.i(TAG, "onFailure: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        handleAuthentication()
    }

    private fun synchronousGet() {
        thread {
            val request = Request.Builder()
                .url("https://publicobject.com/helloworld.txt")
                .build()

            val response = client.newCall(request).execute()

            response.use {
                response.use {
                    if (response.isSuccessful) {
                        Log.i(TAG, "Response body: ${response.body!!.string()}")
                    } else {
                        Log.i(TAG, "Response code: ${response.code}")
                    }
                }
            }
        }
    }

    private fun asynchronousGet() {
        val request = Request.Builder()
            .url("https://publicobject.com/helloworld.txt")
            .build()

        client.newCall(request).enqueue(callback)
    }

    private fun header() {
        val request = Request.Builder()
            .url("https://api.github.com/repos/square/okhttp/issues")
            .header("User-Agent", "OkHttp Headers.java")
            .addHeader("Accept", "application/json; q=0.5")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val headers = response.headers
                        for (header in headers) {
                            Log.i(TAG, "${header.first}: ${header.second}")
                        }

                        val server = response.header("Server")
                        Log.i(TAG, "Response header server: $server")

                        val date = response.header("Date")
                        Log.i(TAG, "Response header date: $date")

                        val varyList = response.headers("Vary")
                        for ((index, vary) in varyList.withIndex()) {
                            Log.i(TAG, "Response header vary $index: $vary")
                        }
                    } else {
                        Log.i(TAG, "Response code: ${response.code}")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.i(TAG, "onFailure: ${e.message}")
            }
        })
    }

    private fun postString() {
        val content = """
        |Releases
        |--------
        |
        | * _1.0_ May 6, 2013
        | * _1.1_ June 15, 2013
        | * _1.2_ August 11, 2013
        |""".trimMargin()

        val request = Request.Builder()
            .url("https://api.github.com/markdown/raw")
            .post(content.toRequestBody(MEDIA_TYPE_MARKDOWN))
            .build()

        client.newCall(request).enqueue(callback)
    }

    private fun postJson(url: String, json: String) {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).enqueue(callback)
    }

    private fun postStream() {
        val requestBody = object : RequestBody() {

            override fun contentType(): MediaType = MEDIA_TYPE_MARKDOWN

            override fun writeTo(sink: BufferedSink) {
                val outputStream = sink.outputStream()
                for (i in 0..100) {
                    outputStream.write("Number $i\n".toByteArray())
                }
            }
        }

        val request = Request.Builder()
            .url("https://api.github.com/markdown/raw")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(callback)
    }

    private fun postFile() {
        val file = File(
            getExternalFilesDir(null),
            "README.md"
        )

        val request = Request.Builder()
            .url("https://api.github.com/markdown/raw")
            .post(file.asRequestBody(MEDIA_TYPE_MARKDOWN))
            .build()

        client.newCall(request).enqueue(callback)
    }

    private fun postFormBody() {
        val formBody = FormBody.Builder()
            .add("search", "Jurassic Park")
            .build()

        val request = Request.Builder()
            .url("https://en.wikipedia.org/w/index.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(callback)
    }

    private fun postMultipartBody() {
        val file = File(
            getExternalFilesDir(null),
            "logo-square.png"
        )

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", "Square Logo")
            .addFormDataPart(
                "image",
                "logo-square.png",
                file.asRequestBody(MEDIA_TYPE_PNG)
            )
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/image")
            .header("Authorization", "Client-ID 9199fdef135c122")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(callback)
    }

    private fun responseCache() {
        thread {
            val client = OkHttpClient.Builder()
                .cache(
                    Cache(
                        directory = File(getExternalFilesDir(null), "cache"),
                        maxSize = 10 * 1024 * 1024
                    )
                )
                .build()

            val request = Request.Builder()
                .url("http://publicobject.com/helloworld.txt")
                .cacheControl(CacheControl.FORCE_CACHE)
                .build()

            client.newCall(request).execute().use {
                if (it.isSuccessful) {
                    Log.i(TAG, "Response: $it")
                    Log.i(TAG, "Cache: ${it.cacheResponse}")
                    Log.i(TAG, "Network: ${it.networkResponse}")
                } else {
                    Log.i(TAG, "Response code: ${it.code}")
                }
            }

            client.newCall(request).execute().use {
                if (it.isSuccessful) {
                    Log.i(TAG, "Response: $it")
                    Log.i(TAG, "Cache: ${it.cacheResponse}")
                    Log.i(TAG, "Network: ${it.networkResponse}")
                } else {
                    Log.i(TAG, "Response code: ${it.code}")
                }
            }
        }
    }

    private fun cancel() {
        val request = Request.Builder()
            .url("http://httpbin.org/delay/2")
            .build()

        val call = client.newCall(request)

        call.enqueue(callback)

        thread {
            Thread.sleep(1000)
            call.cancel()
        }
    }

    private fun timeout() {
        thread {
            val client = OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("http://httpbin.org/delay/2")
                .build()

            client.newCall(request).enqueue(callback)
        }
    }

    private fun perCallConfiguration() {
        val request = Request.Builder()
            .url("http://httpbin.org/delay/1")
            .build()

        client.newBuilder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .build()
            .newCall(request)
            .enqueue(callback)

        client.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .build()
            .newCall(request)
            .enqueue(callback)
    }

    private fun handleAuthentication() {
        val client = OkHttpClient.Builder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") == null) {
                    response.request.newBuilder()
                        .header("Authorization", Credentials.basic("jesse", "password1"))
                        .build()
                } else {
                    null
                }
            }
            .build()

        val request = Request.Builder()
            .url("http://publicobject.com/secrets/hellosecret.txt")
            .build()

        client.newCall(request).enqueue(callback)
    }
}