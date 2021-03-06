/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import java.io.IOException

class DeferredTest {
  @get:Rule val server = MockWebServer()

  private lateinit var service: Service

  interface Service {
    @GET("/") fun body(): Deferred<String>
    @GET("/") fun emptyBody(): Deferred<Unit>
    @GET("/") fun response(): Deferred<Response<String>>
  }

  @Before fun setUp() {
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(StringConverterFactory())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()
    service = retrofit.create(Service::class.java)
  }

  @Test fun bodySuccess200() = runBlocking {
    server.enqueue(MockResponse().setBody("Hi"))

    val deferred = service.body()
    assertThat(deferred.await()).isEqualTo("Hi")
  }

  @Test fun bodySuccess404() = runBlocking {
    server.enqueue(MockResponse().setResponseCode(404))

    val deferred = service.body()
    try {
      deferred.await()
      fail()
    } catch (e: HttpException) {
      assertThat(e).hasMessageThat().isEqualTo("HTTP 404 Client Error")
    }
  }

  @Test fun bodyFailure() = runBlocking {
    server.enqueue(MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST))

    val deferred = service.body()
    try {
      deferred.await()
      fail()
    } catch (e: IOException) {
    }
  }

  @Test fun emptyBodySuccess200() = runBlocking {
    server.enqueue(MockResponse())

    val deferred = service.emptyBody()
    assertThat(deferred.await()).isEqualTo(Unit)
  }

  @Test fun emptyBodySuccess404() = runBlocking {
    server.enqueue(MockResponse().setResponseCode(404))

    val deferred = service.emptyBody()
    try {
      deferred.await()
      fail()
    } catch (e: HttpException) {
      assertThat(e).hasMessageThat().isEqualTo("HTTP 404 Client Error")
    }
  }

  @Test fun emptyBodyFailure() = runBlocking {
    server.enqueue(MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST))

    val deferred = service.emptyBody()
    try {
      deferred.await()
      fail()
    } catch (e: IOException) {
    }
  }

  @Test fun responseSuccess200() = runBlocking {
    server.enqueue(MockResponse().setBody("Hi"))

    val deferred = service.response()
    val response = deferred.await()
    assertThat(response.isSuccessful).isTrue()
    assertThat(response.body()).isEqualTo("Hi")
  }

  @Test fun responseSuccess404() = runBlocking {
    server.enqueue(MockResponse().setResponseCode(404).setBody("Hi"))

    val deferred = service.response()
    val response = deferred.await()
    assertThat(response.isSuccessful).isFalse()
    assertThat(response.errorBody()!!.string()).isEqualTo("Hi")
  }

  @Test fun responseFailure() = runBlocking {
    server.enqueue(MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST))

    val deferred = service.response()
    try {
      deferred.await()
      fail()
    } catch (e: IOException) {
    }
  }
}
