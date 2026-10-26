package restarhalf.stellar.schedule.data.remote

import restarhalf.stellar.schedule.domain.model.JwxtSelectedCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionOperResult
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionPort
import restarhalf.stellar.schedule.domain.port.JwxtSelectionDropResult
import restarhalf.stellar.schedule.domain.port.JwxtSelectionSession

class JwxtCourseSelectionAdapter(
    private val gateway: JwxtGateway,
) : CourseSelectionPort {

    override suspend fun fetchRotations(): List<JwxtSelectionRotation> {
        val resp = gateway.fetchSelectionRotations(isnew = 1)
        JwxtSelectionParser.requireSuccess(resp, "获取选课轮次失败")
        return JwxtSelectionParser.parseList(
            resp.data,
            JwxtSelectionRotation.serializer(),
            "选课轮次列表",
        )
    }

    override suspend fun initSession(rotationId: String): JwxtSelectionSession {
        val resp = gateway.initSelectionSession(rotationId)
        JwxtSelectionParser.requireSuccess(resp, "进入选课失败")
        return JwxtSelectionParser.parseSessionContext(resp.data)
    }

    override suspend fun fetchCourses(
        rotationId: String,
        classificationCode: String,
        sessionTime: String,
        extraRules: Map<String, String>,
        courseInformation: String,
    ): List<JwxtSelectionCourse> {
        val resp = gateway.fetchSelectionCourses(
            rotationId = rotationId,
            classificationCode = classificationCode,
            sessionTime = sessionTime,
            extraRules = extraRules,
            courseInformation = courseInformation,
        )
        JwxtSelectionParser.requireSuccess(resp, "获取课程列表失败")
        return JwxtSelectionParser.parseList(
            resp.data,
            JwxtSelectionCourse.serializer(),
            "课程列表",
        )
    }

    override suspend fun submitSelection(
        rotationId: String,
        courseId: String,
        noticeId: String,
        sessionTime: String,
        classificationCode: String,
        splitIdentification: String,
        selectedNoticeId: String,
        selectedSplitIdentification: String,
        extraRules: Map<String, String>,
    ): JwxtSelectionOperResult = gateway.submitSelection(
        rotationId = rotationId,
        courseId = courseId,
        noticeId = noticeId,
        sessionTime = sessionTime,
        classificationCode = classificationCode,
        splitIdentification = splitIdentification,
        selectedNoticeId = selectedNoticeId,
        selectedSplitIdentification = selectedSplitIdentification,
        extraRules = extraRules,
    )

    override suspend fun dropSelection(
        rotationId: String,
        noticeId: String,
        sessionTime: String,
        courseQualification: String,
    ): JwxtSelectionDropResult {
        val resp = gateway.dropSelection(
            rotationId = rotationId,
            noticeId = noticeId,
            sessionTime = sessionTime,
            courseQualification = courseQualification,
        )
        return JwxtSelectionDropResult(
            success = resp.isSuccess(),
            message = resp.resolvedMessage(),
        )
    }

    override suspend fun fetchSelectedCourses(rotationId: String): List<JwxtSelectedCourse> {
        val resp = gateway.fetchSelectedCourses(rotationId = rotationId)
        JwxtSelectionParser.requireSuccess(resp, "获取已选课程失败")
        return JwxtSelectionParser.parseList(
            resp.data,
            JwxtSelectedCourse.serializer(),
            "已选课程列表",
        )
    }
}
