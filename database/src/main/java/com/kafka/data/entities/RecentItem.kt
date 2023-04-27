package com.kafka.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep
import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class RecentItem(
    @DocumentId
    val fileId: String,
    val itemId: String,
    val title: String,
    val coverUrl: String,
    val creator: String,
    val mediaType: String,
    val createdAt: Long
) {
    constructor() : this(
        fileId = "",
        itemId = "",
        title = "",
        coverUrl = "",
        creator = "",
        mediaType = "",
        createdAt = 0
    )

    companion object {
        fun fromItem(item: ItemDetail): RecentItem {
            return RecentItem(
                fileId = item.files!!.first(),
                itemId = item.itemId,
                title = item.title.orEmpty(),
                coverUrl = item.coverImage.orEmpty(),
                creator = item.creator.orEmpty(),
                mediaType = item.mediaType.orEmpty(),
                createdAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(tableName = "recent_text")
data class RecentTextItem(
    @PrimaryKey val fileId: String,
    val currentPage: Int,
    val localUri: String,
    val type: Type,
    val pages: List<Page>,
) : BaseEntity {
    enum class Type {
        PDF, TXT;

        companion object {
            fun fromString(type: String?): Type = when (type) {
                "pdf" -> PDF
                "txt" -> TXT
                else -> throw IllegalArgumentException("Unknown type $type")
            }
        }
    }

    @Serializable
    data class Page(val index: Int, val text: String)
}

@Entity(tableName = "recent_audio")
data class RecentAudioItem(
    @PrimaryKey val fileId: String,
    val currentTimestamp: Long,
    val duration: Long
) : BaseEntity {
    val progress: Int
        get() = (currentTimestamp * 100 / duration).toInt()
}

data class RecentItemWithProgress(
    val recentItem: RecentItem,
    val percentage: Int
) {
    val progress: Float
        get() = percentage / 100f
}
