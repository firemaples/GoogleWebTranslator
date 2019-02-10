package com.firemaples.googlewebtranslator

import kotlinx.coroutines.*

val threadUI = Dispatchers.Main

fun CoroutineDispatcher.launch(block: suspend CoroutineScope.() -> Unit) =
        GlobalScope.launch(this, block = block)