package com.v2ray.ang

import com.v2ray.ang.util.HttpUtil
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HttpUtilTest {

    @Test
    fun testIdnToASCII() {
        // Regular URL remains unchanged
        val regularUrl = "https://example.com/path"
        assertEquals(regularUrl, HttpUtil.toIdnUrl(regularUrl))

        // Non-ASCII URL converts to ASCII (Punycode)
        val nonAsciiUrl = "https://例子.测试/path"
        val expectedNonAscii = "https://xn--fsqu00a.xn--0zwm56d/path"
        assertEquals(expectedNonAscii, HttpUtil.toIdnUrl(nonAsciiUrl))

        // Mixed URL only converts the host part
        val mixedUrl = "https://例子.com/测试"
        val expectedMixed = "https://xn--fsqu00a.com/测试"
        assertEquals(expectedMixed, HttpUtil.toIdnUrl(mixedUrl))

        // URL with Basic Authentication using regular domain
        val basicAuthUrl = "https://user:password@example.com/path"
        assertEquals(basicAuthUrl, HttpUtil.toIdnUrl(basicAuthUrl))

        // URL with Basic Authentication using non-ASCII domain
        val basicAuthNonAscii = "https://user:password@例子.测试/path"
        val expectedBasicAuthNonAscii = "https://user:password@xn--fsqu00a.xn--0zwm56d/path"
        assertEquals(expectedBasicAuthNonAscii, HttpUtil.toIdnUrl(basicAuthNonAscii))

        // URL with non-ASCII username and password
        val nonAsciiAuth = "https://用户:密码@example.com/path"
        // Basic auth credentials should remain unchanged as they're percent-encoded separately
        assertEquals(nonAsciiAuth, HttpUtil.toIdnUrl(nonAsciiAuth))
    }

    @Test
    fun normalizeHeaderValueTrimsAndValidates() {
        assertEquals("abc-123", HttpUtil.normalizeHeaderValue("  abc-123 \n"))
        assertEquals("", HttpUtil.normalizeHeaderValue(""))
        assertEquals("", HttpUtil.normalizeHeaderValue("   "))
        assertEquals("", HttpUtil.normalizeHeaderValue(null))
        assertEquals("Mozilla/5.0 (Linux; Android 14)", HttpUtil.normalizeHeaderValue("Mozilla/5.0 (Linux; Android 14)"))
        assertNull(HttpUtil.normalizeHeaderValue("abc\ndef"))
        assertNull(HttpUtil.normalizeHeaderValue("устройство"))
    }

    @Test
    fun applyDeviceHeadersAddsHwidOsAndModel() {
        val builder = Request.Builder().url("https://example.com/sub")
        HttpUtil.applyDeviceHeaders(builder, "11111111-2222-3333-4444-555555555555")
        val request = builder.build()
        assertEquals("11111111-2222-3333-4444-555555555555", request.header("x-hwid"))
        assertEquals("Android", request.header("x-device-os"))
        assertEquals("Generic", request.header("x-device-model"))
    }

    @Test
    fun applyDeviceHeadersSkipsBlankHwid() {
        listOf(null, "", "  ").forEach { hwid ->
            val builder = Request.Builder().url("https://example.com/sub")
            HttpUtil.applyDeviceHeaders(builder, hwid)
            val request = builder.build()
            assertNull(request.header("x-hwid"))
            assertNull(request.header("x-device-os"))
            assertNull(request.header("x-device-model"))
        }
    }

    @Test
    fun laterHeaderOverridesDeviceHeader() {
        val builder = Request.Builder().url("https://example.com/sub")
        HttpUtil.applyDeviceHeaders(builder, "auto-hwid")
        builder.header("x-hwid", "from-subscription")
        assertEquals("from-subscription", builder.build().header("x-hwid"))
    }
}