package quoter

import io.ktor.client.call.HttpClientCall
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.content.ByteArrayContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import quoter.requester.ParameterBuilder
import quoter.requester.QuoterRequester


enum class DisplayType(val strValue: String) {
    TEXT("text"),
    DIALOG("dialog")
}

class Quoter(baseUrl: String, val accessKey: String? = null, val defaultRepo: String = "uadaf") {

    private val reqester = QuoterRequester(baseUrl)

    private fun HttpRequestBuilder.addKey() {
        if (accessKey != null) {
            header("X-Access-Key", accessKey)
        } else {
            throw IllegalStateException("Trying to modify quoter without access key")
        }
    }

    private fun ParameterBuilder.init(repo: String) {
        "resolver" to repo
    }

    suspend fun add(adder: String, authors: List<String>, content: String,
                    displayType: DisplayType? = null, attachments: List<String>? = null, repo: String = defaultRepo): HttpClientCall {
        val authorsStr = authors.joinToString(";")
        val attachmentsStr = attachments?.joinToString(";")
        return reqester.putCall("", {
            init(repo)
            "adder" to adder
            "authors" to authorsStr
            "content" to content
            if(displayType != null) {
                "dtype" to displayType.strValue
            }
            if (attachmentsStr != null) {
                "attachments" to attachmentsStr
            }
        }) {
            addKey()
        }
    }

    suspend fun add(adder: String, authors: String, content: String,
                    displayType: DisplayType? = null, attachments: List<String>? = null, repo: String = defaultRepo): HttpClientCall {
        return add(adder, listOf(authors), content, displayType, attachments, repo)
    }

    suspend fun attach(id: Int, attachment: String, repo: String = defaultRepo): HttpClientCall {
        return reqester.putCall("attach", {
            init(repo)
            "id" to id
            "attachment" to attachment
        }) {
            addKey()
        }
    }

    suspend fun byId(id: Int, repo: String = defaultRepo): Quote? {
        return try {
            reqester.get(id.toString(), {
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
        return reqester.get("$from/$to", {
            init(repo)
        })
    }

    suspend fun byRange(range: IntRange, repo: String = defaultRepo): List<Quote> {
        return byRange(range.start, range.endInclusive, repo)
    }

    suspend fun random(count: Int = 1, repo: String = defaultRepo): List<Quote> {
        return reqester.get("random/$count", {
            init(repo)
        })
    }

    suspend fun all(repo: String = defaultRepo): List<Quote> {
        return reqester.get("all", {
            init(repo)
        })
    }

    suspend fun total(repo: String = defaultRepo): Int {
        return reqester.get<String>("total", {
            init(repo)
        }).toInt()
    }

    suspend fun edit(id: Int, editedBy: String, newContent: String, repo: String = defaultRepo): HttpClientCall {
        return reqester.postCall("edit", {
            init(repo)
            "id" to id
            "edited_by" to editedBy
            "new_content" to newContent
        }) {
            addKey()
        }
    }

    suspend fun fixIds(repo: String = defaultRepo): HttpClientCall {
        return reqester.postCall("fix_ids", { init(repo) }) {
            addKey()
        }
    }

    suspend fun addRepo(name: String): HttpClientCall {
        return reqester.putCall("repo", { append("name", name) }) {
            addKey()
        }
    }

    suspend fun registerAttachment(type: String, data: ByteArray): String {
        return reqester.put("attachments", {}) {
            addKey()
            header("X-Attachment-Content-Type", type)
            body = ByteArrayContent(data)
        }
    }

    fun close() {
        reqester.close()
    }

}