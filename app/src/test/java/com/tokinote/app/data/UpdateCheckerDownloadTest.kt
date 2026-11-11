package com.tokinote.app.data

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * UpdateChecker.download 的回归测试：GitHub releases/download 固定先回 302
 * 再跳到 release-assets 的签名地址，下载必须手动跟跳转，否则组件包
 * （和 APK 更新）会在这里悄悄失败。
 */
class UpdateCheckerDownloadTest {

    private lateinit var server: HttpServer
    private var port: Int = 0
    private val payload = "focus-component-1.0".toByteArray(Charsets.UTF_8)
    private val tmpFiles = mutableListOf<File>()

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        port = server.address.port
        server.createContext("/redirect") { exchange ->
            exchange.responseHeaders.add("Location", "/file")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/file") { exchange ->
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.createContext("/missing") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.createContext("/truncated") { exchange ->
            // 声明 100 字节，实际只写 payload 那么多就断开。
            exchange.sendResponseHeaders(200, 100)
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()
    }

    @After
    fun stopServer() {
        server.stop(0)
        tmpFiles.forEach { it.delete() }
    }

    private fun tmpFile(): File {
        val file = File.createTempFile("download-test", ".bin")
        tmpFiles += file
        return file
    }

    private fun url(path: String) = "http://127.0.0.1:$port$path"

    @Test
    fun `follows redirect to the file`() {
        val destination = tmpFile()
        val seen = mutableListOf<Int>()
        assertTrue(UpdateChecker.download(url("/redirect"), destination) { seen += it })
        assertArrayEquals(payload, destination.readBytes())
        assertTrue(seen.isNotEmpty())
        assertTrue(seen.last() == 100)
    }

    @Test
    fun `http error throws instead of writing a partial file`() {
        val destination = tmpFile()
        assertThrows(IOException::class.java) {
            UpdateChecker.download(url("/missing"), destination) {}
        }
    }

    @Test
    fun `truncated body throws instead of passing checksum`() {
        val destination = tmpFile()
        assertThrows(IOException::class.java) {
            UpdateChecker.download(url("/truncated"), destination) {}
        }
    }
}
