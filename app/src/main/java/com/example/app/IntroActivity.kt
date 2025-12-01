package com.wiseyoung.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.ThemeWrapper
import com.wiseyoung.app.R

class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                IntroScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun IntroScreen(onClose: () -> Unit) {
    val listState = rememberLazyListState()
    
    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Intro Header (Logo)
            item {
                IntroHeaderSection()
            }
            
            // 2. Personalized Policy (Growth) - 1번 사진
            item {
                ScrollAnimationItem(listState = listState) { isVisible ->
                    FeatureSection(
                        title = "나에게 딱 맞는\n정책으로 성장하세요",
                        description = "수많은 정보 속에서 헤매지 마세요.\nAI가 당신의 상황을 분석해\n꼭 필요한 지원 정책만 골라드립니다.",
                        imageRes = R.drawable.intro_growth, 
                        imageContentDescription = "Growth",
                        backgroundColor = Color(0xFFFFF7ED), // 연한 주황
                        accentColor = AppColors.Orange,
                        isVisible = isVisible,
                        isImageLeft = false
                    )
                }
            }
            
            // 3. Housing Support (Home & Money) - 2번 사진
            item {
                ScrollAnimationItem(listState = listState) { isVisible ->
                    FeatureSection(
                        title = "내 집 마련의 꿈,\n더 가까이",
                        description = "보증금 지원부터 맞춤형 임대주택까지.\n주거 부담은 줄이고,\n안정적인 미래를 계획하세요.",
                        imageRes = R.drawable.intro_housing, 
                        imageContentDescription = "Housing",
                        backgroundColor = Color(0xFFEFF6FF), // 연한 파랑
                        accentColor = AppColors.LightBlue,
                        isVisible = isVisible,
                        isImageLeft = true
                    )
                }
            }
            
            // 4. Community (Bench/Together) - 4번 사진 (추가됨!)
            item {
                ScrollAnimationItem(listState = listState) { isVisible ->
                    FeatureSection(
                        title = "함께라면\n더 든든하니까",
                        description = "혼자 고민하지 마세요.\n비슷한 꿈을 가진 청년들과\n정보를 나누고 함께 성장해요.",
                        imageRes = R.drawable.intro_together, // 파일명: intro_together.png
                        imageContentDescription = "Together",
                        backgroundColor = Color(0xFFF0FDF4), // 연한 초록
                        accentColor = AppColors.Success,
                        isVisible = isVisible,
                        isImageLeft = false
                    )
                }
            }
            
            // 5. Guide (Shining Person) - 3번 사진
            item {
                ScrollAnimationItem(listState = listState) { isVisible ->
                    FeatureSection(
                        title = "복잡한 세상 속\n든든한 가이드",
                        description = "길을 잃지 않도록,\nWiseYoung이 당신의 빛나는\n내일을 응원하고 함께합니다.",
                        imageRes = R.drawable.intro_guide, 
                        imageContentDescription = "Guide",
                        backgroundColor = Color(0xFFF5F3FF), // 연한 보라
                        accentColor = AppColors.Purple,
                        isVisible = isVisible,
                        isImageLeft = true
                    )
                }
            }
            
            // 6. Outro & Start Button
            item {
                OutroSection(onStart = onClose)
            }
        }
    }
}

@Composable
fun IntroHeaderSection() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(1000)
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(1000)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp) // 화면 가득 채우는 느낌
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        AppColors.LightBlue.copy(alpha = 0.05f)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(animatedScale)
                .alpha(animatedAlpha)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = AppColors.LightBlue.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.wy_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "WiseYoung",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            modifier = Modifier
                .alpha(animatedAlpha)
                .offset(y = if (isVisible) 0.dp else 20.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "청년을 위한\n가장 똑똑한 가이드",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = AppColors.TextSecondary,
            lineHeight = 34.sp,
            modifier = Modifier.alpha(animatedAlpha)
        )
        
        Spacer(modifier = Modifier.height(60.dp))
        
        // Scroll Down Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(0.6f)
        ) {
            Text(
                text = "Scroll",
                fontSize = 12.sp,
                color = AppColors.TextTertiary
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AppColors.TextTertiary
            )
        }
    }
}

@Composable
fun FeatureSection(
    title: String,
    description: String,
    imageRes: Int,
    imageContentDescription: String,
    backgroundColor: Color,
    accentColor: Color,
    isVisible: Boolean,
    isImageLeft: Boolean
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800)
    )
    
    val animatedTranslation by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = tween(800)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 60.dp, horizontal = 24.dp)
            .graphicsLayer {
                alpha = animatedAlpha
            }
    ) {
        if (isImageLeft) {
            // Image Left Layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ImageCard(
                    imageRes = imageRes,
                    contentDescription = imageContentDescription,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            translationX = -animatedTranslation // 왼쪽에서 등장
                        }
                )
                
                Spacer(modifier = Modifier.width(24.dp))
                
                TextContent(
                    title = title,
                    description = description,
                    accentColor = accentColor,
                    alignLeft = false,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            translationX = animatedTranslation // 오른쪽에서 등장
                        }
                )
            }
        } else {
            // Image Right Layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextContent(
                    title = title,
                    description = description,
                    accentColor = accentColor,
                    alignLeft = true,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            translationX = -animatedTranslation
                        }
                )
                
                Spacer(modifier = Modifier.width(24.dp))
                
                ImageCard(
                    imageRes = imageRes,
                    contentDescription = imageContentDescription,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            translationX = animatedTranslation
                        }
                )
            }
        }
    }
}

@Composable
private fun ImageCard(
    imageRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // 정사각형 유지
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun TextContent(
    title: String,
    description: String,
    accentColor: Color,
    alignLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignLeft) Alignment.Start else Alignment.End
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            lineHeight = 30.sp,
            textAlign = if (alignLeft) TextAlign.Start else TextAlign.End
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            fontSize = 15.sp,
            color = AppColors.TextSecondary,
            lineHeight = 22.sp,
            textAlign = if (alignLeft) TextAlign.Start else TextAlign.End
        )
    }
}

@Composable
fun OutroSection(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "준비되셨나요?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "지금 바로 WiseYoung과 함께\n똑똑한 청년 생활을 시작해보세요.",
            fontSize = 16.sp,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.LightBlue
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "WiseYoung 시작하기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ScrollAnimationItem(
    listState: androidx.compose.foundation.lazy.LazyListState,
    content: @Composable (isVisible: Boolean) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val positionY = coordinates.positionInWindow().y
                val screenHeight = with(density) { coordinates.parentCoordinates?.size?.height ?: 1000 }
                isVisible = positionY < screenHeight * 0.85
            }
    ) {
        content(isVisible)
    }
}

fun Modifier.shadow(
    elevation: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape,
    spotColor: Color = Color.Black,
    ambientColor: Color = Color.Black
) = this.graphicsLayer {
    this.shadowElevation = elevation.toPx()
    this.shape = shape
    this.spotShadowColor = spotColor
    this.ambientShadowColor = ambientColor
}
