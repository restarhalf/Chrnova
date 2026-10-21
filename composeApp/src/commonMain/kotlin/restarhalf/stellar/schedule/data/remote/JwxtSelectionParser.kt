package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.port.JwxtSelectionSession


internal object JwxtSelectionParser {

    private val json = Json { ignoreUnknownKeys = true }


    fun <T> parseList(
        data: JsonElement?,
        serializer: kotlinx.serialization.KSerializer<T>,
        label: String,
    ): List<T> {
        if (data == null) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(serializer), data.toString())
        }.getOrElse {
            AppLogger.log("CourseSelection", "解析$label 失败", it)
            emptyList()
        }
    }


    fun parseSessionContext(data: JsonElement?): JwxtSelectionSession {
        val obj = data as? JsonObject
            ?: throw IllegalStateException("会话初始化失败：响应格式异常")
        val sessionTime = obj["sessionTime"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (sessionTime.isBlank()) {
            throw IllegalStateException("会话初始化失败：未获取到 sessionTime")
        }
        val classifications = (obj["classificationList"] as? JsonArray)?.let { arr ->
            runCatching {
                json.decodeFromString(
                    ListSerializer(JwxtSelectionClassification.serializer()),
                    arr.toString(),
                )
            }.getOrElse {
                AppLogger.log("CourseSelection", "解析选课分类列表失败", it)
                emptyList()
            }
        } ?: emptyList()

        val extraRules = buildMap {
            BOOL_RULE_KEYS.forEach { key ->
                val v = obj[key]
                val s = (v as? JsonPrimitive)?.contentOrNull
                if (s == "true" || s == "false") put(key, s)
            }
            val cq = obj["courseQualification"]?.jsonPrimitive?.contentOrNull
            if (!cq.isNullOrBlank()) put("courseQualification", cq)
        }

        return JwxtSelectionSession(
            sessionTime = sessionTime,
            classifications = classifications,
            extraRules = extraRules,
        )
    }


    fun requireSuccess(resp: JwxtSelectionResponse, fallbackMessage: String) {
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.resolvedMessage().ifBlank { fallbackMessage })
        }
    }

    private val BOOL_RULE_KEYS = listOf(
        "compulsorySemester",
        "compulsorySelection",
        "compulsoryGrades",
        "selectionGrades",
        "departmentCurriculum",
    )
}
