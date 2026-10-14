package com.zhihuminus.core.telemetry

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZaProtobufTest {
    @Test
    fun testProtoWriterVarint() {
        val writer = ProtoWriter()
        writer.writeVarint(1L)
        writer.writeVarint(300L)
        val bytes = writer.toByteArray()

        assertEquals(0x01.toByte(), bytes[0])
        // 300 = 0x12c -> 0xac 0x02
        assertEquals(0xac.toByte(), bytes[1])
        assertEquals(0x02.toByte(), bytes[2])
    }

    @Test
    fun testProtoWriterMessage() {
        val writer = ProtoWriter()
        writer.writeMessage(1) {
            writeString(1, "hello")
            writeInt32(2, 42)
        }
        val bytes = writer.toByteArray()
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testGzipCompressAndDecompress() {
        val original = "ZhihuAnalyticsProtobufPayload".toByteArray(Charsets.UTF_8)
        val compressed = original.gzipCompress()

        assertTrue(compressed.size > 2)
        assertEquals(0x1f.toByte(), compressed[0])
        assertEquals(0x8b.toByte(), compressed[1])

        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed)).readBytes()
        assertEquals(original.decodeToString(), decompressed.decodeToString())
    }

    @Test
    fun testBuildBatchPayloadStructure() {
        val items = listOf(
            ZaFeedExposureItem(attachedInfo = "test_attached_info_1", cardIndex = 0),
            ZaFeedExposureItem(attachedInfo = "test_attached_info_2", cardIndex = 1),
        )

        val payload = ZaProtobufBuilder.buildBatchPayload(
            items = items,
            clientId = "test_client_id_uuid",
            dc0Cookie = "test_dc0_value",
            userAgent = "TestAgent/1.0",
        )

        assertTrue(payload.isNotEmpty())
        val compressed = payload.gzipCompress()
        assertTrue(compressed.isNotEmpty())
    }
}
