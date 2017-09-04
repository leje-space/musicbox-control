package space.leje.musicboxcontrol

import java.util.UUID

/**
 * Created by eugene on 26.08.17.
 */
data class JSONRPCBody(
        val method: String,
        val params: Map<String, Any>? = null
) {
    val jsonrpc = "2.0"
    val id = UUID.randomUUID().toString()
}