package restarhalf.stellar.schedule.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
    @Serializable
    data object Main : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object Schedule : Screen

    @Serializable
    data object EMS : Screen


    @Serializable
    data object Settings : Screen

    @Serializable
    data object ChangeBackground : Screen

    @Serializable
    data object About : Screen

    @Serializable
    data class ClassEdit(val courseId: Long? = null) : Screen

}
