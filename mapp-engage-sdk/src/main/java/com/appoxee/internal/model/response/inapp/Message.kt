package com.appoxee.internal.model.response.inapp

abstract class Message(
    open val originalEventId: String,
    open val originalEventKey: String,
    open val templateId: Long,
    open val content: String,
    open val type: InappType,
    open val behaviour: Behaviour?,
    open val location: Location?,
)
