package quoter

import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.response.readBytes
import io.ktor.content.ByteArrayContent
import io.ktor.http.HttpStatusCode
import quoter.requester.QuoterRequester
import quoter.util.JsonObjectBuilder


enum class DisplayType(val strValue: String) {
    TEXT("text"),
    DIALOG("dialog")
}

class Quoter<T: HttpClientEngineConfig>(baseUrl: String, httpClientEngine: HttpClientEngineFactory<T>, val accessKey: String? = null, val defaultRepo: String = "uadaf", httpClientEngineConfig: T.() -> Unit = {  }) {

    private val requester = QuoterRequester(baseUrl, httpClientEngine, httpClientEngineConfig)

    private fun HttpRequestBuilder.addKey() {
        check(accessKey != null) { "Trying to modify quoter without access key" }
        header("X-Access-Key", accessKey)
    }

    private fun JsonObjectBuilder.init(repo: String) {
        "resolver" to repo
    }

    private fun JsonObjectBuilder.maybe(name: String, value: String?) = value?.let { name to it }

    suspend fun add(adder: String, authors: List<String>, content: String,
                    displayType: DisplayType? = null, attachments: List<String>? = null, repo: String = defaultRepo): HttpClientCall {
        val authorsStr = authors.joinToString(";")
        val attachmentsStr = attachments?.joinToString(";")
        return requester.putCall("", {
            init(repo)
            "adder" to adder
            "authors" to authorsStr
            "content" to content
            maybe("dtype", displayType?.strValue)
            maybe("attachments", attachmentsStr)
        }) {
            addKey()
        }
    }

    suspend fun add(adder: String, authors: String, content: String,
                    displayType: DisplayType? = null, attachments: List<String>? = null, repo: String = defaultRepo): HttpClientCall {
        return add(adder, listOf(authors), content, displayType, attachments, repo)
    }

    suspend fun attach(id: Int, attachment: String, repo: String = defaultRepo): HttpClientCall {
        return requester.putCall("attach", {
            init(repo)
            "id" to id
            "attachment" to attachment
        }) {
            addKey()
        }
    }

    suspend fun byId(id: Int, repo: String = defaultRepo): Quote? {
        return try {
            requester.get(id.toString(), {
                init(repo)
            })
        } catch (e: ClientRequestException) {
            if(e.response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw e
            }
        }
    }

    suspend fun byRange(from: Int, to: Int, repo: String = defaultRepo): List<Quote> {
        return requester.get("$from/$to", {
            init(repo)
        })
    }

    suspend fun byRange(range: IntRange, repo: String = defaultRepo): List<Quote> {
        return byRange(range.first, range.last, repo)
    }

    suspend fun random(count: Int = 1, repo: String = defaultRepo): List<Quote> {
        return requester.get("random/$count", {
            init(repo)
        })
    }

    suspend fun all(repo: String = defaultRepo): List<Quote> {
        return requester.get("all", {
            init(repo)
        })
    }

    suspend fun total(repo: String = defaultRepo): Int {
        return requester.get<String>("total", {
            init(repo)
        }).toInt()
    }

    suspend fun edit(id: Int, editedBy: String, newContent: String, repo: String = defaultRepo): HttpClientCall {
        return requester.postCall("edit", {
            init(repo)
            "id" to id
            "edited_by" to editedBy
            "new_content" to newContent
        }) {
            addKey()
        }
    }

    suspend fun search(adder: String? = null, authors: List<String>? = null, content: String? = null,
                       repo: String = defaultRepo): List<Quote> {
        require(authors == null || authors.isNotEmpty())
        return requester.get("search", {
            init(repo)
            maybe("adder", adder)
            maybe("authors", authors?.joinToString(";"))
            maybe("content", content)
        })
    }

    suspend fun search(adder: String? = null, authors: String, content: String? = null,
                       repo: String = defaultRepo): List<Quote> {
        return search(adder, listOf(authors), content, repo)
    }

    suspend fun fixIds(repo: String = defaultRepo): HttpClientCall {
        return requester.postCall("fix_ids", { init(repo) }) {
            addKey()
        }
    }

    suspend fun addRepo(name: String): HttpClientCall {
        return requester.putCall("repo", { "name" to name }) {
            addKey()
        }
    }

    suspend fun registerAttachment(type: String, data: ByteArray): String {
        return requester.put("attachments", {}) {
            addKey()
            header("X-Attachment-Content-Type", type)
            body = ByteArrayContent(data)
        }
    }

    suspend fun getAttachment(id: String): Pair<String, ByteArray>? {
        val call = requester.getCall("attachments/$id", {})
        if (call.response.status == HttpStatusCode.NotFound) {
            return null
        }
        val type = call.response.headers["X-Attachment-Content-Type"] ?: throw IllegalStateException("Excepted X-Attachment-Content-Type header to be in attachment response")
        val data = call.response.readBytes()
        return type to data
    }

    suspend fun deleteAttachment(id: String): HttpClientCall {
        return requester.deleteCall("attachments/$id", {}) {
            addKey()
        }
    }

    fun close() {
        requester.close()
    }

}
