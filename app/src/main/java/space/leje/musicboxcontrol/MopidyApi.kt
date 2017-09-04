package space.leje.musicboxcontrol

import io.reactivex.Observable
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by eugene on 26.08.17.
 */
interface MopidyApi {
    @POST("/mopidy/rpc")
    fun send(@Body body: JSONRPCBody): Observable<JSONRPCResult>

    companion object Factory {
        fun create(baseUrl: String): MopidyApi {
            val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()

            return retrofit.create(MopidyApi::class.java)
        }
    }
}