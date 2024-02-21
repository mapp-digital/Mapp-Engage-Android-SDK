package com.appoxee.internal.model.response.inapp

abstract class Message(
    open val templateId: String,
    open val content: String,
    open val type: Int,
    open val behaviour: Behaviour?,
    open val location: Location?,
)
