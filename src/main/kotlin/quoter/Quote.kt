package quoter
import com.google.gson.annotations.SerializedName

data class Quote(
    val id: Int,
    val adder: String,
    val authors: List<String>,
    @SerializedName("dtype") val displayType: String,
    val content: String,
    val date: Long,
    @SerializedName("edited_by") val editedBy: String,
    @SerializedName("edited_at") val editedAt: Long,
    val attachments: List<String>,
    @SerializedName("is_old") val isOld: Boolean
) {

    override fun equals(other: Any?): Boolean {
        return other is Quote && id == other.id
    }

    override fun hashCode(): Int {
        return 7 * id.hashCode()
    }

}