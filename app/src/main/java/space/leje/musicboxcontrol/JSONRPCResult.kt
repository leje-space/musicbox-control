package space.leje.musicboxcontrol

/**
 * Created by eugene on 26.08.17.
 */
data class JSONRPCResult(
        val jsonrpc: String,
        val id: Any,
        val result: Any
)