package com.apollographql.apollo3.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  return requested ?: Dispatchers.IO
}

actual class BackgroundDispatcher actual constructor() {
  private var disposed = false
  private val _dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  actual val coroutineDispatcher: CoroutineDispatcher
    get() = _dispatcher

  actual fun dispose() {
    if(!disposed) {
      _dispatcher.close()
    }
  }
}

actual class DefaultMutex : Mutex {
  val lock = Object()

  override fun <T> lock(block: () -> T): T {
    return synchronized(lock) {
      block()
    }
  }
}