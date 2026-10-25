package com.stilllynnthecloset.sdcp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class MainboardId constructor(val value: String)

@Serializable
@JvmInline
value class MachineId constructor(val value: String)