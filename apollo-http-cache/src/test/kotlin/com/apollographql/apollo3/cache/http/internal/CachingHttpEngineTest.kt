package com.apollographql.apollo3.cache.http.internal

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.api.http.withHeader
import com.apollographql.apollo3.cache.http.CachingHttpEngine
import com.apollographql.apollo3.cache.http.DiskLruHttpCache
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CachingHttpEngineTest {
  lateinit var mockServer: MockServer
  lateinit var engine: CachingHttpEngine

  @Before
  fun before() {
    mockServer = MockServer()
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    engine = CachingHttpEngine(dir, Long.MAX_VALUE)
  }

  @Test
  fun successResponsesAreCached() {
    mockServer.enqueue(MockResponse(statusCode = 200, body = "success"))

    runBlocking {
      val request = HttpRequest(
          method = HttpMethod.Get,
          url = mockServer.url(),
          body = null,
          headers = emptyList()
      )
      var response = engine.execute(request)
      assertEquals("success", response.body?.readUtf8())

      // 2nd request should hit the cache
      response = engine.execute(request.withHeader(CachingHttpEngine.CACHE_FETCH_POLICY_HEADER, CachingHttpEngine.CACHE_ONLY))
      assertEquals("success", response.body?.readUtf8())
      assertEquals("true", response.headers.valueOf(CachingHttpEngine.FROM_CACHE))
    }
  }

  @Test
  fun failureResponsesAreNotCached() {
    mockServer.enqueue(MockResponse(statusCode = 500, body = "error"))

    runBlocking {
      val request = HttpRequest(
          method = HttpMethod.Get,
          url = mockServer.url(),
          body = null,
          headers = emptyList()
      )

      // Warm the cache
      val response = engine.execute(request)
      assertEquals("error", response.body?.readUtf8())

      // 2nd request should trigger a cache miss
      assertFailsWith(HttpCacheMissException::class) {
        engine.execute(request.withHeader(CachingHttpEngine.CACHE_FETCH_POLICY_HEADER, CachingHttpEngine.CACHE_ONLY))
      }
    }
  }

  @Test
  fun timeoutWorks() {
    mockServer.enqueue(MockResponse(statusCode = 200, body = "success"))

    runBlocking {
      val request = HttpRequest(
          method = HttpMethod.Get,
          url = mockServer.url(),
          body = null,
          headers = emptyList()
      )

      // Warm the cache
      var response = engine.execute(request)
      assertEquals("success", response.body?.readUtf8())

      // 2nd request should hit the cache
      response = engine.execute(request.withHeader(CachingHttpEngine.CACHE_FETCH_POLICY_HEADER, CachingHttpEngine.CACHE_ONLY))
      assertEquals("success", response.body?.readUtf8())

      delay(1000)
      // 3rd request with a 500ms timeout should miss
      assertFailsWith(HttpCacheMissException::class) {
        engine.execute(
            request.withHeader(CachingHttpEngine.CACHE_FETCH_POLICY_HEADER, CachingHttpEngine.CACHE_ONLY)
                .withHeader(CachingHttpEngine.CACHE_EXPIRE_TIMEOUT_HEADER, "500")
        )
      }
    }
  }
}