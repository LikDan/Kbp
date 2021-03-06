package com.ldc.kbp.models
import com.ldc.kbp.API_URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.URL

@Serializable
data class Schedule(
    val status: List<Status>,
    val subjects: List<Subjects?>,
    val info: Info
) {

    @Serializable
    data class Status(
        val weekIndex: Int,
        val dayIndex: Int,
        val status: StatusInfo
    )

    @Serializable
    data class Subjects(
        val weekIndex: Int,
        val columnIndex: Int,
        val rowIndex: Int,
        val isStay: Boolean,
        val subjects: List<Subject>
    )

    @Serializable
    data class Subject(
        var subject: String,
        var teacher: String,
        var room: String,
        var group: String,
        var type: Type
    )

    @Serializable
    data class Info(
        val weeksCount: Int,
        val daysCount: Int,
        val subjectsCount: Int,
        val type: String,
        val name: String,
        val studyPlaceId: Int,
        val studyPlaceName: String
    )

    @Serializable
    enum class Type {
        STAY,
        ADDED,
        REMOVED
    }

    @Serializable
    enum class StatusInfo {
        UPDATED,
        NOT_UPDATED
    }

    companion object {
        fun load(type: String, name: String): Schedule =
            Json.decodeFromStream(URL("$API_URL/schedule?type=$type&name=$name&studyPlaceId=0").openStream())
    }
}
