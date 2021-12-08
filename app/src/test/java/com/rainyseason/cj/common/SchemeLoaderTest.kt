package com.rainyseason.cj.common

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock

class SchemeLoaderTest {

    private val intIdSelector = object : SchemeLoader.IdSelector<Int, Int> {
        override fun invoke(key: Int): Int {
            return key
        }
    }

    @Test
    fun `first page only`() = runBlocking {
        val localSource: SchemeLoader.LocalSource<Unit, Int> = mock {
            onBlocking {
                get(Unit)
            }.thenReturn(null)
        }
        val remoteSource: SchemeLoader.RemoteSource<Int> = mock {
            onBlocking {
                get(0, 10)
            }.thenReturn((0..8).toList())
        }

        val loader = SchemeLoader(localSource, remoteSource, intIdSelector)
        loader.invoke(Unit, listOf(10))

        inOrder(localSource, remoteSource) {
            verify(localSource).get(Unit)
            verify(remoteSource).get(0, 10)
            verify(localSource).set(Unit, (0..8).toList())
        }
    }

    @Test
    fun `first page then half seconds page`() = runBlocking {
        val localSource: SchemeLoader.LocalSource<Unit, Int> = mock {
            onBlocking {
                get(Unit)
            }.thenReturn(null)
        }
        val remoteSource: SchemeLoader.RemoteSource<Int> = mock {
            onBlocking {
                get(0, 10)
            }.thenReturn((0..9).toList())
            onBlocking {
                get(10, 10)
            }.thenReturn((10..12).toList())
        }

        val loader = SchemeLoader(localSource, remoteSource, intIdSelector)
        loader.invoke(Unit, listOf(10))

        inOrder(localSource, remoteSource) {
            verify(localSource).get(Unit)
            verify(remoteSource).get(0, 10)
            verify(localSource).set(Unit, (0..9).toList())
            verify(remoteSource).get(10, 10)
            verify(localSource).set(Unit, (0..12).toList())
        }
    }

    @Test
    fun `first seconds, third page empty`() = runBlocking {
        val localSource: SchemeLoader.LocalSource<Unit, Int> = mock {
            onBlocking {
                get(Unit)
            }.thenReturn(null)
        }
        val remoteSource: SchemeLoader.RemoteSource<Int> = mock {
            onBlocking {
                get(0, 10)
            }.thenReturn((0..9).toList())
            onBlocking {
                get(10, 10)
            }.thenReturn((10..19).toList())
            onBlocking {
                get(20, 10)
            }.thenReturn(emptyList())
        }

        val loader = SchemeLoader(localSource, remoteSource, intIdSelector)
        loader.invoke(Unit, listOf(10))

        inOrder(localSource, remoteSource) {
            verify(localSource).get(Unit)
            verify(remoteSource).get(0, 10)
            verify(localSource).set(Unit, (0..9).toList())
            verify(remoteSource).get(10, 10)
            verify(localSource).set(Unit, (0..19).toList())
            verify(remoteSource).get(20, 10)
        }
    }

    @Test
    fun `first seconds third page half`() = runBlocking {
        val localSource: SchemeLoader.LocalSource<Unit, Int> = mock {
            onBlocking {
                get(Unit)
            }.thenReturn(null)
        }
        val remoteSource: SchemeLoader.RemoteSource<Int> = mock {
            onBlocking {
                get(0, 10)
            }.thenReturn((0..9).toList())
            onBlocking {
                get(10, 10)
            }.thenReturn((10..19).toList())
            onBlocking {
                get(20, 10)
            }.thenReturn((20..25).toList())
        }

        val loader = SchemeLoader(localSource, remoteSource, intIdSelector)
        loader.invoke(Unit, listOf(10))

        inOrder(localSource, remoteSource) {
            verify(localSource).get(Unit)
            verify(remoteSource).get(0, 10)
            verify(localSource).set(Unit, (0..9).toList())
            verify(remoteSource).get(10, 10)
            verify(localSource).set(Unit, (0..19).toList())
            verify(remoteSource).get(20, 10)
            verify(localSource).set(Unit, (0..25).toList())
        }
    }

    @Test
    fun `old source distinct`() = runBlocking {
        val localSource: SchemeLoader.LocalSource<Unit, Int> = mock {
            onBlocking {
                get(Unit)
            }.thenReturn(listOf(0, 2, 4, 6, 8, 10))
        }
        val remoteSource: SchemeLoader.RemoteSource<Int> = mock {
            onBlocking {
                get(0, 10)
            }.thenReturn((0..9).toList())
            onBlocking {
                get(10, 10)
            }.thenReturn((10..19).toList())
            onBlocking {
                get(20, 10)
            }.thenReturn((20..25).toList())
        }

        val loader = SchemeLoader(localSource, remoteSource, intIdSelector)
        loader.invoke(Unit, listOf(10))

        inOrder(localSource, remoteSource) {
            verify(localSource).get(Unit)
            verify(remoteSource).get(0, 10)
            verify(localSource).set(Unit, (0..10).toList())
            verify(remoteSource).get(10, 10)
            verify(localSource).set(Unit, (0..19).toList())
            verify(remoteSource).get(20, 10)
            verify(localSource).set(Unit, (0..25).toList())
        }
    }

    @Test
    fun `old list != new list`() = runBlocking {
        val localSource: SchemeLoader.LocalSource<Unit, Int> = mock {
            onBlocking {
                get(Unit)
            }.thenReturn((0..25).toList() + listOf(100))
        }
        val remoteSource: SchemeLoader.RemoteSource<Int> = mock {
            onBlocking {
                get(0, 10)
            }.thenReturn((0..9).toList())
            onBlocking {
                get(10, 10)
            }.thenReturn((10..19).toList())
            onBlocking {
                get(20, 10)
            }.thenReturn((20..25).toList())
        }

        val loader = SchemeLoader(localSource, remoteSource, intIdSelector)
        loader.invoke(Unit, listOf(10))

        inOrder(localSource, remoteSource) {
            verify(localSource).get(Unit)
            verify(remoteSource).get(0, 10)
            verify(localSource).set(Unit, (0..25).toList() + listOf(100))
            verify(remoteSource).get(10, 10)
            verify(localSource).set(Unit, (0..25).toList() + listOf(100))
            verify(remoteSource).get(20, 10)
            verify(localSource).set(Unit, (0..25).toList() + listOf(100))
            verify(localSource).set(Unit, (0..25).toList())
        }
    }

    @Test
    fun `incremental load`() = runBlocking {
        val localSource: SchemeLoader.LocalSource<Unit, Int> = mock {
            onBlocking {
                get(Unit)
            }.thenReturn((0..25).toList() + listOf(100))
        }
        val remoteSource: SchemeLoader.RemoteSource<Int> = mock {
            onBlocking {
                get(0, 1)
            }.thenReturn(listOf(0))
            onBlocking {
                get(1, 10)
            }.thenReturn((1..10).toList())
            onBlocking {
                get(11, 10)
            }.thenReturn((11..15).toList())
        }

        val loader = SchemeLoader(localSource, remoteSource, intIdSelector)
        loader.invoke(Unit, listOf(1, 10))

        inOrder(localSource, remoteSource) {
            verify(localSource).get(Unit)
            verify(remoteSource).get(0, 1)
            verify(localSource).set(Unit, (0..25).toList() + listOf(100))
            verify(remoteSource).get(1, 10)
            verify(localSource).set(Unit, (0..25).toList() + listOf(100))
            verify(remoteSource).get(11, 10)
            verify(localSource).set(Unit, (0..25).toList() + listOf(100))
            verify(localSource).set(Unit, (0..15).toList())
        }
    }
}
