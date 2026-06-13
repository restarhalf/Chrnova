package restarhalf.stellar.schedule.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverters
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 应用Room数据库
 * 
 * 定义应用的本地数据库结构，包含以下表：
 * - courses: 课程表
 * - examinations: 考试安排
 * - grades: 成绩
 * - pe_scores: 体育成绩
 * - pe_student_info: 体育学生信息
 * - pe_detail_scores: 体育详情成绩
 * - pe_detail_summary: 体育成绩摘要
 */
@Database(
    entities = [
        Course::class,
        ExaminationEntity::class,
        GradeEntity::class,
        PEYearScoreEntity::class,
        PEStudentInfoEntity::class,
        PESubjectScoreEntity::class,
        PEDetailSummaryEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    /** 课程DAO */
    abstract fun courseDao(): CourseDao
    /** 考试安排DAO */
    abstract fun examinationDao(): ExaminationDao
    /** 成绩DAO */
    abstract fun gradeDao(): GradeDao
    /** 体育年度成绩DAO */
    abstract fun peYearScoreDao(): PEYearScoreDao
    /** 体育学生信息DAO */
    abstract fun peStudentInfoDao(): PEStudentInfoDao
    /** 体育详情DAO */
    abstract fun peDetailDao(): PEDetailDao

    companion object {
        const val DATABASE_NAME: String = "schedule.db"
    }
}

/** 数据库构造器（平台特定实现） */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** 数据库迁移：添加type字段 */
private val migration2To3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
    }
}

/** 数据库迁移：添加remoteKey字段 */
private val migration3To4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN remoteKey TEXT NOT NULL DEFAULT ''")
    }
}

/** 数据库迁移：添加originRemoteKey字段 */
private val migration4To5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN originRemoteKey TEXT")
    }
}

/** 数据库迁移：添加targetWeek字段 */
private val migration5To6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN targetWeek INTEGER NOT NULL DEFAULT 0")
    }
}

/** 数据库迁移：添加semesterId字段 */
private val migration6To7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE courses ADD COLUMN semesterId TEXT NOT NULL DEFAULT ''")
    }
}

/** 数据库迁移：创建考试和成绩表 */
private val migration7To8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `examinations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseNumber` TEXT NOT NULL, `courseName` TEXT NOT NULL, `time` TEXT NOT NULL, `examinationPlace` TEXT NOT NULL, `zwh` TEXT NOT NULL, `ksbz` TEXT NOT NULL, `semesterId` TEXT NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `grades` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseCode` TEXT NOT NULL, `courseName` TEXT NOT NULL, `score` TEXT NOT NULL, `gradePoint` REAL NOT NULL, `credit` REAL NOT NULL, `curriculumAttributes` TEXT NOT NULL, `courseNature` TEXT NOT NULL, `examName` TEXT NOT NULL, `examinationNature` TEXT NOT NULL, `passStatus` TEXT NOT NULL, `gradeLevel` TEXT NOT NULL, `markFlag` TEXT NOT NULL, `repeatSemester` TEXT NOT NULL, `gradeId` TEXT NOT NULL, `semester` TEXT NOT NULL)"
        )
    }
}

/** 数据库迁移：创建体育成绩表 */
private val migration8To9 = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_scores` (`schoolYear` TEXT PRIMARY KEY NOT NULL, `total` REAL NOT NULL, `isFree` INTEGER NOT NULL, `done` INTEGER NOT NULL, `nums` INTEGER NOT NULL)"
        )
    }
}

/** 数据库迁移：创建体育学生信息表 */
private val migration9To10 = object : Migration(9, 10) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_student_info` (`id` TEXT PRIMARY KEY NOT NULL, `testCode` TEXT NOT NULL, `stuName` TEXT NOT NULL, `stdNumber` TEXT NOT NULL)"
        )
    }
}

/** 数据库迁移：创建体育详情成绩表和摘要表 */
private val migration10To11 = object : Migration(10, 11) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_detail_scores` (`schoolYear` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `subName` TEXT NOT NULL, `result` TEXT, `score` INTEGER, `unit` TEXT NOT NULL, `subRatio` TEXT NOT NULL, `grade` TEXT, `isJoin` INTEGER NOT NULL, PRIMARY KEY (`schoolYear`, `subjectId`))"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pe_detail_summary` (`schoolYear` TEXT PRIMARY KEY NOT NULL, `totalScore` REAL NOT NULL, `totalGrade` TEXT NOT NULL)"
        )
    }
}

/**
 * 构建应用数据库
 * 
 * @param builder Room数据库构建器
 * @return 配置好的AppDatabase实例
 */
fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(AppIoDispatcher)
        .fallbackToDestructiveMigration(false)
        .addMigrations(
            migration2To3,
            migration3To4,
            migration4To5,
            migration5To6,
            migration6To7,
            migration7To8,
            migration8To9,
            migration9To10,
            migration10To11
        )
        .build()
