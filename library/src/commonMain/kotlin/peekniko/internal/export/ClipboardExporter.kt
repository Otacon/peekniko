package peekniko.internal.export

import peekniko.NetworkCall

internal interface ClipboardExporter {

    fun export(call: NetworkCall)

}
