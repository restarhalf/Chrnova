package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Paper

/**
 * 课件管理端口接口
 *
 * 定义课件相关操作的抽象接口，包括查询、下载、上传、删除等。
 */
interface PapersPort {
    /**
     * 获取课件列表
     *
     * @return 课件列表
     */
    suspend fun listPapers(): List<Paper>

    /**
     * 获取所有课程名称列表
     *
     * @return 课程名称列表
     */
    suspend fun getCourses(): List<String>

    /**
     * 获取所有文件夹路径列表
     *
     * @return 文件夹路径列表
     */
    suspend fun getFolders(): List<String>

    /**
     * 根据ID获取课件详情
     *
     * @param id 课件唯一标识符
     * @return 课件详情
     */
    suspend fun getPaper(id: String): Paper

    /**
     * 下载课件到本地
     *
     * @param id 课件唯一标识符
     * @return 本地文件路径
     */
    suspend fun downloadPaper(id: String): String

    /**
     * 上传课件
     *
     * @param fileBytes 文件内容
     * @param fileName 文件名
     * @param mimeType MIME类型
     * @param title 课件标题
     * @param folder 所属文件夹
     * @return 上传后的课件信息
     */
    suspend fun uploadPaper(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        folder: String,
    ): Paper

    /**
     * 删除课件
     *
     * @param id 课件唯一标识符
     * @return 删除是否成功
     */
    suspend fun deletePaper(id: String): Boolean

    /**
     * 验证用户是否 star 了 GitHub 仓库
     *
     * @param username GitHub 用户名
     * @return 是否已 star
     */
    suspend fun verifyStar(username: String): Boolean
}
