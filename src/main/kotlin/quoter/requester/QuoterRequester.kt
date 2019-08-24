package quoter.requester

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.call
import io.ktor.client.call.receive
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import quoter.util.JsonObjectBuilder
import quoter.util.json
import java.lang.IllegalArgumentException

typealias RequestBuilder = HttpRequestBuilder.() -> Unit

class QuoterRequester<T: HttpClientEngineConfig>(val baseUrl: String, factory: HttpClientEngineFactory<T>, engineConfig: T.() -> Unit) {

    val client = HttpClient(factory) {
        install(JsonFeature)
        engine { engineConfig() }
    }

    suspend fun call(path: String, method: HttpMethod, body: RequestBuilder = {}): HttpClientCall {
        return client.call {
            url.takeFrom("$baseUrl${path.removePrefix("/")}")
            this.method = method
            body()
        }
    }

    suspend inline fun <reified T> get(path: String, noinline params: JsonObjectBuilder.() -> Unit, noinline body: RequestBuilder = {}): T = getCall(path, params, body).receive()

    suspend inline fun <reified T> post(path: String, noinline params:JsonObjectBuilder.() -> Unit = {}, noinline body:RequestBuilder = {}): T = postCall(path, params, body).receive()
    suspend inline fun <reified T> put(path: String, noinline params:JsonObjectBuilder.() -> Unit = {}, noinline body:RequestBuilder = {}): T = putCall(path, params, body).receive()

    suspend inline fun <reified T> delete(path: String, noinline params:JsonObjectBuilder.() -> Unit, noinline body:RequestBuilder = {}): T = deleteCall(path, params, body).receive()

    suspend inline fun <reified T> head(path: String, noinline params:JsonObjectBuilder.() -> Unit, noinline body:RequestBuilder = {}): T = headCall(path, params, body).receive()

    suspend fun getCall(path: String, params: JsonObjectBuilder.() -> Unit, body: RequestBuilder = {}): HttpClientCall = call(path, Get) {
        val p = json(params)
        p.entrySet().map { e -> e.key to e.value }.forEach { (k, v) ->
            if(!v.isJsonPrimitive) {
                throw IllegalArgumentException("Can't use complex parameters in GET request")
            }
            parameter(k, v.asString)
        }
        body()
    }
    
    suspend fun complexCall(path: String, method: HttpMethod, params: JsonObjectBuilder.() -> Unit = {}, body: RequestBuilder = {}) = call(path, method) {
        contentType(ContentType.Application.Json)
        this.body = json(params)
        body()
    }

    suspend fun postCall(path: String, params: JsonObjectBuilder.() -> Unit = {}, body: RequestBuilder = {}) = complexCall(path, Post, params, body)

    suspend fun putCall(path: String, params: JsonObjectBuilder.() -> Unit = {}, body: RequestBuilder = {}) = complexCall(path, Put, params, body)

    suspend fun deleteCall(path: String, params: JsonObjectBuilder.() -> Unit, body: RequestBuilder = {}) = complexCall(path, Delete, params, body)

    suspend fun headCall(path: String, params: JsonObjectBuilder.() -> Unit, body: RequestBuilder = {}) = complexCall(path, Head, params, body)

    fun close() {
        client.close()
    }
}