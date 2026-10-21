package com.echonow.data.remote.streaming

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object StreamingCrypto {
    actual fun aesDecryptCbc(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    actual fun aesEncryptCbc(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(plaintext)
    }

    actual fun aesDecryptGcm(ciphertextWithTag: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertextWithTag)
    }

    actual fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    actual fun sha512(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-512").digest(data)
    }

    actual fun md5(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("MD5").digest(data)
    }

    actual fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        return mac.doFinal(data)
    }

    actual fun pbkdf2(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int,
        algorithm: String,
    ): ByteArray {
        val algoName = when (algorithm.uppercase()) {
            "SHA-256" -> "PBKDF2WithHmacSHA256"
            "SHA-512" -> "PBKDF2WithHmacSHA512"
            else -> "PBKDF2WithHmacSHA1"
        }
        val spec = PBEKeySpec(
            String(password, Charsets.UTF_8).toCharArray(),
            salt,
            iterations,
            keyLengthBytes * 8,
        )
        val factory = SecretKeyFactory.getInstance(algoName)
        return factory.generateSecret(spec).encoded
    }
}
