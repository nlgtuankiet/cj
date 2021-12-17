package com.rainyseason.cj.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.lang.Exception
import java.util.concurrent.CountDownLatch

class CoroutineScopeTest {

    suspend fun sampleError() {
        delay(1)
        throw Exception("test exception")
    }

    @Test
    fun `still active when suspend error`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        val countDown = CountDownLatch(1)
        scope.launch {
            try {
                sampleError()
            } catch (ex: Exception) {
            }
            countDown.countDown()
        }
        countDown.await()
        delay(500)
        Assert.assertEquals(true, scope.isActive)
    }

    @Test
    fun `not active when async error`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        val async = scope.async {
            sampleError()
        }
        try {
            async.await()
        } catch (ex: Exception) {
        }
        delay(500)
        Assert.assertEquals(false, scope.isActive)
    }

    @Test
    fun `active when async error`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val async = scope.async {
            sampleError()
        }
        try {
            async.await()
        } catch (ex: Exception) {
        }
        delay(500)
        Assert.assertEquals(true, scope.isActive)
    }
}
