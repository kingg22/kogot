package io.github.kingg22.jextract.gradle.internal

import java.io.File
import java.security.MessageDigest

@Throws(IllegalStateException::class)
fun verifySha256(dest: File, fileName: String, expected: String) {
    val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
    dest.inputStream().buffered().use { stream ->
        val buf = ByteArray(8192)
        var n = stream.read(buf)
        while (n != -1) {
            digest.update(buf, 0, n)
            n = stream.read(buf)
        }
    }
    val actual = digest.digest().joinToString("") { "%02x".format(it) }
    check(actual.equals(expected, ignoreCase = true)) {
        "SHA-256 mismatch for $fileName!\n  expected: $expected\n  actual:   $actual"
    }
}
