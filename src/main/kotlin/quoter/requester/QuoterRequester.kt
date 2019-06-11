package quoter.requester

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.call
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.plus
import io.ktor.http.takeFrom
import io.ktor.client.features.json.JsonFeature
typealias RequestBuilder = HttpRequestBuilder.() -> Unit

interface ParameterBuilder {

    fun append(n: String, v: String)

    infix fun String.to(v: Any) {
        append(this, v.toString())
    }

    fun end()
}

class GetParameterBulder(private val req: HttpRequestBuilder) : ParameterBuilder {

    override fun append(n: String, v: String) {
        req.parameter(n, v)
    }

    override fun end() {}

}

class PostParameterBuilder(private val req: HttpRequestBuilder) : ParameterBuilder {

    var params = Parameters.Empty

    override fun append(n: String, v: String) {
        params += Parameters.build { append(n, v) }
    }

    override fun end() {
        req.body = FormDataContent(params)
    }

}

class QuoterRequester(val baseUrl: String) {

    val client = HttpClient {

        install(JsonFeature)

    }

    suspend inline fun <reified T> request(path: String, method: HttpMethod, body: RequestBuilder = {}): T {
        return client.request {
            url.takeFrom("$baseUrl${path.removePrefix("/")}")
            this.method = method
            body()
            if (this.body is Parameters) {
                this.body = FormDataContent(this.body as Parameters)
            }
        }
    }

    suspend inline fun call(path: String, method: HttpMethod, crossinline body: RequestBuilder = {}): HttpClientCall {
        return client.call {
            url.takeFrom("$baseUrl${path.removePrefix("/")}")
            this.method = method
            body()
            if (this.body is Parameters) {
                this.body = FormDataContent(this.body as Parameters)
            }
        }
    }

    suspend inline fun <reified T> get(path: String, params: ParameterBuilder.() -> Unit, body: RequestBuilder = {}): T = request(path, HttpMethod.Get) {
        val p = GetParameterBulder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun <reified T> post(path: String, params: ParameterBuilder.() -> Unit = {}, body: RequestBuilder = {}): T = request(path, HttpMethod.Post) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun <reified T> put(path: String, params: ParameterBuilder.() -> Unit = {}, body: RequestBuilder = {}): T = request(path, HttpMethod.Put) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun <reified T> delete(path: String, params: ParameterBuilder.() -> Unit, body: RequestBuilder = {}): T = request(path, HttpMethod.Delete) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun <reified T> head(path: String, params: ParameterBuilder.() -> Unit, body: RequestBuilder = {}): T = request(path, HttpMethod.Head) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun getCall(path: String, crossinline params: ParameterBuilder.() -> Unit, crossinline body: RequestBuilder = {}): HttpClientCall = call(path, HttpMethod.Get) {
        val p = GetParameterBulder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun postCall(path: String, crossinline params: ParameterBuilder.() -> Unit = {}, crossinline body: RequestBuilder = {}): HttpClientCall = call(path, HttpMethod.Post) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun putCall(path: String, crossinline params: ParameterBuilder.() -> Unit = {}, crossinline body: RequestBuilder = {}): HttpClientCall = call(path, HttpMethod.Put) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun deleteCall(path: String, crossinline params: ParameterBuilder.() -> Unit, crossinline body: RequestBuilder = {}): HttpClientCall = call(path, HttpMethod.Delete) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    suspend inline fun headCall(path: String, crossinline params: ParameterBuilder.() -> Unit, crossinline body: RequestBuilder = {}): HttpClientCall = call(path, HttpMethod.Head) {
        val p = PostParameterBuilder(this)
        p.params()
        p.end()
        body()
    }

    fun close() {
        client.close()
    }
}