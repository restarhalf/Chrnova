package restarhalf.stellar.schedule.ui.screens.exclusion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chrnova.composeapp.generated.resources.Res
import chrnova.composeapp.generated.resources.ic_privacy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PrivacyScreen(
    pagerState: PagerState,
    onExit: ()-> Unit,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val colors = MiuixTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            painter = painterResource(Res.drawable.ic_privacy),
            contentDescription = "Privacy",
            tint = colors.primary,
            modifier = Modifier.size(90.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "隐私政策",
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.verticalScroll(scrollState).padding(16.dp)) {
                Text(
                    text =
                            "本项目（Chrnova）是个人开发的开源软件，采用 GNU Affero General Public License v3.0（AGPL-3.0）许可协议发布。使用本软件即表示您已阅读、理解并同意本免责声明的全部条款。\n" +
                            "\n" +
                            "一、关于软件性质的声明\n" +
                            "\n" +
                            "1. 本项目并非大连民族大学官方应用，与学校及其教务部门无任何关联、赞助或背书关系。\n" +
                            "2. 本软件为个人独立开发，仅为方便学生日常查看课表、成绩等教务信息而提供。\n" +
                            "3. 开发者保留随时修改、暂停或终止本软件功能的权利，无需事先通知。\n" +
                            "4. 为统计每日活跃设备数量，本软件会在确认本声明后的每个自然日首次启动时，向上报一个由本地随机生成、不关联您身份的匿名设备标识；除此之外不会收集或上传您的任何课表、成绩等个人数据。\n" +
                            "\n" +
                            "二、关于教务数据与账号安全\n" +
                            "\n" +
                            "1. 课表同步、成绩查询、考试安排等功能依赖于学校教务系统，如教务系统接口变动、字段调整或服务不可用，可能导致相关功能无法正常使用。\n" +
                            "2. 使用相关功能需要您输入教务系统账号及密码，请确保您已理解并自行承担由此带来的账号信息安全风险。\n" +
                            "3. 开发者不会主动收集或上传您的教务账号及密码，但您因使用本软件导致的账号信息泄露及其他相关损失，**由您自行承担全部责任**。\n" +
                            "4. 学校教务系统可能存在数据延迟、缺失或不准确的情况，本软件仅作展示用途，不保证数据的实时性、完整性和准确性，所有信息请以学校教务系统官方数据为准。\n" +
                            "5. 教务系统可能有墙，导致登录失败，请这时候连接校园网然后再登录进行同步\n"+
                            "\n" +
                            "三、关于软件使用的风险提示\n" +
                            "\n" +
                            "1. 本软件按“原样”（AS IS）提供，不作任何形式的明示或暗示担保，包括但不限于适销性、特定用途适用性及不侵权的担保。\n" +
                            "2. 在适用法律允许的最大范围内，开发者不对因使用或无法使用本软件而产生的任何直接、间接、偶然、特殊或结果性损失（包括但不限于错过课程、错过考试、成绩数据丢失、账号信息泄露、业务中断或其他损失）承担责任，即使开发者已被告知可能发生此类损害。\n" +
                            "3. 提醒功能的准确性取决于设备系统、网络状态和教务数据等多重因素，开发者不保证提醒信息的及时送达，未收到提醒不得作为申诉依据。\n" +
                            "\n" +
                            "四、关于开源许可与第三方代码\n" +
                            "\n" +
                            "1. 本项目采用 AGPL-3.0 许可证，使用、复制、修改、分发本软件均须遵守该许可证的全部条款。\n" +
                            "2. 本项目使用了部分第三方开源项目代码（详见 README 致谢部分），各第三方代码的版权归其各自权利人所有，其使用须同时遵守相应的开源许可协议。\n" +
                            "3. 如将本软件适配至其他学校使用，通常需要替换教务接口适配层，所产生的适配问题由修改者自行负责。\n" +
                            "\n" +
                            "五、其他\n" +
                            "\n" +
                            "1. 本免责声明的最终解释权归项目开发者所有。\n" +
                            "2. 本声明可能随时更新，请定期查阅最新版本。\n" +
                            "3. 如您不同意本声明的任何内容，请立即停止使用并卸载本软件。\n",
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            text = "拒绝",
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(2)
                }
            },
            colors = ButtonDefaults.textButtonColorsPrimary(),
            text = "同意",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

