package com.echonow.data.remote.streaming

expect object StreamingCrypto {
    fun aesDecryptCbc(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun aesEncryptCbc(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun aesDecryptGcm(ciphertextWithTag: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun sha256(data: ByteArray): ByteArray
    fun sha512(data: ByteArray): ByteArray
    fun md5(data: ByteArray): ByteArray
    fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray
    fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLengthBytes: Int, algorithm: String): ByteArray
}
