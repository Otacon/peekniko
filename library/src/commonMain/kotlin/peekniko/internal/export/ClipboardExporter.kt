package peekniko.internal.export

import peekniko.NetworkCall

internal interface ClipboardExporter {

    suspend fun export(call: NetworkCall)

}
