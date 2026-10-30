package com.assayflux.healthmonitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureTransferInstrumentedTest {
    private val code = "ABCDEFGHJKLMNPQRSTUVWXYZ2345"

    @Test fun encryptedArchiveRoundTripsSelectedPayload() {
        val payload = "private health data".toByteArray()
        val encrypted = SecureTransfer.createEncryptedArchive(mapOf("vitals.json" to payload), code)
        val opened = SecureTransfer.openEncryptedArchive(encrypted, code)
        assertArrayEquals(payload, opened.getValue("records/vitals.json"))
    }

    @Test fun incorrectImportCodeIsRejected() {
        val encrypted = SecureTransfer.createEncryptedArchive(mapOf("members.json" to byteArrayOf(1,2,3)), code)
        assertThrows(IllegalArgumentException::class.java) {
            SecureTransfer.openEncryptedArchive(encrypted, "ZZZZZZZZZZZZZZZZZZZZ")
        }
    }

    @Test fun modifiedCiphertextIsRejected() {
        val encrypted = SecureTransfer.createEncryptedArchive(mapOf("labs.json" to byteArrayOf(4,5,6)), code)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            SecureTransfer.openEncryptedArchive(encrypted, code)
        }
    }

    @Test fun archiveRejectsPathTraversal() {
        assertThrows(IllegalArgumentException::class.java) {
            SecureTransfer.createEncryptedArchive(mapOf("../escape" to byteArrayOf(1)), code)
        }
    }
}
