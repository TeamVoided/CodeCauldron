package org.teamvoided.template.client

import org.teamvoided.template.Template

@Suppress("unused")
object TemplateClient {
    fun init() {
        Template.log.info("Hello from Client")
    }
}