package com.campilot

object StaticCameraBridge {
    var onFrame: ((ByteArray) -> Unit)? = null
}
