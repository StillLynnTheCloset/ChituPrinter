package com.stilllynnthecloset.sdcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main() {
    runBlocking {
        withContext(Dispatchers.IO) {
//            Helper.refreshAttributes()
        }
    }
}
