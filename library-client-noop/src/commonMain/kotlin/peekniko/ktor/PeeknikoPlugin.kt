package peekniko.ktor

import peekniko.Peekniko
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin

public val Peekniko.plugin: ClientPlugin<*> by lazy { createClientPlugin("PeeknikoNoOp") {} }
