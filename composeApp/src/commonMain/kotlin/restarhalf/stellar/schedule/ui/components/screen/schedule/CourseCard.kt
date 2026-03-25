package restarhalf.stellar.schedule.ui.components.screen.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.mapper.CourseCardModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun CourseCard(model: CourseCardModel, onClick: (() -> Unit)?) {

    Card(
        modifier =
            Modifier
                .padding(horizontal = 2.5.dp)
                .fillMaxWidth()
                .height(model.height)
                .offset(y = model.topOffsetY)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors =
            CardDefaults.defaultColors(
                color = model.color.copy(alpha = model.cardAlpha),
            ),
        cornerRadius = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = model.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = model.titleColor,
                    lineHeight = 13.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "@${model.location}",
                    fontSize = 9.sp,
                    color = model.subTextColor,
                    lineHeight = 11.sp
                )

                Text(
                    text = model.teacher,
                    fontSize = 9.sp,
                    color = model.subTextColor,
                    lineHeight = 11.sp
                )
            }

            if (model.badgeCount != null) {

                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(20.dp)
                        .height(20.dp)
                ) {
                    val w = size.width

                    val h = size.height

                    val path =
                        Path().apply {
                            moveTo(0f, h)

                            lineTo(w, h)

                            lineTo(w, 0f)

                            close()
                        }

                    drawPath(path = path, color = model.titleColor.copy(alpha = 0.3f), style = Fill)
                }
            }
        }
    }
}
