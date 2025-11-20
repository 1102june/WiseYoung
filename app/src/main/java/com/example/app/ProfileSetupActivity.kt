package com.wiseyoung.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.app.ui.theme.WiseYoungTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileSetupActivity : ComponentActivity() {

    private val client by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiseYoungTheme {
                var isSubmitting by remember { mutableStateOf(false) }
                var snackbarMessage by remember { mutableStateOf<String?>(null) }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        snackbarMessage = null
                    }
                }

                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                    ProfileSetupScreen(
                        modifier = Modifier.padding(paddingValues),
                        isSubmitting = isSubmitting,
                        onBack = { finish() },
                        onSubmit = { payload ->
                            if (isSubmitting) return@ProfileSetupScreen
                            isSubmitting = true
                            submitProfile(
                                payload = payload,
                                onResult = { success, message ->
                                    isSubmitting = false
                                    if (success) {
                                        ProfilePreferences.setProfileCompleted(this, true)
                                        Toast.makeText(
                                            this,
                                            "프로필 저장이 완료되었습니다.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    } else {
                                        snackbarMessage = message
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private fun submitProfile(
        payload: ProfilePayload,
        onResult: (Boolean, String) -> Unit
    ) {
        val jsonObject = JSONObject().apply {
            put("birthDate", payload.birthDate)
            put("gender", payload.gender)
            put("province", payload.province)
            put("city", payload.city)
            put("education", payload.education)
            put("employment", payload.employment)
            put("interests", JSONArray(payload.interests))
        }

        val requestBody = jsonObject.toString()
            .toRequestBody("application/json".toMediaType())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://172.16.1.42:8080/profile")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val message = if (response.isSuccessful) {
                        "서버에 프로필이 저장되었습니다."
                    } else {
                        "서버 오류: ${response.code}"
                    }
                    withContext(Dispatchers.Main) {
                        onResult(response.isSuccessful, message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "알 수 없는 오류")
                }
            }
        }
    }
}

data class ProfilePayload(
    val birthDate: String,
    val gender: String,
    val province: String,
    val city: String,
    val education: String,
    val employment: String,
    val interests: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (ProfilePayload) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var gender by remember { mutableStateOf("male") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var employment by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(setOf<String>()) }

    val cityMap = remember { provinceCities }

    val canSubmit = birthDate != null &&
            province.isNotBlank() &&
            city.isNotBlank() &&
            education.isNotBlank() &&
            employment.isNotBlank() &&
            interests.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            HeaderSection(onBack = onBack)

            Spacer(modifier = Modifier.height(12.dp))

            DateSection(
                birthDate = birthDate,
                onClick = {
                    val now = birthDate ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            birthDate = LocalDate.of(year, month + 1, dayOfMonth)
                        },
                        now.year,
                        now.monthValue - 1,
                        now.dayOfMonth
                    ).show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            GenderSection(
                selectedGender = gender,
                onSelect = { gender = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            DropdownSection(
                label = "거주 지역 (도)",
                value = province,
                options = cityMap.keys.toList(),
                placeholder = "도 선택",
                onValueChange = {
                    province = it
                    city = ""
                }
            )

            if (province.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                DropdownSection(
                    label = "거주 지역 (시)",
                    value = city,
                    options = cityMap[province].orEmpty(),
                    placeholder = "시 선택",
                    onValueChange = { city = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            DropdownSection(
                label = "학력/재학 상태",
                value = education,
                options = listOf("고등학교 졸업", "대학교 재학", "대학교 졸업", "대학원 이상"),
                placeholder = "선택해주세요",
                onValueChange = { education = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            DropdownSection(
                label = "직업/고용 상태",
                value = employment,
                options = listOf("학생", "직장인", "구직자", "자영업자"),
                placeholder = "선택해주세요",
                onValueChange = { employment = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            InterestSection(
                selected = interests,
                onToggle = { interest ->
                    interests = if (interests.contains(interest)) {
                        interests - interest
                    } else {
                        interests + interest
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    birthDate?.let { date ->
                        onSubmit(
                            ProfilePayload(
                                birthDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                gender = gender,
                                province = province,
                                city = city,
                                education = education,
                                employment = employment,
                                interests = interests.toList()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = if (isSubmitting) "저장 중..." else "시작하기",
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "최초 Google 로그인 시 한번만 입력해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun HeaderSection(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "프로필 입력",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DateSection(birthDate: LocalDate?, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "생년월일",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = birthDate?.formatKoreanDate() ?: "날짜 선택",
                modifier = Modifier.padding(vertical = 4.dp),
                color = if (birthDate == null) Color.Gray else Color.Black
            )
        }
    }
}

@Composable
private fun GenderSection(selectedGender: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "성별",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GenderButton(
                text = "남성",
                isSelected = selectedGender == "male",
                onClick = { onSelect("male") }
            )
            GenderButton(
                text = "여성",
                isSelected = selectedGender == "female",
                onClick = { onSelect("female") }
            )
        }
    }
}

@Composable
private fun GenderButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF8B5CF6) else Color.White,
            contentColor = if (isSelected) Color.White else Color(0xFF8B5CF6)
        )
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSection(
    label: String,
    value: String,
    options: List<String>,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = if (value.isBlank()) placeholder else value,
                    color = if (value.isBlank()) Color.Gray else Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InterestSection(selected: Set<String>, onToggle: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "관심분야", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        val interests = listOf("취업", "창업", "주거", "복지")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            interests.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { interest ->
                        val isSelected = selected.contains(interest)
                        Button(
                            onClick = { onToggle(interest) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF8B5CF6) else Color.White,
                                contentColor = if (isSelected) Color.White else Color(0xFF8B5CF6)
                            )
                        ) {
                            Text(interest)
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private val provinceCities = mapOf(
    "서울" to listOf("서울특별시"),
    "부산" to listOf("부산광역시"),
    "경기" to listOf(
        "고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시", "남양주시", "동두천시", "부천시",
        "성남시", "수원시", "시흥시", "안산시", "안성시", "안양시", "양주시", "여주시", "오산시", "용인시",
        "의왕시", "의정부시", "이천시", "파주시", "평택시", "포천시", "하남시", "화성시"
    ),
    "인천" to listOf("인천광역시"),
    "대구" to listOf("대구광역시"),
    "광주" to listOf("광주광역시"),
    "대전" to listOf("대전광역시"),
    "울산" to listOf("울산광역시"),
    "강원" to listOf("강릉시", "동해시", "삼척시", "속초시", "원주시", "춘천시", "태백시"),
    "충북" to listOf("제천시", "청주시", "충주시"),
    "충남" to listOf("계룡시", "공주시", "논산시", "당진시", "보령시", "서산시", "아산시", "천안시"),
    "전북" to listOf("군산시", "김제시", "남원시", "익산시", "전주시", "정읍시"),
    "전남" to listOf("광양시", "나주시", "목포시", "순천시", "여수시"),
    "경북" to listOf("경산시", "경주시", "구미시", "김천시", "문경시", "상주시", "안동시", "영주시", "영천시", "포항시"),
    "경남" to listOf("거제시", "김해시", "밀양시", "사천시", "양산시", "진주시", "창원시", "통영시"),
    "제주" to listOf("제주시", "서귀포시")
)

private fun LocalDate.formatKoreanDate(): String {
    return String.format(Locale.KOREA, "%d년 %02d월 %02d일", year, monthValue, dayOfMonth)
}