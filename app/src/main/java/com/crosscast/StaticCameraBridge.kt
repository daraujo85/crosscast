package com.crosscast

object StaticCameraBridge {
    var onFrame: ((ByteArray) -> Unit)? = null
}
