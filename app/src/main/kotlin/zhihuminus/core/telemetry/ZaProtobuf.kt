package com.zhihuminus.core.telemetry

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ProtoWriter {
    private val stream = ByteArrayOutputStream()

    fun writeVarint(value: Long) {
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            stream.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        stream.write((v and 0x7F).toInt())
    }

    private fun writeTag(fieldNumber: Int, wireType: Int) {
        writeVarint(((fieldNumber shl 3) or wireType).toLong())
    }

    fun writeInt32(fieldNumber: Int, value: Int) {
        writeTag(fieldNumber, 0)
        writeVarint(value.toLong())
    }

    fun writeInt64(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, 0)
        writeVarint(value)
    }

    fun writeString(fieldNumber: Int, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeTag(fieldNumber, 2)
        writeVarint(bytes.size.toLong())
        stream.write(bytes)
    }

    fun writeBytes(fieldNumber: Int, bytes: ByteArray) {
        writeTag(fieldNumber, 2)
        writeVarint(bytes.size.toLong())
        stream.write(bytes)
    }

    fun writeMessage(fieldNumber: Int, block: ProtoWriter.() -> Unit) {
        val subWriter = ProtoWriter()
        subWriter.block()
        val subBytes = subWriter.toByteArray()
        writeTag(fieldNumber, 2)
        writeVarint(subBytes.size.toLong())
        stream.write(subBytes)
    }

    fun toByteArray(): ByteArray = stream.toByteArray()
}

fun ByteArray.gzipCompress(): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).use { it.write(this) }
    return bos.toByteArray()
}

data class ZaFeedExposureItem(
    val attachedInfo: String,
    val cardIndex: Int,
)

object ZaProtobufBuilder {
    const val LOG_VERSION = "3.5.54"
    const val PAGE_ID_HOME = "10148"

    fun buildBatchPayload(
        items: List<ZaFeedExposureItem>,
        clientId: String,
        dc0Cookie: String,
        userAgent: String,
        timestamp: Long = System.currentTimeMillis(),
        timezone: String = "+0800",
    ): ByteArray {
        val root = ProtoWriter()
        items.forEachIndexed { seq, item ->
            val localIncrementId = seq + 1
            root.writeMessage(1) {
                // ZaLogEntry
                writeString(1, LOG_VERSION)
                writeInt32(2, localIncrementId)
                writeInt32(3, 11) // LogType.Proto3
                writeMessage(9) {
                    // proto3.ZaLogEntry
                    writeString(1, LOG_VERSION)
                    writeInt32(2, localIncrementId)
                    writeInt32(3, 2) // LogType.Show
                    writeMessage(4) {
                        // BaseInfo
                        writeMessage(1) {
                            // IDInfo
                            writeString(4, clientId)
                            writeInt32(5, 1)
                            if (dc0Cookie.isNotBlank()) {
                                writeString(11, dc0Cookie)
                            }
                        }
                        writeMessage(2) {
                            // ClientInfo
                            writeInt32(1, 1) // product = Zhihu
                            writeInt32(2, 1) // platform = DesktopWeb
                            writeString(3, "100.0.0")
                            writeString(5, userAgent)
                        }
                        writeMessage(3) {
                            // TimeInfo
                            writeInt64(1, timestamp)
                            writeString(2, timezone)
                        }
                    }
                    writeMessage(5) {
                        // DetailInfo
                        writeMessage(1) {
                            // ViewInfo
                            writeString(1, "https://www.zhihu.com/")
                            writeString(2, "https://www.zhihu.com/")
                            writeMessage(7) {
                                // ElementLocation
                                writeInt32(1, 8) // Element.Type.Card
                                writeMessage(4) {
                                    // PageInfo
                                    writeString(1, PAGE_ID_HOME)
                                    writeInt32(2, 0)
                                }
                                writeMessage(6) {
                                    // CardInfo
                                    writeInt32(1, item.cardIndex)
                                }
                            }
                        }
                    }
                    writeMessage(6) {
                        // ExtraInfo
                        writeString(11, item.attachedInfo) // attached_info_bytes
                    }
                }
            }
        }
        return root.toByteArray()
    }
}
