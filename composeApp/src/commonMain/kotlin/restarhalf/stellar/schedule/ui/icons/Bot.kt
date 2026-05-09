package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Bot: ImageVector
    get() {
        val current = _bot
        if (current != null) return current

        return ImageVector.Builder(
            name = "Rounded.Bot",
            defaultWidth = 734.0.dp,
            defaultHeight = 734.0.dp,
            viewportWidth = 734.0f,
            viewportHeight = 734.0f,
        ).apply {
            // M370 62.2 c3.57 -.09 13.02 .28 21 .83 s19 1.7 24.5 2.56 15.85 2.86 23 4.44 a347 347 0 0 1 20 5 c3.85 1.17 11.5 3.7 17 5.61 s19 7.91 30 13.34 c11.98 5.9 25.63 13.59 34.04 19.19 7.72 5.13 16.75 11.58 20.07 14.33 s7.2 6.35 8.62 8 4.42 6.82 6.68 11.5 4.11 9.06 4.1 9.75 a2.1 2.1 0 0 1 -1.01 1.75 c-.55 .28 -5.61 -2.6 -11.25 -6.39 A282 282 0 0 0 546 139.66 a341 341 0 0 0 -21.5 -10.23 309 309 0 0 0 -21 -7.94 c-5.5 -1.79 -16.75 -4.65 -25 -6.36 -8.25 -1.72 -19.95 -3.57 -26 -4.12 a370 370 0 0 0 -27.25 -1.01 c-8.94 0 -21.54 .68 -28 1.5 a397 397 0 0 0 -22.25 3.6 283 283 0 0 0 -19 4.57 312 312 0 0 0 -21.5 7.54 330 330 0 0 0 -21.5 9.32 301 301 0 0 0 -18 9.97 344 344 0 0 0 -17 11.03 328 328 0 0 0 -17 13.24 291 291 0 0 0 -19 17.86 482 482 0 0 0 -16.66 18.38 272 272 0 0 0 -14.8 19.99 c-4.21 6.32 -11.14 18.47 -15.4 27 -4.27 8.52 -9.83 21.12 -12.35 28 a320 320 0 0 0 -8.23 27 261 261 0 0 0 -5.1 27 c-.89 7.59 -1.44 21.34 -1.38 57.5 l-9.79 7.22 c-5.38 3.97 -13.39 9.76 -17.79 12.87 a865.23 865.23 0 0 0 -48.41 38 404 404 0 0 0 -22.99 22 c-6.81 7.1 -14.25 15.39 -16.52 18.41 s-4.72 7.52 -5.44 10 c-.71 2.48 -1.15 5.62 -.97 7 .23 1.78 1.49 3.06 4.33 4.42 2.2 1.06 7.83 2.36 12.5 2.9 5.94 .69 13.17 .62 24 -.22 a498 498 0 0 0 29 -3.22 819 819 0 0 0 29 -5.03 c8.53 -1.66 24.05 -4.98 34.5 -7.37 s25.75 -6.14 34 -8.33 22.43 -6 31.5 -8.47 c9.07 -2.46 25.05 -6.98 35.5 -10.04 A2214 2214 0 0 0 315 463.2 a574 574 0 0 1 18.25 -5.54 c1.77 -.43 2.66 -.27 2.5 .43 -.14 .61 -4.52 2.87 -9.75 5.05 s-19.4 7.95 -31.5 12.83 a1829 1829 0 0 1 -28.5 11.31 c-3.57 1.33 -13.25 4.91 -21.5 7.94 a792 792 0 0 1 -21.5 7.6 c-3.57 1.14 -7.06 2.78 -7.75 3.63 -1.01 1.25 -.97 2.08 .2 4.3 a90 90 0 0 0 5.75 8.41 387 387 0 0 0 13.8 16.37 267 267 0 0 0 17.5 17.87 c4.4 3.95 10.48 9.24 13.5 11.76 3.02 2.53 9.32 7.26 14 10.51 a370 370 0 0 0 17 11.01 285 285 0 0 0 14.5 8.11 c3.3 1.65 9.37 4.34 13.5 5.97 a433 433 0 0 0 16.5 6.01 466 466 0 0 0 20 6.13 223 223 0 0 0 20.5 4.58 c5.23 .82 17.38 2.02 27 2.65 a224 224 0 0 0 34 .03 c9.07 -.62 21.68 -2.04 28 -3.14 a282 282 0 0 0 21 -4.61 c5.23 -1.43 14.9 -4.54 21.5 -6.93 s16.5 -6.42 22 -8.98 c5.5 -2.55 14.95 -7.63 21 -11.29 a340 340 0 0 0 23 -15.62 c7.16 -5.36 18.05 -15.08 27 -24.07 8.25 -8.3 19.73 -20.81 25.5 -27.81 s11.06 -12.73 11.75 -12.72 c.79 .01 0 2.49 -2.14 6.76 a297 297 0 0 1 -7.84 14.25 699 699 0 0 1 -9.24 15 c-2.64 4.12 -9.35 13.34 -14.91 20.49 a342 342 0 0 1 -22.12 25.08 c-6.6 6.65 -15.38 14.76 -19.5 18.01 -4.13 3.25 -9.3 7.29 -11.5 8.97 s-8.05 5.77 -13 9.08 a323 323 0 0 1 -18 11.06 493 493 0 0 1 -16 8.56 386 386 0 0 1 -17.5 8.02 395 395 0 0 1 -19.5 7.66 281 281 0 0 1 -18 5.53 c-4.95 1.3 -15.3 3.73 -23 5.38 a330 330 0 0 1 -23.5 4.18 c-6.2 .77 -20.09 1.02 -40 .72 -25.54 -.37 -32.21 -.78 -41 -2.52 a964 964 0 0 1 -21.5 -4.55 239 239 0 0 1 -19 -5.15 694 694 0 0 1 -17.5 -6.21 c-5.23 -1.95 -17.6 -7.48 -27.5 -12.3 -9.9 -4.81 -23.4 -12.34 -30 -16.72 a490 490 0 0 1 -23.5 -16.87 c-6.32 -4.89 -18.27 -15.65 -26.53 -23.91 A271 271 0 0 1 135.61 556 c-5.95 -7.98 -11.56 -15.74 -12.46 -17.26 q-1.65 -2.77 -4.15 -2.76 c-1.37 .01 -4.97 .64 -8 1.41 a633 633 0 0 1 -15 3.47 592 592 0 0 1 -24.5 4.55 c-8.25 1.36 -18.82 2.75 -23.5 3.08 a90 90 0 0 1 -16 -.48 59 59 0 0 1 -13.4 -3.8 c-3.25 -1.49 -6.87 -3.83 -8.05 -5.21 a23 23 0 0 1 -3.44 -6.5 c-1.01 -3.12 -1.05 -5.1 -.16 -9 .63 -2.75 2.68 -8.15 4.56 -12 s5.8 -10.14 8.7 -13.97 a330 330 0 0 1 15.36 -18 c5.54 -6.07 17.2 -17.55 25.91 -25.53 s16.05 -15.18 16.31 -16 -.73 -7.57 -2.21 -15 a385 385 0 0 1 -4.23 -28 c-1.13 -10.57 -1.41 -20.2 -1.02 -35.5 .29 -11.55 1.24 -25.73 2.12 -31.5 a408 408 0 0 1 4.51 -23.5 c1.6 -7.15 4.56 -18.4 6.56 -25 s5.67 -16.95 8.15 -23 a408.03 408.03 0 0 1 25.89 -50.5 377 377 0 0 1 14.63 -21 A375 375 0 0 1 153 160.5 c6.68 -7.15 16.05 -16.51 20.82 -20.8 a337 337 0 0 1 20.68 -16.65 390 390 0 0 1 22.75 -15.46 358 358 0 0 1 22.5 -12.45 A421 421 0 0 1 264 84.21 a292 292 0 0 1 23 -8.11 565 565 0 0 1 26 -6.57 c8.52 -1.94 20.9 -4.2 27.5 -5.02 6.6 -.83 14.48 -1.64 17.5 -1.82 s8.43 -.4 12 -.49 m332.25 141.38 c5.7 .24 11.52 1.11 14 2.07 a38 38 0 0 1 7.69 4.25 18 18 0 0 1 4.86 6.1 c1.13 2.78 1.2 4.52 .33 8.5 -.68 3.09 -2.96 7.68 -5.97 12 -2.68 3.85 -8.45 10.6 -12.82 15 s-14.44 13.23 -22.39 19.62 A608 608 0 0 1 661 291.46 a789 789 0 0 1 -24.5 16.32 c-6.6 4.18 -15.83 9.92 -20.5 12.77 s-16.37 9.67 -26 15.16 a2187 2187 0 0 1 -36 20.02 2261 2261 0 0 1 -51.5 26.52 c-18.15 9.07 -42 20.79 -53 26.04 s-21.35 9.87 -23 10.26 c-2.04 .48 -2.68 .38 -2 -.31 .55 -.57 11.8 -6.97 25 -14.25 13.2 -7.27 32.55 -17.95 43 -23.74 a1802 1802 0 0 0 33.5 -19.06 c7.98 -4.69 22.38 -13.46 32 -19.5 s23.58 -15.1 31 -20.14 a935 935 0 0 0 27 -19.19 391 391 0 0 0 25 -20.52 c6.33 -5.77 13.36 -12.94 15.63 -15.92 a73 73 0 0 0 5.88 -8.92 c.95 -1.92 1.93 -5.75 2.18 -8.5 .33 -3.86 -.04 -5.74 -1.63 -8.25 -1.22 -1.93 -4.2 -4.29 -12.56 -8.35 l-24.5 .38 c-21.21 .33 -26.51 .75 -39.5 3.1 a924 924 0 0 0 -31 6.29 c-8.8 1.96 -19.15 4.19 -23 4.94 s-7.23 1.12 -7.5 .82 .17 -.96 1 -1.47 9.6 -3.62 19.5 -6.9 24.75 -8.05 33 -10.58 a1284 1284 0 0 1 31.5 -9.14 781 781 0 0 1 31.5 -7.84 775 775 0 0 1 24 -4.94 278 278 0 0 1 18 -2.51 c4.95 -.49 13.39 -.7 18.75 -.47 m-40.5 -106.51 c1.24 -.04 4.84 .74 8 1.73 a46 46 0 0 1 10.25 4.9 51 51 0 0 1 6.69 5.44 c1.2 1.3 3.63 5.51 5.4 9.36 2.43 5.3 3.2 8.34 3.15 12.5 a39 39 0 0 1 -2.32 11.5 33 33 0 0 1 -7.34 11.28 c-3.05 3.17 -7.08 6.11 -10.08 7.36 a60 60 0 0 1 -10.5 3.04 c-4.73 .83 -6.47 .64 -12.47 -1.36 -3.84 -1.28 -8.79 -3.71 -11 -5.4 a38 38 0 0 1 -7.21 -8 48 48 0 0 1 -4.95 -11.42 c-1.5 -5.56 -1.58 -7.29 -.55 -12 a52 52 0 0 1 3.15 -9.5 c1.07 -2.2 3.98 -6.14 6.48 -8.75 s5.9 -5.42 7.55 -6.25 a56 56 0 0 1 8.25 -2.93 c2.89 -.79 6.26 -1.47 7.5 -1.5 M374.23 193 c.6 0 1.23 1.46 1.39 3.25 .17 1.79 1.01 14.95 1.87 29.25 .85 14.3 2.21 31.63 3.01 38.5 s2.16 16.32 3.02 21 2.95 13 4.64 18.5 4.81 13.6 6.93 18 5.99 10.48 8.59 13.5 c2.61 3.02 7.78 8.2 11.48 11.5 s8.89 7.23 11.54 8.73 c2.64 1.51 9.53 4.35 15.3 6.32 6.49 2.21 15.65 4.33 24 5.55 a356 356 0 0 1 15.25 2.44 c.96 .25 1.75 .91 1.75 1.46 s-.79 1.2 -1.75 1.45 a170 170 0 0 1 -10.25 1.57 257 257 0 0 0 -17 2.93 c-4.68 1 -12.1 3.31 -16.5 5.14 a161 161 0 0 0 -12.5 5.78 c-2.48 1.35 -9.27 7.22 -15.1 13.04 -9.08 9.07 -11.17 11.81 -14.64 19.09 a128 128 0 0 0 -6.1 15.5 249 249 0 0 0 -4.13 17.5 366 366 0 0 0 -3.48 22 638 638 0 0 0 -2.53 29.5 c-.62 9.9 -1.54 23.4 -2.05 30 s-1.15 12.23 -1.44 12.5 -.87 .12 -1.28 -.34 -1.07 -6.32 -1.46 -13 c-.39 -6.69 -1.2 -19.58 -1.81 -28.66 a611 611 0 0 0 -3.03 -31.5 331 331 0 0 0 -4.33 -25.5 167 167 0 0 0 -5.48 -18.5 c-1.7 -4.4 -4.71 -10.7 -6.71 -14 -1.99 -3.3 -5.49 -8.22 -7.77 -10.94 a99 99 0 0 0 -10.28 -10 91 91 0 0 0 -14.5 -9.21 113 113 0 0 0 -15.38 -6.18 181 181 0 0 0 -15.5 -3.54 240 240 0 0 0 -15 -2.14 c-3.58 -.34 -7.18 -1.04 -8 -1.56 -1.33 -.83 -1.33 -1.03 0 -1.85 .82 -.5 5.32 -1.21 10 -1.56 s13 -1.7 18.5 -2.99 13.6 -3.72 18 -5.4 a90 90 0 0 0 14 -6.93 c3.3 -2.14 8.42 -6.09 11.38 -8.79 a79 79 0 0 0 10.04 -11.91 108 108 0 0 0 8 -15 c1.84 -4.4 4.42 -11.6 5.74 -16 s3.26 -13.4 4.32 -20 c1.05 -6.6 2.59 -19.42 3.42 -28.5 s1.99 -25.05 2.59 -35.5 1.32 -20.24 1.61 -21.75 1.03 -2.75 1.63 -2.75
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 0.99f,
                pathFillType = PathFillType.EvenOdd,
                strokeAlpha = 0.99f,
            ) {
                // M 370 62.2
                moveTo(x = 370.0f, y = 62.2f)
                // c 3.57 -0.09 13.02 0.28 21 0.83
                curveToRelative(
                    dx1 = 3.57f,
                    dy1 = -0.09f,
                    dx2 = 13.02f,
                    dy2 = 0.28f,
                    dx3 = 21.0f,
                    dy3 = 0.83f,
                )
                // s 19 1.7 24.5 2.56
                reflectiveCurveToRelative(
                    dx1 = 19.0f,
                    dy1 = 1.7f,
                    dx2 = 24.5f,
                    dy2 = 2.56f,
                )
                // s 15.85 2.86 23 4.44
                reflectiveCurveToRelative(
                    dx1 = 15.85f,
                    dy1 = 2.86f,
                    dx2 = 23.0f,
                    dy2 = 4.44f,
                )
                // a 347 347 0 0 1 20 5
                arcToRelative(
                    a = 347.0f,
                    b = 347.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 20.0f,
                    dy1 = 5.0f,
                )
                // c 3.85 1.17 11.5 3.7 17 5.61
                curveToRelative(
                    dx1 = 3.85f,
                    dy1 = 1.17f,
                    dx2 = 11.5f,
                    dy2 = 3.7f,
                    dx3 = 17.0f,
                    dy3 = 5.61f,
                )
                // s 19 7.91 30 13.34
                reflectiveCurveToRelative(
                    dx1 = 19.0f,
                    dy1 = 7.91f,
                    dx2 = 30.0f,
                    dy2 = 13.34f,
                )
                // c 11.98 5.9 25.63 13.59 34.04 19.19
                curveToRelative(
                    dx1 = 11.98f,
                    dy1 = 5.9f,
                    dx2 = 25.63f,
                    dy2 = 13.59f,
                    dx3 = 34.04f,
                    dy3 = 19.19f,
                )
                // c 7.72 5.13 16.75 11.58 20.07 14.33
                curveToRelative(
                    dx1 = 7.72f,
                    dy1 = 5.13f,
                    dx2 = 16.75f,
                    dy2 = 11.58f,
                    dx3 = 20.07f,
                    dy3 = 14.33f,
                )
                // s 7.2 6.35 8.62 8
                reflectiveCurveToRelative(
                    dx1 = 7.2f,
                    dy1 = 6.35f,
                    dx2 = 8.62f,
                    dy2 = 8.0f,
                )
                // s 4.42 6.82 6.68 11.5
                reflectiveCurveToRelative(
                    dx1 = 4.42f,
                    dy1 = 6.82f,
                    dx2 = 6.68f,
                    dy2 = 11.5f,
                )
                // s 4.11 9.06 4.1 9.75
                reflectiveCurveToRelative(
                    dx1 = 4.11f,
                    dy1 = 9.06f,
                    dx2 = 4.1f,
                    dy2 = 9.75f,
                )
                // a 2.1 2.1 0 0 1 -1.01 1.75
                arcToRelative(
                    a = 2.1f,
                    b = 2.1f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -1.01f,
                    dy1 = 1.75f,
                )
                // c -0.55 0.28 -5.61 -2.6 -11.25 -6.39
                curveToRelative(
                    dx1 = -0.55f,
                    dy1 = 0.28f,
                    dx2 = -5.61f,
                    dy2 = -2.6f,
                    dx3 = -11.25f,
                    dy3 = -6.39f,
                )
                // A 282 282 0 0 0 546 139.66
                arcTo(
                    horizontalEllipseRadius = 282.0f,
                    verticalEllipseRadius = 282.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 546.0f,
                    y1 = 139.66f,
                )
                // a 341 341 0 0 0 -21.5 -10.23
                arcToRelative(
                    a = 341.0f,
                    b = 341.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -21.5f,
                    dy1 = -10.23f,
                )
                // a 309 309 0 0 0 -21 -7.94
                arcToRelative(
                    a = 309.0f,
                    b = 309.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -21.0f,
                    dy1 = -7.94f,
                )
                // c -5.5 -1.79 -16.75 -4.65 -25 -6.36
                curveToRelative(
                    dx1 = -5.5f,
                    dy1 = -1.79f,
                    dx2 = -16.75f,
                    dy2 = -4.65f,
                    dx3 = -25.0f,
                    dy3 = -6.36f,
                )
                // c -8.25 -1.72 -19.95 -3.57 -26 -4.12
                curveToRelative(
                    dx1 = -8.25f,
                    dy1 = -1.72f,
                    dx2 = -19.95f,
                    dy2 = -3.57f,
                    dx3 = -26.0f,
                    dy3 = -4.12f,
                )
                // a 370 370 0 0 0 -27.25 -1.01
                arcToRelative(
                    a = 370.0f,
                    b = 370.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -27.25f,
                    dy1 = -1.01f,
                )
                // c -8.94 0 -21.54 0.68 -28 1.5
                curveToRelative(
                    dx1 = -8.94f,
                    dy1 = 0.0f,
                    dx2 = -21.54f,
                    dy2 = 0.68f,
                    dx3 = -28.0f,
                    dy3 = 1.5f,
                )
                // a 397 397 0 0 0 -22.25 3.6
                arcToRelative(
                    a = 397.0f,
                    b = 397.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -22.25f,
                    dy1 = 3.6f,
                )
                // a 283 283 0 0 0 -19 4.57
                arcToRelative(
                    a = 283.0f,
                    b = 283.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -19.0f,
                    dy1 = 4.57f,
                )
                // a 312 312 0 0 0 -21.5 7.54
                arcToRelative(
                    a = 312.0f,
                    b = 312.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -21.5f,
                    dy1 = 7.54f,
                )
                // a 330 330 0 0 0 -21.5 9.32
                arcToRelative(
                    a = 330.0f,
                    b = 330.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -21.5f,
                    dy1 = 9.32f,
                )
                // a 301 301 0 0 0 -18 9.97
                arcToRelative(
                    a = 301.0f,
                    b = 301.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -18.0f,
                    dy1 = 9.97f,
                )
                // a 344 344 0 0 0 -17 11.03
                arcToRelative(
                    a = 344.0f,
                    b = 344.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -17.0f,
                    dy1 = 11.03f,
                )
                // a 328 328 0 0 0 -17 13.24
                arcToRelative(
                    a = 328.0f,
                    b = 328.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -17.0f,
                    dy1 = 13.24f,
                )
                // a 291 291 0 0 0 -19 17.86
                arcToRelative(
                    a = 291.0f,
                    b = 291.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -19.0f,
                    dy1 = 17.86f,
                )
                // a 482 482 0 0 0 -16.66 18.38
                arcToRelative(
                    a = 482.0f,
                    b = 482.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -16.66f,
                    dy1 = 18.38f,
                )
                // a 272 272 0 0 0 -14.8 19.99
                arcToRelative(
                    a = 272.0f,
                    b = 272.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -14.8f,
                    dy1 = 19.99f,
                )
                // c -4.21 6.32 -11.14 18.47 -15.4 27
                curveToRelative(
                    dx1 = -4.21f,
                    dy1 = 6.32f,
                    dx2 = -11.14f,
                    dy2 = 18.47f,
                    dx3 = -15.4f,
                    dy3 = 27.0f,
                )
                // c -4.27 8.52 -9.83 21.12 -12.35 28
                curveToRelative(
                    dx1 = -4.27f,
                    dy1 = 8.52f,
                    dx2 = -9.83f,
                    dy2 = 21.12f,
                    dx3 = -12.35f,
                    dy3 = 28.0f,
                )
                // a 320 320 0 0 0 -8.23 27
                arcToRelative(
                    a = 320.0f,
                    b = 320.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -8.23f,
                    dy1 = 27.0f,
                )
                // a 261 261 0 0 0 -5.1 27
                arcToRelative(
                    a = 261.0f,
                    b = 261.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -5.1f,
                    dy1 = 27.0f,
                )
                // c -0.89 7.59 -1.44 21.34 -1.38 57.5
                curveToRelative(
                    dx1 = -0.89f,
                    dy1 = 7.59f,
                    dx2 = -1.44f,
                    dy2 = 21.34f,
                    dx3 = -1.38f,
                    dy3 = 57.5f,
                )
                // l -9.79 7.22
                lineToRelative(dx = -9.79f, dy = 7.22f)
                // c -5.38 3.97 -13.39 9.76 -17.79 12.87
                curveToRelative(
                    dx1 = -5.38f,
                    dy1 = 3.97f,
                    dx2 = -13.39f,
                    dy2 = 9.76f,
                    dx3 = -17.79f,
                    dy3 = 12.87f,
                )
                // a 865.23 865.23 0 0 0 -48.41 38
                arcToRelative(
                    a = 865.23f,
                    b = 865.23f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -48.41f,
                    dy1 = 38.0f,
                )
                // a 404 404 0 0 0 -22.99 22
                arcToRelative(
                    a = 404.0f,
                    b = 404.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -22.99f,
                    dy1 = 22.0f,
                )
                // c -6.81 7.1 -14.25 15.39 -16.52 18.41
                curveToRelative(
                    dx1 = -6.81f,
                    dy1 = 7.1f,
                    dx2 = -14.25f,
                    dy2 = 15.39f,
                    dx3 = -16.52f,
                    dy3 = 18.41f,
                )
                // s -4.72 7.52 -5.44 10
                reflectiveCurveToRelative(
                    dx1 = -4.72f,
                    dy1 = 7.52f,
                    dx2 = -5.44f,
                    dy2 = 10.0f,
                )
                // c -0.71 2.48 -1.15 5.62 -0.97 7
                curveToRelative(
                    dx1 = -0.71f,
                    dy1 = 2.48f,
                    dx2 = -1.15f,
                    dy2 = 5.62f,
                    dx3 = -0.97f,
                    dy3 = 7.0f,
                )
                // c 0.23 1.78 1.49 3.06 4.33 4.42
                curveToRelative(
                    dx1 = 0.23f,
                    dy1 = 1.78f,
                    dx2 = 1.49f,
                    dy2 = 3.06f,
                    dx3 = 4.33f,
                    dy3 = 4.42f,
                )
                // c 2.2 1.06 7.83 2.36 12.5 2.9
                curveToRelative(
                    dx1 = 2.2f,
                    dy1 = 1.06f,
                    dx2 = 7.83f,
                    dy2 = 2.36f,
                    dx3 = 12.5f,
                    dy3 = 2.9f,
                )
                // c 5.94 0.69 13.17 0.62 24 -0.22
                curveToRelative(
                    dx1 = 5.94f,
                    dy1 = 0.69f,
                    dx2 = 13.17f,
                    dy2 = 0.62f,
                    dx3 = 24.0f,
                    dy3 = -0.22f,
                )
                // a 498 498 0 0 0 29 -3.22
                arcToRelative(
                    a = 498.0f,
                    b = 498.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 29.0f,
                    dy1 = -3.22f,
                )
                // a 819 819 0 0 0 29 -5.03
                arcToRelative(
                    a = 819.0f,
                    b = 819.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 29.0f,
                    dy1 = -5.03f,
                )
                // c 8.53 -1.66 24.05 -4.98 34.5 -7.37
                curveToRelative(
                    dx1 = 8.53f,
                    dy1 = -1.66f,
                    dx2 = 24.05f,
                    dy2 = -4.98f,
                    dx3 = 34.5f,
                    dy3 = -7.37f,
                )
                // s 25.75 -6.14 34 -8.33
                reflectiveCurveToRelative(
                    dx1 = 25.75f,
                    dy1 = -6.14f,
                    dx2 = 34.0f,
                    dy2 = -8.33f,
                )
                // s 22.43 -6 31.5 -8.47
                reflectiveCurveToRelative(
                    dx1 = 22.43f,
                    dy1 = -6.0f,
                    dx2 = 31.5f,
                    dy2 = -8.47f,
                )
                // c 9.07 -2.46 25.05 -6.98 35.5 -10.04
                curveToRelative(
                    dx1 = 9.07f,
                    dy1 = -2.46f,
                    dx2 = 25.05f,
                    dy2 = -6.98f,
                    dx3 = 35.5f,
                    dy3 = -10.04f,
                )
                // A 2214 2214 0 0 0 315 463.2
                arcTo(
                    horizontalEllipseRadius = 2214.0f,
                    verticalEllipseRadius = 2214.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 315.0f,
                    y1 = 463.2f,
                )
                // a 574 574 0 0 1 18.25 -5.54
                arcToRelative(
                    a = 574.0f,
                    b = 574.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 18.25f,
                    dy1 = -5.54f,
                )
                // c 1.77 -0.43 2.66 -0.27 2.5 0.43
                curveToRelative(
                    dx1 = 1.77f,
                    dy1 = -0.43f,
                    dx2 = 2.66f,
                    dy2 = -0.27f,
                    dx3 = 2.5f,
                    dy3 = 0.43f,
                )
                // c -0.14 0.61 -4.52 2.87 -9.75 5.05
                curveToRelative(
                    dx1 = -0.14f,
                    dy1 = 0.61f,
                    dx2 = -4.52f,
                    dy2 = 2.87f,
                    dx3 = -9.75f,
                    dy3 = 5.05f,
                )
                // s -19.4 7.95 -31.5 12.83
                reflectiveCurveToRelative(
                    dx1 = -19.4f,
                    dy1 = 7.95f,
                    dx2 = -31.5f,
                    dy2 = 12.83f,
                )
                // a 1829 1829 0 0 1 -28.5 11.31
                arcToRelative(
                    a = 1829.0f,
                    b = 1829.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -28.5f,
                    dy1 = 11.31f,
                )
                // c -3.57 1.33 -13.25 4.91 -21.5 7.94
                curveToRelative(
                    dx1 = -3.57f,
                    dy1 = 1.33f,
                    dx2 = -13.25f,
                    dy2 = 4.91f,
                    dx3 = -21.5f,
                    dy3 = 7.94f,
                )
                // a 792 792 0 0 1 -21.5 7.6
                arcToRelative(
                    a = 792.0f,
                    b = 792.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -21.5f,
                    dy1 = 7.6f,
                )
                // c -3.57 1.14 -7.06 2.78 -7.75 3.63
                curveToRelative(
                    dx1 = -3.57f,
                    dy1 = 1.14f,
                    dx2 = -7.06f,
                    dy2 = 2.78f,
                    dx3 = -7.75f,
                    dy3 = 3.63f,
                )
                // c -1.01 1.25 -0.97 2.08 0.2 4.3
                curveToRelative(
                    dx1 = -1.01f,
                    dy1 = 1.25f,
                    dx2 = -0.97f,
                    dy2 = 2.08f,
                    dx3 = 0.2f,
                    dy3 = 4.3f,
                )
                // a 90 90 0 0 0 5.75 8.41
                arcToRelative(
                    a = 90.0f,
                    b = 90.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 5.75f,
                    dy1 = 8.41f,
                )
                // a 387 387 0 0 0 13.8 16.37
                arcToRelative(
                    a = 387.0f,
                    b = 387.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 13.8f,
                    dy1 = 16.37f,
                )
                // a 267 267 0 0 0 17.5 17.87
                arcToRelative(
                    a = 267.0f,
                    b = 267.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 17.5f,
                    dy1 = 17.87f,
                )
                // c 4.4 3.95 10.48 9.24 13.5 11.76
                curveToRelative(
                    dx1 = 4.4f,
                    dy1 = 3.95f,
                    dx2 = 10.48f,
                    dy2 = 9.24f,
                    dx3 = 13.5f,
                    dy3 = 11.76f,
                )
                // c 3.02 2.53 9.32 7.26 14 10.51
                curveToRelative(
                    dx1 = 3.02f,
                    dy1 = 2.53f,
                    dx2 = 9.32f,
                    dy2 = 7.26f,
                    dx3 = 14.0f,
                    dy3 = 10.51f,
                )
                // a 370 370 0 0 0 17 11.01
                arcToRelative(
                    a = 370.0f,
                    b = 370.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 17.0f,
                    dy1 = 11.01f,
                )
                // a 285 285 0 0 0 14.5 8.11
                arcToRelative(
                    a = 285.0f,
                    b = 285.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 14.5f,
                    dy1 = 8.11f,
                )
                // c 3.3 1.65 9.37 4.34 13.5 5.97
                curveToRelative(
                    dx1 = 3.3f,
                    dy1 = 1.65f,
                    dx2 = 9.37f,
                    dy2 = 4.34f,
                    dx3 = 13.5f,
                    dy3 = 5.97f,
                )
                // a 433 433 0 0 0 16.5 6.01
                arcToRelative(
                    a = 433.0f,
                    b = 433.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 16.5f,
                    dy1 = 6.01f,
                )
                // a 466 466 0 0 0 20 6.13
                arcToRelative(
                    a = 466.0f,
                    b = 466.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 20.0f,
                    dy1 = 6.13f,
                )
                // a 223 223 0 0 0 20.5 4.58
                arcToRelative(
                    a = 223.0f,
                    b = 223.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 20.5f,
                    dy1 = 4.58f,
                )
                // c 5.23 0.82 17.38 2.02 27 2.65
                curveToRelative(
                    dx1 = 5.23f,
                    dy1 = 0.82f,
                    dx2 = 17.38f,
                    dy2 = 2.02f,
                    dx3 = 27.0f,
                    dy3 = 2.65f,
                )
                // a 224 224 0 0 0 34 0.03
                arcToRelative(
                    a = 224.0f,
                    b = 224.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 34.0f,
                    dy1 = 0.03f,
                )
                // c 9.07 -0.62 21.68 -2.04 28 -3.14
                curveToRelative(
                    dx1 = 9.07f,
                    dy1 = -0.62f,
                    dx2 = 21.68f,
                    dy2 = -2.04f,
                    dx3 = 28.0f,
                    dy3 = -3.14f,
                )
                // a 282 282 0 0 0 21 -4.61
                arcToRelative(
                    a = 282.0f,
                    b = 282.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 21.0f,
                    dy1 = -4.61f,
                )
                // c 5.23 -1.43 14.9 -4.54 21.5 -6.93
                curveToRelative(
                    dx1 = 5.23f,
                    dy1 = -1.43f,
                    dx2 = 14.9f,
                    dy2 = -4.54f,
                    dx3 = 21.5f,
                    dy3 = -6.93f,
                )
                // s 16.5 -6.42 22 -8.98
                reflectiveCurveToRelative(
                    dx1 = 16.5f,
                    dy1 = -6.42f,
                    dx2 = 22.0f,
                    dy2 = -8.98f,
                )
                // c 5.5 -2.55 14.95 -7.63 21 -11.29
                curveToRelative(
                    dx1 = 5.5f,
                    dy1 = -2.55f,
                    dx2 = 14.95f,
                    dy2 = -7.63f,
                    dx3 = 21.0f,
                    dy3 = -11.29f,
                )
                // a 340 340 0 0 0 23 -15.62
                arcToRelative(
                    a = 340.0f,
                    b = 340.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 23.0f,
                    dy1 = -15.62f,
                )
                // c 7.16 -5.36 18.05 -15.08 27 -24.07
                curveToRelative(
                    dx1 = 7.16f,
                    dy1 = -5.36f,
                    dx2 = 18.05f,
                    dy2 = -15.08f,
                    dx3 = 27.0f,
                    dy3 = -24.07f,
                )
                // c 8.25 -8.3 19.73 -20.81 25.5 -27.81
                curveToRelative(
                    dx1 = 8.25f,
                    dy1 = -8.3f,
                    dx2 = 19.73f,
                    dy2 = -20.81f,
                    dx3 = 25.5f,
                    dy3 = -27.81f,
                )
                // s 11.06 -12.73 11.75 -12.72
                reflectiveCurveToRelative(
                    dx1 = 11.06f,
                    dy1 = -12.73f,
                    dx2 = 11.75f,
                    dy2 = -12.72f,
                )
                // c 0.79 0.01 0 2.49 -2.14 6.76
                curveToRelative(
                    dx1 = 0.79f,
                    dy1 = 0.01f,
                    dx2 = 0.0f,
                    dy2 = 2.49f,
                    dx3 = -2.14f,
                    dy3 = 6.76f,
                )
                // a 297 297 0 0 1 -7.84 14.25
                arcToRelative(
                    a = 297.0f,
                    b = 297.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -7.84f,
                    dy1 = 14.25f,
                )
                // a 699 699 0 0 1 -9.24 15
                arcToRelative(
                    a = 699.0f,
                    b = 699.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -9.24f,
                    dy1 = 15.0f,
                )
                // c -2.64 4.12 -9.35 13.34 -14.91 20.49
                curveToRelative(
                    dx1 = -2.64f,
                    dy1 = 4.12f,
                    dx2 = -9.35f,
                    dy2 = 13.34f,
                    dx3 = -14.91f,
                    dy3 = 20.49f,
                )
                // a 342 342 0 0 1 -22.12 25.08
                arcToRelative(
                    a = 342.0f,
                    b = 342.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -22.12f,
                    dy1 = 25.08f,
                )
                // c -6.6 6.65 -15.38 14.76 -19.5 18.01
                curveToRelative(
                    dx1 = -6.6f,
                    dy1 = 6.65f,
                    dx2 = -15.38f,
                    dy2 = 14.76f,
                    dx3 = -19.5f,
                    dy3 = 18.01f,
                )
                // c -4.13 3.25 -9.3 7.29 -11.5 8.97
                curveToRelative(
                    dx1 = -4.13f,
                    dy1 = 3.25f,
                    dx2 = -9.3f,
                    dy2 = 7.29f,
                    dx3 = -11.5f,
                    dy3 = 8.97f,
                )
                // s -8.05 5.77 -13 9.08
                reflectiveCurveToRelative(
                    dx1 = -8.05f,
                    dy1 = 5.77f,
                    dx2 = -13.0f,
                    dy2 = 9.08f,
                )
                // a 323 323 0 0 1 -18 11.06
                arcToRelative(
                    a = 323.0f,
                    b = 323.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -18.0f,
                    dy1 = 11.06f,
                )
                // a 493 493 0 0 1 -16 8.56
                arcToRelative(
                    a = 493.0f,
                    b = 493.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -16.0f,
                    dy1 = 8.56f,
                )
                // a 386 386 0 0 1 -17.5 8.02
                arcToRelative(
                    a = 386.0f,
                    b = 386.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -17.5f,
                    dy1 = 8.02f,
                )
                // a 395 395 0 0 1 -19.5 7.66
                arcToRelative(
                    a = 395.0f,
                    b = 395.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -19.5f,
                    dy1 = 7.66f,
                )
                // a 281 281 0 0 1 -18 5.53
                arcToRelative(
                    a = 281.0f,
                    b = 281.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -18.0f,
                    dy1 = 5.53f,
                )
                // c -4.95 1.3 -15.3 3.73 -23 5.38
                curveToRelative(
                    dx1 = -4.95f,
                    dy1 = 1.3f,
                    dx2 = -15.3f,
                    dy2 = 3.73f,
                    dx3 = -23.0f,
                    dy3 = 5.38f,
                )
                // a 330 330 0 0 1 -23.5 4.18
                arcToRelative(
                    a = 330.0f,
                    b = 330.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -23.5f,
                    dy1 = 4.18f,
                )
                // c -6.2 0.77 -20.09 1.02 -40 0.72
                curveToRelative(
                    dx1 = -6.2f,
                    dy1 = 0.77f,
                    dx2 = -20.09f,
                    dy2 = 1.02f,
                    dx3 = -40.0f,
                    dy3 = 0.72f,
                )
                // c -25.54 -0.37 -32.21 -0.78 -41 -2.52
                curveToRelative(
                    dx1 = -25.54f,
                    dy1 = -0.37f,
                    dx2 = -32.21f,
                    dy2 = -0.78f,
                    dx3 = -41.0f,
                    dy3 = -2.52f,
                )
                // a 964 964 0 0 1 -21.5 -4.55
                arcToRelative(
                    a = 964.0f,
                    b = 964.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -21.5f,
                    dy1 = -4.55f,
                )
                // a 239 239 0 0 1 -19 -5.15
                arcToRelative(
                    a = 239.0f,
                    b = 239.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -19.0f,
                    dy1 = -5.15f,
                )
                // a 694 694 0 0 1 -17.5 -6.21
                arcToRelative(
                    a = 694.0f,
                    b = 694.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -17.5f,
                    dy1 = -6.21f,
                )
                // c -5.23 -1.95 -17.6 -7.48 -27.5 -12.3
                curveToRelative(
                    dx1 = -5.23f,
                    dy1 = -1.95f,
                    dx2 = -17.6f,
                    dy2 = -7.48f,
                    dx3 = -27.5f,
                    dy3 = -12.3f,
                )
                // c -9.9 -4.81 -23.4 -12.34 -30 -16.72
                curveToRelative(
                    dx1 = -9.9f,
                    dy1 = -4.81f,
                    dx2 = -23.4f,
                    dy2 = -12.34f,
                    dx3 = -30.0f,
                    dy3 = -16.72f,
                )
                // a 490 490 0 0 1 -23.5 -16.87
                arcToRelative(
                    a = 490.0f,
                    b = 490.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -23.5f,
                    dy1 = -16.87f,
                )
                // c -6.32 -4.89 -18.27 -15.65 -26.53 -23.91
                curveToRelative(
                    dx1 = -6.32f,
                    dy1 = -4.89f,
                    dx2 = -18.27f,
                    dy2 = -15.65f,
                    dx3 = -26.53f,
                    dy3 = -23.91f,
                )
                // A 271 271 0 0 1 135.61 556
                arcTo(
                    horizontalEllipseRadius = 271.0f,
                    verticalEllipseRadius = 271.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 135.61f,
                    y1 = 556.0f,
                )
                // c -5.95 -7.98 -11.56 -15.74 -12.46 -17.26
                curveToRelative(
                    dx1 = -5.95f,
                    dy1 = -7.98f,
                    dx2 = -11.56f,
                    dy2 = -15.74f,
                    dx3 = -12.46f,
                    dy3 = -17.26f,
                )
                // q -1.65 -2.77 -4.15 -2.76
                quadToRelative(
                    dx1 = -1.65f,
                    dy1 = -2.77f,
                    dx2 = -4.15f,
                    dy2 = -2.76f,
                )
                // c -1.37 0.01 -4.97 0.64 -8 1.41
                curveToRelative(
                    dx1 = -1.37f,
                    dy1 = 0.01f,
                    dx2 = -4.97f,
                    dy2 = 0.64f,
                    dx3 = -8.0f,
                    dy3 = 1.41f,
                )
                // a 633 633 0 0 1 -15 3.47
                arcToRelative(
                    a = 633.0f,
                    b = 633.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -15.0f,
                    dy1 = 3.47f,
                )
                // a 592 592 0 0 1 -24.5 4.55
                arcToRelative(
                    a = 592.0f,
                    b = 592.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -24.5f,
                    dy1 = 4.55f,
                )
                // c -8.25 1.36 -18.82 2.75 -23.5 3.08
                curveToRelative(
                    dx1 = -8.25f,
                    dy1 = 1.36f,
                    dx2 = -18.82f,
                    dy2 = 2.75f,
                    dx3 = -23.5f,
                    dy3 = 3.08f,
                )
                // a 90 90 0 0 1 -16 -0.48
                arcToRelative(
                    a = 90.0f,
                    b = 90.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -16.0f,
                    dy1 = -0.48f,
                )
                // a 59 59 0 0 1 -13.4 -3.8
                arcToRelative(
                    a = 59.0f,
                    b = 59.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -13.4f,
                    dy1 = -3.8f,
                )
                // c -3.25 -1.49 -6.87 -3.83 -8.05 -5.21
                curveToRelative(
                    dx1 = -3.25f,
                    dy1 = -1.49f,
                    dx2 = -6.87f,
                    dy2 = -3.83f,
                    dx3 = -8.05f,
                    dy3 = -5.21f,
                )
                // a 23 23 0 0 1 -3.44 -6.5
                arcToRelative(
                    a = 23.0f,
                    b = 23.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.44f,
                    dy1 = -6.5f,
                )
                // c -1.01 -3.12 -1.05 -5.1 -0.16 -9
                curveToRelative(
                    dx1 = -1.01f,
                    dy1 = -3.12f,
                    dx2 = -1.05f,
                    dy2 = -5.1f,
                    dx3 = -0.16f,
                    dy3 = -9.0f,
                )
                // c 0.63 -2.75 2.68 -8.15 4.56 -12
                curveToRelative(
                    dx1 = 0.63f,
                    dy1 = -2.75f,
                    dx2 = 2.68f,
                    dy2 = -8.15f,
                    dx3 = 4.56f,
                    dy3 = -12.0f,
                )
                // s 5.8 -10.14 8.7 -13.97
                reflectiveCurveToRelative(
                    dx1 = 5.8f,
                    dy1 = -10.14f,
                    dx2 = 8.7f,
                    dy2 = -13.97f,
                )
                // a 330 330 0 0 1 15.36 -18
                arcToRelative(
                    a = 330.0f,
                    b = 330.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 15.36f,
                    dy1 = -18.0f,
                )
                // c 5.54 -6.07 17.2 -17.55 25.91 -25.53
                curveToRelative(
                    dx1 = 5.54f,
                    dy1 = -6.07f,
                    dx2 = 17.2f,
                    dy2 = -17.55f,
                    dx3 = 25.91f,
                    dy3 = -25.53f,
                )
                // s 16.05 -15.18 16.31 -16
                reflectiveCurveToRelative(
                    dx1 = 16.05f,
                    dy1 = -15.18f,
                    dx2 = 16.31f,
                    dy2 = -16.0f,
                )
                // s -0.73 -7.57 -2.21 -15
                reflectiveCurveToRelative(
                    dx1 = -0.73f,
                    dy1 = -7.57f,
                    dx2 = -2.21f,
                    dy2 = -15.0f,
                )
                // a 385 385 0 0 1 -4.23 -28
                arcToRelative(
                    a = 385.0f,
                    b = 385.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.23f,
                    dy1 = -28.0f,
                )
                // c -1.13 -10.57 -1.41 -20.2 -1.02 -35.5
                curveToRelative(
                    dx1 = -1.13f,
                    dy1 = -10.57f,
                    dx2 = -1.41f,
                    dy2 = -20.2f,
                    dx3 = -1.02f,
                    dy3 = -35.5f,
                )
                // c 0.29 -11.55 1.24 -25.73 2.12 -31.5
                curveToRelative(
                    dx1 = 0.29f,
                    dy1 = -11.55f,
                    dx2 = 1.24f,
                    dy2 = -25.73f,
                    dx3 = 2.12f,
                    dy3 = -31.5f,
                )
                // a 408 408 0 0 1 4.51 -23.5
                arcToRelative(
                    a = 408.0f,
                    b = 408.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.51f,
                    dy1 = -23.5f,
                )
                // c 1.6 -7.15 4.56 -18.4 6.56 -25
                curveToRelative(
                    dx1 = 1.6f,
                    dy1 = -7.15f,
                    dx2 = 4.56f,
                    dy2 = -18.4f,
                    dx3 = 6.56f,
                    dy3 = -25.0f,
                )
                // s 5.67 -16.95 8.15 -23
                reflectiveCurveToRelative(
                    dx1 = 5.67f,
                    dy1 = -16.95f,
                    dx2 = 8.15f,
                    dy2 = -23.0f,
                )
                // a 408.03 408.03 0 0 1 25.89 -50.5
                arcToRelative(
                    a = 408.03f,
                    b = 408.03f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 25.89f,
                    dy1 = -50.5f,
                )
                // a 377 377 0 0 1 14.63 -21
                arcToRelative(
                    a = 377.0f,
                    b = 377.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 14.63f,
                    dy1 = -21.0f,
                )
                // A 375 375 0 0 1 153 160.5
                arcTo(
                    horizontalEllipseRadius = 375.0f,
                    verticalEllipseRadius = 375.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 153.0f,
                    y1 = 160.5f,
                )
                // c 6.68 -7.15 16.05 -16.51 20.82 -20.8
                curveToRelative(
                    dx1 = 6.68f,
                    dy1 = -7.15f,
                    dx2 = 16.05f,
                    dy2 = -16.51f,
                    dx3 = 20.82f,
                    dy3 = -20.8f,
                )
                // a 337 337 0 0 1 20.68 -16.65
                arcToRelative(
                    a = 337.0f,
                    b = 337.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 20.68f,
                    dy1 = -16.65f,
                )
                // a 390 390 0 0 1 22.75 -15.46
                arcToRelative(
                    a = 390.0f,
                    b = 390.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 22.75f,
                    dy1 = -15.46f,
                )
                // a 358 358 0 0 1 22.5 -12.45
                arcToRelative(
                    a = 358.0f,
                    b = 358.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 22.5f,
                    dy1 = -12.45f,
                )
                // A 421 421 0 0 1 264 84.21
                arcTo(
                    horizontalEllipseRadius = 421.0f,
                    verticalEllipseRadius = 421.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 264.0f,
                    y1 = 84.21f,
                )
                // a 292 292 0 0 1 23 -8.11
                arcToRelative(
                    a = 292.0f,
                    b = 292.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 23.0f,
                    dy1 = -8.11f,
                )
                // a 565 565 0 0 1 26 -6.57
                arcToRelative(
                    a = 565.0f,
                    b = 565.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 26.0f,
                    dy1 = -6.57f,
                )
                // c 8.52 -1.94 20.9 -4.2 27.5 -5.02
                curveToRelative(
                    dx1 = 8.52f,
                    dy1 = -1.94f,
                    dx2 = 20.9f,
                    dy2 = -4.2f,
                    dx3 = 27.5f,
                    dy3 = -5.02f,
                )
                // c 6.6 -0.83 14.48 -1.64 17.5 -1.82
                curveToRelative(
                    dx1 = 6.6f,
                    dy1 = -0.83f,
                    dx2 = 14.48f,
                    dy2 = -1.64f,
                    dx3 = 17.5f,
                    dy3 = -1.82f,
                )
                // s 8.43 -0.4 12 -0.49
                reflectiveCurveToRelative(
                    dx1 = 8.43f,
                    dy1 = -0.4f,
                    dx2 = 12.0f,
                    dy2 = -0.49f,
                )
                // m 332.25 141.38
                moveToRelative(dx = 332.25f, dy = 141.38f)
                // c 5.7 0.24 11.52 1.11 14 2.07
                curveToRelative(
                    dx1 = 5.7f,
                    dy1 = 0.24f,
                    dx2 = 11.52f,
                    dy2 = 1.11f,
                    dx3 = 14.0f,
                    dy3 = 2.07f,
                )
                // a 38 38 0 0 1 7.69 4.25
                arcToRelative(
                    a = 38.0f,
                    b = 38.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 7.69f,
                    dy1 = 4.25f,
                )
                // a 18 18 0 0 1 4.86 6.1
                arcToRelative(
                    a = 18.0f,
                    b = 18.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.86f,
                    dy1 = 6.1f,
                )
                // c 1.13 2.78 1.2 4.52 0.33 8.5
                curveToRelative(
                    dx1 = 1.13f,
                    dy1 = 2.78f,
                    dx2 = 1.2f,
                    dy2 = 4.52f,
                    dx3 = 0.33f,
                    dy3 = 8.5f,
                )
                // c -0.68 3.09 -2.96 7.68 -5.97 12
                curveToRelative(
                    dx1 = -0.68f,
                    dy1 = 3.09f,
                    dx2 = -2.96f,
                    dy2 = 7.68f,
                    dx3 = -5.97f,
                    dy3 = 12.0f,
                )
                // c -2.68 3.85 -8.45 10.6 -12.82 15
                curveToRelative(
                    dx1 = -2.68f,
                    dy1 = 3.85f,
                    dx2 = -8.45f,
                    dy2 = 10.6f,
                    dx3 = -12.82f,
                    dy3 = 15.0f,
                )
                // s -14.44 13.23 -22.39 19.62
                reflectiveCurveToRelative(
                    dx1 = -14.44f,
                    dy1 = 13.23f,
                    dx2 = -22.39f,
                    dy2 = 19.62f,
                )
                // A 608 608 0 0 1 661 291.46
                arcTo(
                    horizontalEllipseRadius = 608.0f,
                    verticalEllipseRadius = 608.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 661.0f,
                    y1 = 291.46f,
                )
                // a 789 789 0 0 1 -24.5 16.32
                arcToRelative(
                    a = 789.0f,
                    b = 789.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -24.5f,
                    dy1 = 16.32f,
                )
                // c -6.6 4.18 -15.83 9.92 -20.5 12.77
                curveToRelative(
                    dx1 = -6.6f,
                    dy1 = 4.18f,
                    dx2 = -15.83f,
                    dy2 = 9.92f,
                    dx3 = -20.5f,
                    dy3 = 12.77f,
                )
                // s -16.37 9.67 -26 15.16
                reflectiveCurveToRelative(
                    dx1 = -16.37f,
                    dy1 = 9.67f,
                    dx2 = -26.0f,
                    dy2 = 15.16f,
                )
                // a 2187 2187 0 0 1 -36 20.02
                arcToRelative(
                    a = 2187.0f,
                    b = 2187.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -36.0f,
                    dy1 = 20.02f,
                )
                // a 2261 2261 0 0 1 -51.5 26.52
                arcToRelative(
                    a = 2261.0f,
                    b = 2261.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -51.5f,
                    dy1 = 26.52f,
                )
                // c -18.15 9.07 -42 20.79 -53 26.04
                curveToRelative(
                    dx1 = -18.15f,
                    dy1 = 9.07f,
                    dx2 = -42.0f,
                    dy2 = 20.79f,
                    dx3 = -53.0f,
                    dy3 = 26.04f,
                )
                // s -21.35 9.87 -23 10.26
                reflectiveCurveToRelative(
                    dx1 = -21.35f,
                    dy1 = 9.87f,
                    dx2 = -23.0f,
                    dy2 = 10.26f,
                )
                // c -2.04 0.48 -2.68 0.38 -2 -0.31
                curveToRelative(
                    dx1 = -2.04f,
                    dy1 = 0.48f,
                    dx2 = -2.68f,
                    dy2 = 0.38f,
                    dx3 = -2.0f,
                    dy3 = -0.31f,
                )
                // c 0.55 -0.57 11.8 -6.97 25 -14.25
                curveToRelative(
                    dx1 = 0.55f,
                    dy1 = -0.57f,
                    dx2 = 11.8f,
                    dy2 = -6.97f,
                    dx3 = 25.0f,
                    dy3 = -14.25f,
                )
                // c 13.2 -7.27 32.55 -17.95 43 -23.74
                curveToRelative(
                    dx1 = 13.2f,
                    dy1 = -7.27f,
                    dx2 = 32.55f,
                    dy2 = -17.95f,
                    dx3 = 43.0f,
                    dy3 = -23.74f,
                )
                // a 1802 1802 0 0 0 33.5 -19.06
                arcToRelative(
                    a = 1802.0f,
                    b = 1802.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 33.5f,
                    dy1 = -19.06f,
                )
                // c 7.98 -4.69 22.38 -13.46 32 -19.5
                curveToRelative(
                    dx1 = 7.98f,
                    dy1 = -4.69f,
                    dx2 = 22.38f,
                    dy2 = -13.46f,
                    dx3 = 32.0f,
                    dy3 = -19.5f,
                )
                // s 23.58 -15.1 31 -20.14
                reflectiveCurveToRelative(
                    dx1 = 23.58f,
                    dy1 = -15.1f,
                    dx2 = 31.0f,
                    dy2 = -20.14f,
                )
                // a 935 935 0 0 0 27 -19.19
                arcToRelative(
                    a = 935.0f,
                    b = 935.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 27.0f,
                    dy1 = -19.19f,
                )
                // a 391 391 0 0 0 25 -20.52
                arcToRelative(
                    a = 391.0f,
                    b = 391.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 25.0f,
                    dy1 = -20.52f,
                )
                // c 6.33 -5.77 13.36 -12.94 15.63 -15.92
                curveToRelative(
                    dx1 = 6.33f,
                    dy1 = -5.77f,
                    dx2 = 13.36f,
                    dy2 = -12.94f,
                    dx3 = 15.63f,
                    dy3 = -15.92f,
                )
                // a 73 73 0 0 0 5.88 -8.92
                arcToRelative(
                    a = 73.0f,
                    b = 73.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 5.88f,
                    dy1 = -8.92f,
                )
                // c 0.95 -1.92 1.93 -5.75 2.18 -8.5
                curveToRelative(
                    dx1 = 0.95f,
                    dy1 = -1.92f,
                    dx2 = 1.93f,
                    dy2 = -5.75f,
                    dx3 = 2.18f,
                    dy3 = -8.5f,
                )
                // c 0.33 -3.86 -0.04 -5.74 -1.63 -8.25
                curveToRelative(
                    dx1 = 0.33f,
                    dy1 = -3.86f,
                    dx2 = -0.04f,
                    dy2 = -5.74f,
                    dx3 = -1.63f,
                    dy3 = -8.25f,
                )
                // c -1.22 -1.93 -4.2 -4.29 -12.56 -8.35
                curveToRelative(
                    dx1 = -1.22f,
                    dy1 = -1.93f,
                    dx2 = -4.2f,
                    dy2 = -4.29f,
                    dx3 = -12.56f,
                    dy3 = -8.35f,
                )
                // l -24.5 0.38
                lineToRelative(dx = -24.5f, dy = 0.38f)
                // c -21.21 0.33 -26.51 0.75 -39.5 3.1
                curveToRelative(
                    dx1 = -21.21f,
                    dy1 = 0.33f,
                    dx2 = -26.51f,
                    dy2 = 0.75f,
                    dx3 = -39.5f,
                    dy3 = 3.1f,
                )
                // a 924 924 0 0 0 -31 6.29
                arcToRelative(
                    a = 924.0f,
                    b = 924.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -31.0f,
                    dy1 = 6.29f,
                )
                // c -8.8 1.96 -19.15 4.19 -23 4.94
                curveToRelative(
                    dx1 = -8.8f,
                    dy1 = 1.96f,
                    dx2 = -19.15f,
                    dy2 = 4.19f,
                    dx3 = -23.0f,
                    dy3 = 4.94f,
                )
                // s -7.23 1.12 -7.5 0.82
                reflectiveCurveToRelative(
                    dx1 = -7.23f,
                    dy1 = 1.12f,
                    dx2 = -7.5f,
                    dy2 = 0.82f,
                )
                // s 0.17 -0.96 1 -1.47
                reflectiveCurveToRelative(
                    dx1 = 0.17f,
                    dy1 = -0.96f,
                    dx2 = 1.0f,
                    dy2 = -1.47f,
                )
                // s 9.6 -3.62 19.5 -6.9
                reflectiveCurveToRelative(
                    dx1 = 9.6f,
                    dy1 = -3.62f,
                    dx2 = 19.5f,
                    dy2 = -6.9f,
                )
                // s 24.75 -8.05 33 -10.58
                reflectiveCurveToRelative(
                    dx1 = 24.75f,
                    dy1 = -8.05f,
                    dx2 = 33.0f,
                    dy2 = -10.58f,
                )
                // a 1284 1284 0 0 1 31.5 -9.14
                arcToRelative(
                    a = 1284.0f,
                    b = 1284.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 31.5f,
                    dy1 = -9.14f,
                )
                // a 781 781 0 0 1 31.5 -7.84
                arcToRelative(
                    a = 781.0f,
                    b = 781.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 31.5f,
                    dy1 = -7.84f,
                )
                // a 775 775 0 0 1 24 -4.94
                arcToRelative(
                    a = 775.0f,
                    b = 775.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 24.0f,
                    dy1 = -4.94f,
                )
                // a 278 278 0 0 1 18 -2.51
                arcToRelative(
                    a = 278.0f,
                    b = 278.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 18.0f,
                    dy1 = -2.51f,
                )
                // c 4.95 -0.49 13.39 -0.7 18.75 -0.47
                curveToRelative(
                    dx1 = 4.95f,
                    dy1 = -0.49f,
                    dx2 = 13.39f,
                    dy2 = -0.7f,
                    dx3 = 18.75f,
                    dy3 = -0.47f,
                )
                // m -40.5 -106.51
                moveToRelative(dx = -40.5f, dy = -106.51f)
                // c 1.24 -0.04 4.84 0.74 8 1.73
                curveToRelative(
                    dx1 = 1.24f,
                    dy1 = -0.04f,
                    dx2 = 4.84f,
                    dy2 = 0.74f,
                    dx3 = 8.0f,
                    dy3 = 1.73f,
                )
                // a 46 46 0 0 1 10.25 4.9
                arcToRelative(
                    a = 46.0f,
                    b = 46.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 10.25f,
                    dy1 = 4.9f,
                )
                // a 51 51 0 0 1 6.69 5.44
                arcToRelative(
                    a = 51.0f,
                    b = 51.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 6.69f,
                    dy1 = 5.44f,
                )
                // c 1.2 1.3 3.63 5.51 5.4 9.36
                curveToRelative(
                    dx1 = 1.2f,
                    dy1 = 1.3f,
                    dx2 = 3.63f,
                    dy2 = 5.51f,
                    dx3 = 5.4f,
                    dy3 = 9.36f,
                )
                // c 2.43 5.3 3.2 8.34 3.15 12.5
                curveToRelative(
                    dx1 = 2.43f,
                    dy1 = 5.3f,
                    dx2 = 3.2f,
                    dy2 = 8.34f,
                    dx3 = 3.15f,
                    dy3 = 12.5f,
                )
                // a 39 39 0 0 1 -2.32 11.5
                arcToRelative(
                    a = 39.0f,
                    b = 39.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.32f,
                    dy1 = 11.5f,
                )
                // a 33 33 0 0 1 -7.34 11.28
                arcToRelative(
                    a = 33.0f,
                    b = 33.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -7.34f,
                    dy1 = 11.28f,
                )
                // c -3.05 3.17 -7.08 6.11 -10.08 7.36
                curveToRelative(
                    dx1 = -3.05f,
                    dy1 = 3.17f,
                    dx2 = -7.08f,
                    dy2 = 6.11f,
                    dx3 = -10.08f,
                    dy3 = 7.36f,
                )
                // a 60 60 0 0 1 -10.5 3.04
                arcToRelative(
                    a = 60.0f,
                    b = 60.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -10.5f,
                    dy1 = 3.04f,
                )
                // c -4.73 0.83 -6.47 0.64 -12.47 -1.36
                curveToRelative(
                    dx1 = -4.73f,
                    dy1 = 0.83f,
                    dx2 = -6.47f,
                    dy2 = 0.64f,
                    dx3 = -12.47f,
                    dy3 = -1.36f,
                )
                // c -3.84 -1.28 -8.79 -3.71 -11 -5.4
                curveToRelative(
                    dx1 = -3.84f,
                    dy1 = -1.28f,
                    dx2 = -8.79f,
                    dy2 = -3.71f,
                    dx3 = -11.0f,
                    dy3 = -5.4f,
                )
                // a 38 38 0 0 1 -7.21 -8
                arcToRelative(
                    a = 38.0f,
                    b = 38.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -7.21f,
                    dy1 = -8.0f,
                )
                // a 48 48 0 0 1 -4.95 -11.42
                arcToRelative(
                    a = 48.0f,
                    b = 48.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.95f,
                    dy1 = -11.42f,
                )
                // c -1.5 -5.56 -1.58 -7.29 -0.55 -12
                curveToRelative(
                    dx1 = -1.5f,
                    dy1 = -5.56f,
                    dx2 = -1.58f,
                    dy2 = -7.29f,
                    dx3 = -0.55f,
                    dy3 = -12.0f,
                )
                // a 52 52 0 0 1 3.15 -9.5
                arcToRelative(
                    a = 52.0f,
                    b = 52.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.15f,
                    dy1 = -9.5f,
                )
                // c 1.07 -2.2 3.98 -6.14 6.48 -8.75
                curveToRelative(
                    dx1 = 1.07f,
                    dy1 = -2.2f,
                    dx2 = 3.98f,
                    dy2 = -6.14f,
                    dx3 = 6.48f,
                    dy3 = -8.75f,
                )
                // s 5.9 -5.42 7.55 -6.25
                reflectiveCurveToRelative(
                    dx1 = 5.9f,
                    dy1 = -5.42f,
                    dx2 = 7.55f,
                    dy2 = -6.25f,
                )
                // a 56 56 0 0 1 8.25 -2.93
                arcToRelative(
                    a = 56.0f,
                    b = 56.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 8.25f,
                    dy1 = -2.93f,
                )
                // c 2.89 -0.79 6.26 -1.47 7.5 -1.5
                curveToRelative(
                    dx1 = 2.89f,
                    dy1 = -0.79f,
                    dx2 = 6.26f,
                    dy2 = -1.47f,
                    dx3 = 7.5f,
                    dy3 = -1.5f,
                )
                // M 374.23 193
                moveTo(x = 374.23f, y = 193.0f)
                // c 0.6 0 1.23 1.46 1.39 3.25
                curveToRelative(
                    dx1 = 0.6f,
                    dy1 = 0.0f,
                    dx2 = 1.23f,
                    dy2 = 1.46f,
                    dx3 = 1.39f,
                    dy3 = 3.25f,
                )
                // c 0.17 1.79 1.01 14.95 1.87 29.25
                curveToRelative(
                    dx1 = 0.17f,
                    dy1 = 1.79f,
                    dx2 = 1.01f,
                    dy2 = 14.95f,
                    dx3 = 1.87f,
                    dy3 = 29.25f,
                )
                // c 0.85 14.3 2.21 31.63 3.01 38.5
                curveToRelative(
                    dx1 = 0.85f,
                    dy1 = 14.3f,
                    dx2 = 2.21f,
                    dy2 = 31.63f,
                    dx3 = 3.01f,
                    dy3 = 38.5f,
                )
                // s 2.16 16.32 3.02 21
                reflectiveCurveToRelative(
                    dx1 = 2.16f,
                    dy1 = 16.32f,
                    dx2 = 3.02f,
                    dy2 = 21.0f,
                )
                // s 2.95 13 4.64 18.5
                reflectiveCurveToRelative(
                    dx1 = 2.95f,
                    dy1 = 13.0f,
                    dx2 = 4.64f,
                    dy2 = 18.5f,
                )
                // s 4.81 13.6 6.93 18
                reflectiveCurveToRelative(
                    dx1 = 4.81f,
                    dy1 = 13.6f,
                    dx2 = 6.93f,
                    dy2 = 18.0f,
                )
                // s 5.99 10.48 8.59 13.5
                reflectiveCurveToRelative(
                    dx1 = 5.99f,
                    dy1 = 10.48f,
                    dx2 = 8.59f,
                    dy2 = 13.5f,
                )
                // c 2.61 3.02 7.78 8.2 11.48 11.5
                curveToRelative(
                    dx1 = 2.61f,
                    dy1 = 3.02f,
                    dx2 = 7.78f,
                    dy2 = 8.2f,
                    dx3 = 11.48f,
                    dy3 = 11.5f,
                )
                // s 8.89 7.23 11.54 8.73
                reflectiveCurveToRelative(
                    dx1 = 8.89f,
                    dy1 = 7.23f,
                    dx2 = 11.54f,
                    dy2 = 8.73f,
                )
                // c 2.64 1.51 9.53 4.35 15.3 6.32
                curveToRelative(
                    dx1 = 2.64f,
                    dy1 = 1.51f,
                    dx2 = 9.53f,
                    dy2 = 4.35f,
                    dx3 = 15.3f,
                    dy3 = 6.32f,
                )
                // c 6.49 2.21 15.65 4.33 24 5.55
                curveToRelative(
                    dx1 = 6.49f,
                    dy1 = 2.21f,
                    dx2 = 15.65f,
                    dy2 = 4.33f,
                    dx3 = 24.0f,
                    dy3 = 5.55f,
                )
                // a 356 356 0 0 1 15.25 2.44
                arcToRelative(
                    a = 356.0f,
                    b = 356.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 15.25f,
                    dy1 = 2.44f,
                )
                // c 0.96 0.25 1.75 0.91 1.75 1.46
                curveToRelative(
                    dx1 = 0.96f,
                    dy1 = 0.25f,
                    dx2 = 1.75f,
                    dy2 = 0.91f,
                    dx3 = 1.75f,
                    dy3 = 1.46f,
                )
                // s -0.79 1.2 -1.75 1.45
                reflectiveCurveToRelative(
                    dx1 = -0.79f,
                    dy1 = 1.2f,
                    dx2 = -1.75f,
                    dy2 = 1.45f,
                )
                // a 170 170 0 0 1 -10.25 1.57
                arcToRelative(
                    a = 170.0f,
                    b = 170.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -10.25f,
                    dy1 = 1.57f,
                )
                // a 257 257 0 0 0 -17 2.93
                arcToRelative(
                    a = 257.0f,
                    b = 257.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -17.0f,
                    dy1 = 2.93f,
                )
                // c -4.68 1 -12.1 3.31 -16.5 5.14
                curveToRelative(
                    dx1 = -4.68f,
                    dy1 = 1.0f,
                    dx2 = -12.1f,
                    dy2 = 3.31f,
                    dx3 = -16.5f,
                    dy3 = 5.14f,
                )
                // a 161 161 0 0 0 -12.5 5.78
                arcToRelative(
                    a = 161.0f,
                    b = 161.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -12.5f,
                    dy1 = 5.78f,
                )
                // c -2.48 1.35 -9.27 7.22 -15.1 13.04
                curveToRelative(
                    dx1 = -2.48f,
                    dy1 = 1.35f,
                    dx2 = -9.27f,
                    dy2 = 7.22f,
                    dx3 = -15.1f,
                    dy3 = 13.04f,
                )
                // c -9.08 9.07 -11.17 11.81 -14.64 19.09
                curveToRelative(
                    dx1 = -9.08f,
                    dy1 = 9.07f,
                    dx2 = -11.17f,
                    dy2 = 11.81f,
                    dx3 = -14.64f,
                    dy3 = 19.09f,
                )
                // a 128 128 0 0 0 -6.1 15.5
                arcToRelative(
                    a = 128.0f,
                    b = 128.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -6.1f,
                    dy1 = 15.5f,
                )
                // a 249 249 0 0 0 -4.13 17.5
                arcToRelative(
                    a = 249.0f,
                    b = 249.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -4.13f,
                    dy1 = 17.5f,
                )
                // a 366 366 0 0 0 -3.48 22
                arcToRelative(
                    a = 366.0f,
                    b = 366.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -3.48f,
                    dy1 = 22.0f,
                )
                // a 638 638 0 0 0 -2.53 29.5
                arcToRelative(
                    a = 638.0f,
                    b = 638.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -2.53f,
                    dy1 = 29.5f,
                )
                // c -0.62 9.9 -1.54 23.4 -2.05 30
                curveToRelative(
                    dx1 = -0.62f,
                    dy1 = 9.9f,
                    dx2 = -1.54f,
                    dy2 = 23.4f,
                    dx3 = -2.05f,
                    dy3 = 30.0f,
                )
                // s -1.15 12.23 -1.44 12.5
                reflectiveCurveToRelative(
                    dx1 = -1.15f,
                    dy1 = 12.23f,
                    dx2 = -1.44f,
                    dy2 = 12.5f,
                )
                // s -0.87 0.12 -1.28 -0.34
                reflectiveCurveToRelative(
                    dx1 = -0.87f,
                    dy1 = 0.12f,
                    dx2 = -1.28f,
                    dy2 = -0.34f,
                )
                // s -1.07 -6.32 -1.46 -13
                reflectiveCurveToRelative(
                    dx1 = -1.07f,
                    dy1 = -6.32f,
                    dx2 = -1.46f,
                    dy2 = -13.0f,
                )
                // c -0.39 -6.69 -1.2 -19.58 -1.81 -28.66
                curveToRelative(
                    dx1 = -0.39f,
                    dy1 = -6.69f,
                    dx2 = -1.2f,
                    dy2 = -19.58f,
                    dx3 = -1.81f,
                    dy3 = -28.66f,
                )
                // a 611 611 0 0 0 -3.03 -31.5
                arcToRelative(
                    a = 611.0f,
                    b = 611.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -3.03f,
                    dy1 = -31.5f,
                )
                // a 331 331 0 0 0 -4.33 -25.5
                arcToRelative(
                    a = 331.0f,
                    b = 331.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -4.33f,
                    dy1 = -25.5f,
                )
                // a 167 167 0 0 0 -5.48 -18.5
                arcToRelative(
                    a = 167.0f,
                    b = 167.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -5.48f,
                    dy1 = -18.5f,
                )
                // c -1.7 -4.4 -4.71 -10.7 -6.71 -14
                curveToRelative(
                    dx1 = -1.7f,
                    dy1 = -4.4f,
                    dx2 = -4.71f,
                    dy2 = -10.7f,
                    dx3 = -6.71f,
                    dy3 = -14.0f,
                )
                // c -1.99 -3.3 -5.49 -8.22 -7.77 -10.94
                curveToRelative(
                    dx1 = -1.99f,
                    dy1 = -3.3f,
                    dx2 = -5.49f,
                    dy2 = -8.22f,
                    dx3 = -7.77f,
                    dy3 = -10.94f,
                )
                // a 99 99 0 0 0 -10.28 -10
                arcToRelative(
                    a = 99.0f,
                    b = 99.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -10.28f,
                    dy1 = -10.0f,
                )
                // a 91 91 0 0 0 -14.5 -9.21
                arcToRelative(
                    a = 91.0f,
                    b = 91.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -14.5f,
                    dy1 = -9.21f,
                )
                // a 113 113 0 0 0 -15.38 -6.18
                arcToRelative(
                    a = 113.0f,
                    b = 113.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -15.38f,
                    dy1 = -6.18f,
                )
                // a 181 181 0 0 0 -15.5 -3.54
                arcToRelative(
                    a = 181.0f,
                    b = 181.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -15.5f,
                    dy1 = -3.54f,
                )
                // a 240 240 0 0 0 -15 -2.14
                arcToRelative(
                    a = 240.0f,
                    b = 240.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -15.0f,
                    dy1 = -2.14f,
                )
                // c -3.58 -0.34 -7.18 -1.04 -8 -1.56
                curveToRelative(
                    dx1 = -3.58f,
                    dy1 = -0.34f,
                    dx2 = -7.18f,
                    dy2 = -1.04f,
                    dx3 = -8.0f,
                    dy3 = -1.56f,
                )
                // c -1.33 -0.83 -1.33 -1.03 0 -1.85
                curveToRelative(
                    dx1 = -1.33f,
                    dy1 = -0.83f,
                    dx2 = -1.33f,
                    dy2 = -1.03f,
                    dx3 = 0.0f,
                    dy3 = -1.85f,
                )
                // c 0.82 -0.5 5.32 -1.21 10 -1.56
                curveToRelative(
                    dx1 = 0.82f,
                    dy1 = -0.5f,
                    dx2 = 5.32f,
                    dy2 = -1.21f,
                    dx3 = 10.0f,
                    dy3 = -1.56f,
                )
                // s 13 -1.7 18.5 -2.99
                reflectiveCurveToRelative(
                    dx1 = 13.0f,
                    dy1 = -1.7f,
                    dx2 = 18.5f,
                    dy2 = -2.99f,
                )
                // s 13.6 -3.72 18 -5.4
                reflectiveCurveToRelative(
                    dx1 = 13.6f,
                    dy1 = -3.72f,
                    dx2 = 18.0f,
                    dy2 = -5.4f,
                )
                // a 90 90 0 0 0 14 -6.93
                arcToRelative(
                    a = 90.0f,
                    b = 90.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 14.0f,
                    dy1 = -6.93f,
                )
                // c 3.3 -2.14 8.42 -6.09 11.38 -8.79
                curveToRelative(
                    dx1 = 3.3f,
                    dy1 = -2.14f,
                    dx2 = 8.42f,
                    dy2 = -6.09f,
                    dx3 = 11.38f,
                    dy3 = -8.79f,
                )
                // a 79 79 0 0 0 10.04 -11.91
                arcToRelative(
                    a = 79.0f,
                    b = 79.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 10.04f,
                    dy1 = -11.91f,
                )
                // a 108 108 0 0 0 8 -15
                arcToRelative(
                    a = 108.0f,
                    b = 108.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 8.0f,
                    dy1 = -15.0f,
                )
                // c 1.84 -4.4 4.42 -11.6 5.74 -16
                curveToRelative(
                    dx1 = 1.84f,
                    dy1 = -4.4f,
                    dx2 = 4.42f,
                    dy2 = -11.6f,
                    dx3 = 5.74f,
                    dy3 = -16.0f,
                )
                // s 3.26 -13.4 4.32 -20
                reflectiveCurveToRelative(
                    dx1 = 3.26f,
                    dy1 = -13.4f,
                    dx2 = 4.32f,
                    dy2 = -20.0f,
                )
                // c 1.05 -6.6 2.59 -19.42 3.42 -28.5
                curveToRelative(
                    dx1 = 1.05f,
                    dy1 = -6.6f,
                    dx2 = 2.59f,
                    dy2 = -19.42f,
                    dx3 = 3.42f,
                    dy3 = -28.5f,
                )
                // s 1.99 -25.05 2.59 -35.5
                reflectiveCurveToRelative(
                    dx1 = 1.99f,
                    dy1 = -25.05f,
                    dx2 = 2.59f,
                    dy2 = -35.5f,
                )
                // s 1.32 -20.24 1.61 -21.75
                reflectiveCurveToRelative(
                    dx1 = 1.32f,
                    dy1 = -20.24f,
                    dx2 = 1.61f,
                    dy2 = -21.75f,
                )
                // s 1.03 -2.75 1.63 -2.75
                reflectiveCurveToRelative(
                    dx1 = 1.03f,
                    dy1 = -2.75f,
                    dx2 = 1.63f,
                    dy2 = -2.75f,
                )
            }
        }.build().also { _bot = it }
    }


@Suppress("ObjectPropertyName")
private var _bot: ImageVector? = null
