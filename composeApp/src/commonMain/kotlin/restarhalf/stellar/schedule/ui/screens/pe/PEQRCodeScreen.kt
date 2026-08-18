package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.ktor.http.encodeURLParameter
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PEQRCodeScreen(
    vm: PEViewModel,
    jwxtAuthProfile: JwxtAuthProfile?,
    onBack: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val peAuthProfile = uiState.authProfile

    val id = peAuthProfile?.stdNumber ?: jwxtAuthProfile?.userNo
    val name = peAuthProfile?.stuName ?: jwxtAuthProfile?.name
    val colors = MiuixTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "体测二维码",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                            tint = colors.onBackground,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = 0.dp,
                    )
                )
                .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (id != null && name != null) {
                AppCard(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "体测二维码",
                            style = MiuixTheme.textStyles.title3,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "请将此二维码展示给体测老师扫描",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    clip = false
                                )
                                .squircleClip(16.dp)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            val urlName = name.encodeURLParameter()
                            val codeContent = "id=$id&name=$urlName"
                            Image(
                                modifier = Modifier.size(240.dp),
                                painter = rememberQrCodePainter(
                                    data = codeContent,
                                    options = QrOptions(
                                        errorCorrectionLevel = QrErrorCorrectionLevel.High,
                                    ),
                                ),
                                contentDescription = "体测二维码",
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "学号",
                                        style = MiuixTheme.textStyles.body2,
                                        color = colors.onSurfaceVariantSummary,
                                    )
                                    Text(
                                        text = id,
                                        style = MiuixTheme.textStyles.body2,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "姓名",
                                        style = MiuixTheme.textStyles.body2,
                                        color = colors.onSurfaceVariantSummary,
                                    )
                                    Text(
                                        text = name,
                                        style = MiuixTheme.textStyles.body2,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                AppCard(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "暂无学生信息",
                            style = MiuixTheme.textStyles.body1,
                            color = colors.onSurfaceVariantSummary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请先登录教务系统或体测系统",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}
