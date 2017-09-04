package space.leje.musicboxcontrol

/**
 * Created by eugene on 30.08.17.
 */
data class HttpService(
        val name: String,
        val hostIp: String,
        val port: Int
) {
    override fun toString(): String = name
}