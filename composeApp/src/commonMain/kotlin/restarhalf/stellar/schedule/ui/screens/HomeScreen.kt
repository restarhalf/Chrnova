package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.usecase.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.ui.components.screen.home.HomeExamSection
import restarhalf.stellar.schedule.ui.components.screen.home.HomePeriodSection
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页屏幕
 * 
 * 显示今日课程、问候语、当前时间等信息。
 * 
 * @param campus 当前校区
 * @param termStartMs 学期开始时间戳
 * @param totalWeeks 学期总周数
 */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    hasBackground: Boolean = false,
    componentsAlpha: Float,
    campus: Campus,
    termStartMs: Long,
    totalWeeks: Int,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val colors = MiuixTheme.colorScheme
    val textPrimary = colors.onBackground
    val textSecondary = colors.onSurfaceVariantSummary
    val dividerColor = colors.surfaceContainerHigh
    val homeUiState by vm.uiState.collectAsState()
    val renderState =
        remember(
            homeUiState.courses,
            campus,
            termStartMs,
            totalWeeks,
            hasBackground,
            componentsAlpha,
            homeUiState.nowMs
        ) {
            vm.buildHomeRenderState(
                courses = homeUiState.courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                hasBackground = hasBackground,
                componentsAlpha = componentsAlpha,
                nowMs = homeUiState.nowMs
            )
        }
    val headerUi = renderState.headerUi
    val surfaceUi = renderState.surfaceUi
    val sectionRenders = renderState.sectionRenders
    val todayExams = remember(homeUiState.exams, homeUiState.nowMs) {
        vm.buildExamUiList(
            vm.getTodayExams(homeUiState.exams, homeUiState.nowMs),
            homeUiState.nowMs
        )
    }
    Scaffold(
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp)
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp + paddingValues.calculateTopPadding())
                        .background(
                            if (surfaceUi.headerBackgroundMode ==
                                BuildHomeSurfaceUiUseCase.HeaderBackgroundMode.IMAGE_OVERLAY
                            ) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            colors.primary.copy(0.8f),
                                            colors.primary.copy(0.8f)
                                        )
                                )
                            }
                        )
            ) {
                Column(
                    modifier =
                        Modifier.padding(top = paddingValues.calculateTopPadding() + 32.dp, start = 32.dp)
                ) {
                    Text(
                        text = headerUi.dateLabel,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = headerUi.greeting,
                        color = colors.onPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding() + 140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface.copy(alpha = surfaceUi.contentSurfaceAlpha))
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .padding(bottom = appScaffoldPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    sectionRenders.forEach { section ->
                        HomePeriodSection(
                            title = section.title,
                            rows = section.rows,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            dividerColor = dividerColor,
                        )
                    }
                    if (todayExams.isNotEmpty()) {
                        HomeExamSection(
                            exams = todayExams,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            dividerColor = dividerColor,
                        )
                    }
                }
            }
        }
    }
}

