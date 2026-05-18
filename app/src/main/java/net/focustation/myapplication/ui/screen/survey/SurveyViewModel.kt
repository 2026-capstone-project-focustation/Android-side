package net.focustation.myapplication.ui.screen.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.focustation.myapplication.data.repository.FirestoreSurveyRepository
import net.focustation.myapplication.survey.SurveyResponse
import net.focustation.myapplication.survey.SurveyResponseStore

data class SurveyOption(
    val value: String,
    val label: String,
)

data class LikertQuestion(
    val key: String,
    val title: String,
    val lowLabel: String = "낮음",
    val highLabel: String = "높음",
)

data class SurveyUiState(
    val userType: String = "balanced",
    val generalPlaceType: String = "library",
    val generalTaskType: String = "deep_study",
    val generalSocialMode: String = "mostly_solo",
    val generalStayDuration: String = "1_2h",
    val generalTimeSlot: String = "afternoon",
    val generalDistractions: Set<String> = setOf("noise"),
    val generalPriorities: Set<String> = setOf("quiet"),
    val preferences: Map<String, Int> = defaultPreferenceScores,
    val placeType: String = "library",
    val placeName: String = "",
    val taskType: String = "deep_study",
    val groupSize: String = "solo",
    val stayDuration: String = "1_2h",
    val timeSlot: String = "afternoon",
    val dayType: String = "weekday",
    val distanceMinutes: Float = 20f,
    val weather: String = "clear",
    val indoorOutdoor: String = "indoor",
    val visitFrequency: String = "sometimes",
    val placeRatings: Map<String, Int> = defaultPlaceRatingScores,
    val labels: Map<String, Int> = defaultLabelScores,
    val satisfactionScore: Int = 50,
    val isSaving: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
)

class SurveyViewModel(
    private val repository: FirestoreSurveyRepository = FirestoreSurveyRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurveyUiState())
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    fun setUserType(value: String) {
        _uiState.update { it.copy(userType = value) }
    }

    fun setGeneralPlaceType(value: String) {
        _uiState.update { it.copy(generalPlaceType = value) }
    }

    fun setGeneralTaskType(value: String) {
        _uiState.update { it.copy(generalTaskType = value) }
    }

    fun setGeneralSocialMode(value: String) {
        _uiState.update { it.copy(generalSocialMode = value) }
    }

    fun setGeneralStayDuration(value: String) {
        _uiState.update { it.copy(generalStayDuration = value) }
    }

    fun setGeneralTimeSlot(value: String) {
        _uiState.update { it.copy(generalTimeSlot = value) }
    }

    fun toggleGeneralDistraction(value: String) {
        _uiState.update { state ->
            state.copy(generalDistractions = state.generalDistractions.toggled(value))
        }
    }

    fun toggleGeneralPriority(value: String) {
        _uiState.update { state ->
            state.copy(generalPriorities = state.generalPriorities.toggled(value))
        }
    }

    fun setPreference(
        key: String,
        value: Int,
    ) {
        _uiState.update { state ->
            state.copy(preferences = state.preferences.updatedScore(key, value))
        }
    }

    fun setPlaceType(value: String) {
        _uiState.update { it.copy(placeType = value) }
    }

    fun setPlaceName(value: String) {
        _uiState.update { it.copy(placeName = value) }
    }

    fun setTaskType(value: String) {
        _uiState.update { it.copy(taskType = value) }
    }

    fun setGroupSize(value: String) {
        _uiState.update { it.copy(groupSize = value) }
    }

    fun setStayDuration(value: String) {
        _uiState.update { it.copy(stayDuration = value) }
    }

    fun setTimeSlot(value: String) {
        _uiState.update { it.copy(timeSlot = value) }
    }

    fun setDayType(value: String) {
        _uiState.update { it.copy(dayType = value) }
    }

    fun setDistanceMinutes(value: Float) {
        _uiState.update { it.copy(distanceMinutes = value.coerceIn(1f, 40f)) }
    }

    fun setWeather(value: String) {
        _uiState.update { it.copy(weather = value) }
    }

    fun setIndoorOutdoor(value: String) {
        _uiState.update { it.copy(indoorOutdoor = value) }
    }

    fun setVisitFrequency(value: String) {
        _uiState.update { it.copy(visitFrequency = value) }
    }

    fun setPlaceRating(
        key: String,
        value: Int,
    ) {
        _uiState.update { state ->
            state.copy(placeRatings = state.placeRatings.updatedScore(key, value))
        }
    }

    fun setLabel(
        key: String,
        value: Int,
    ) {
        _uiState.update { state ->
            state.copy(labels = state.labels.updatedScore(key, value))
        }
    }

    fun setSatisfactionScore(value: Float) {
        _uiState.update { it.copy(satisfactionScore = value.toInt().coerceIn(1, 100)) }
    }

    fun submit() {
        if (_uiState.value.isSaving) return

        val state = _uiState.value
        val response = state.toSurveyResponse()
        SurveyResponseStore.save(response)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = repository.saveSurveyResponse(response)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSubmitted = true,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "설문 저장에 실패했어요.",
                        )
                    }
                },
            )
        }
    }
}

val userTypeOptions =
    listOf(
        SurveyOption("balanced", "균형형"),
        SurveyOption("quiet_deepwork", "조용한 몰입형"),
        SurveyOption("comfort_oriented", "편안함 중시형"),
        SurveyOption("convenience_oriented", "편의성 중시형"),
        SurveyOption("social_collaborative", "협업 선호형"),
    )

val placeTypeOptions =
    listOf(
        SurveyOption("library", "도서관"),
        SurveyOption("cafe", "카페"),
        SurveyOption("study_room", "스터디룸"),
        SurveyOption("classroom", "강의실"),
        SurveyOption("computer_lab", "컴퓨터실"),
        SurveyOption("coworking_space", "코워킹 공간"),
        SurveyOption("dorm_lounge", "기숙사 라운지"),
        SurveyOption("home", "집"),
        SurveyOption("outdoor", "야외"),
    )

val taskTypeOptions =
    listOf(
        SurveyOption("deep_study", "깊은 공부"),
        SurveyOption("coding", "코딩"),
        SurveyOption("report_writing", "보고서/글쓰기"),
        SurveyOption("light_reading", "가벼운 독서"),
        SurveyOption("online_class", "온라인 수업"),
        SurveyOption("team_project", "팀 프로젝트"),
        SurveyOption("meeting", "회의"),
    )

val socialModeOptions =
    listOf(
        SurveyOption("mostly_solo", "대부분 혼자"),
        SurveyOption("mixed", "혼자/함께 반반"),
        SurveyOption("mostly_group", "대부분 함께"),
    )

val durationOptions =
    listOf(
        SurveyOption("under_1h", "1시간 미만"),
        SurveyOption("1_2h", "1~2시간"),
        SurveyOption("2_4h", "2~4시간"),
        SurveyOption("over_4h", "4시간 이상"),
    )

val timeSlotOptions =
    listOf(
        SurveyOption("early_morning", "이른 아침"),
        SurveyOption("morning", "오전"),
        SurveyOption("afternoon", "오후"),
        SurveyOption("evening", "저녁"),
        SurveyOption("late_night", "늦은 밤"),
    )

val groupSizeOptions =
    listOf(
        SurveyOption("solo", "혼자"),
        SurveyOption("2", "2명"),
        SurveyOption("3_4", "3~4명"),
        SurveyOption("5_plus", "5명 이상"),
    )

val dayTypeOptions =
    listOf(
        SurveyOption("weekday", "평일"),
        SurveyOption("weekend", "주말"),
    )

val weatherOptions =
    listOf(
        SurveyOption("clear", "맑음"),
        SurveyOption("cloudy", "흐림"),
        SurveyOption("rainy", "비"),
        SurveyOption("hot", "더움"),
        SurveyOption("cold", "추움"),
    )

val indoorOutdoorOptions =
    listOf(
        SurveyOption("indoor", "실내"),
        SurveyOption("outdoor", "실외"),
    )

val visitFrequencyOptions =
    listOf(
        SurveyOption("first_time", "처음"),
        SurveyOption("rarely", "가끔"),
        SurveyOption("sometimes", "종종"),
        SurveyOption("often", "자주"),
    )

val distractionOptions =
    listOf(
        SurveyOption("noise", "소음"),
        SurveyOption("crowd", "혼잡"),
        SurveyOption("visual", "시각적 방해"),
        SurveyOption("temperature", "온도/공기"),
        SurveyOption("outlet", "콘센트"),
        SurveyOption("distance", "이동 거리"),
    )

val priorityOptions =
    listOf(
        SurveyOption("quiet", "조용함"),
        SurveyOption("outlet", "콘센트"),
        SurveyOption("distance", "가까운 거리"),
        SurveyOption("comfort", "편안함"),
        SurveyOption("privacy", "프라이버시"),
    )

val preferenceQuestions =
    listOf(
        LikertQuestion("pref_quiet", "조용한 장소가 중요해요"),
        LikertQuestion("pref_light", "밝기가 충분한 장소가 좋아요"),
        LikertQuestion("pref_low_crowd", "사람이 많으면 집중하기 어려워요"),
        LikertQuestion("pref_privacy", "시선이 덜 노출되는 자리를 선호해요"),
        LikertQuestion("pref_outlet", "콘센트 사용 가능 여부가 중요해요"),
        LikertQuestion("pref_distance", "이동 거리/시간에 민감해요"),
        LikertQuestion("pref_thermal_air", "덥거나 춥고 답답한 환경에 민감해요"),
        LikertQuestion("pref_control", "자리/조명/환기 조절이 가능한 곳이 좋아요"),
        LikertQuestion("pref_comfort", "의자와 책상 편안함이 중요해요"),
        LikertQuestion("pref_deepwork", "혼자 깊게 집중할 수 있는 장소가 좋아요"),
    )

val placeRatingQuestions =
    listOf(
        LikertQuestion("place_quiet", "소음이 방해되지 않았어요"),
        LikertQuestion("place_light", "밝기가 충분했어요"),
        LikertQuestion("place_low_crowd", "사람이 많지 않았어요"),
        LikertQuestion("place_low_visual_distraction", "시선/동선 방해가 적었어요"),
        LikertQuestion("place_control", "자리/조명/환기 조절이 가능했어요"),
        LikertQuestion("place_comfort", "의자와 책상이 편안했어요"),
        LikertQuestion("place_outlet", "콘센트 사용이 편리했어요"),
        LikertQuestion("place_task_fit", "작업에 맞는 분위기였어요"),
        LikertQuestion("place_temperature_air", "온도와 공기가 적절했어요"),
        LikertQuestion("place_seat_availability", "앉을 자리를 찾기 쉬웠어요"),
    )

val labelQuestions =
    listOf(
        LikertQuestion("focus_score", "실제로 집중이 잘 되었어요"),
        LikertQuestion("productivity_score", "작업 효율이 높았어요"),
        LikertQuestion("revisit_score", "다시 선택하고 싶어요"),
        LikertQuestion("recommend_score", "다른 사람에게 추천하고 싶어요"),
    )

private val defaultPreferenceScores = preferenceQuestions.associate { it.key to 3 }
private val defaultPlaceRatingScores = placeRatingQuestions.associate { it.key to 3 }
private val defaultLabelScores = labelQuestions.associate { it.key to 3 }

private fun Set<String>.toggled(value: String): Set<String> =
    if (contains(value)) {
        this - value
    } else {
        this + value
    }

private fun Map<String, Int>.updatedScore(
    key: String,
    value: Int,
): Map<String, Int> = this + (key to value.coerceIn(1, 5))

private fun SurveyUiState.toSurveyResponse(): SurveyResponse =
    SurveyResponse(
        modelInput = toModelInputPayload(),
        labels = toLabelPayload(),
    )

private fun SurveyUiState.toModelInputPayload(): Map<String, Any> {
    val payload = linkedMapOf<String, Any>()

    payload["user_type"] = userType
    preferences.forEach { (key, value) -> payload[key] = value }

    payload["general_place_type"] = generalPlaceType
    payload["general_task_type"] = generalTaskType
    payload["general_social_mode"] = generalSocialMode
    payload["general_stay_duration"] = generalStayDuration
    payload["general_time_slot"] = generalTimeSlot
    distractionOptions.forEach { option ->
        payload["general_distraction_${option.value}"] = if (generalDistractions.contains(option.value)) 1 else 0
    }
    priorityOptions.forEach { option ->
        payload["general_priority_${option.value}"] = if (generalPriorities.contains(option.value)) 1 else 0
    }

    payload["place_type"] = placeType
    payload["place_name"] = placeName.ifBlank { "장소 미지정" }
    payload["task_type"] = taskType
    payload["group_size"] = groupSize
    payload["stay_duration"] = stayDuration
    payload["time_slot"] = timeSlot
    payload["day_type"] = dayType
    payload["distance_minutes"] = "%.1f".format(distanceMinutes).toFloat()
    payload["weather"] = weather
    payload["indoor_outdoor"] = indoorOutdoor
    payload["visit_frequency"] = visitFrequency
    placeRatings.forEach { (key, value) -> payload[key] = value }
    payload.putAll(buildDerivedFeatures())

    return payload
}

private fun SurveyUiState.toLabelPayload(): Map<String, Any> =
    linkedMapOf<String, Any>().apply {
        labels.forEach { (key, value) -> put(key, value) }
        put("satisfaction_score", satisfactionScore)
    }

private fun SurveyUiState.buildDerivedFeatures(): Map<String, Any> {
    val pref = preferences.withDefault { 3 }
    val place = placeRatings.withDefault { 3 }

    return linkedMapOf(
        "quiet_match" to pref.getValue("pref_quiet") * place.getValue("place_quiet"),
        "light_match" to pref.getValue("pref_light") * place.getValue("place_light"),
        "crowd_match" to pref.getValue("pref_low_crowd") * place.getValue("place_low_crowd"),
        "privacy_match" to
            pref.getValue("pref_privacy") * place.getValue("place_low_visual_distraction"),
        "outlet_match" to pref.getValue("pref_outlet") * place.getValue("place_outlet"),
        "thermal_air_match" to
            pref.getValue("pref_thermal_air") * place.getValue("place_temperature_air"),
        "control_match" to pref.getValue("pref_control") * place.getValue("place_control"),
        "comfort_match" to pref.getValue("pref_comfort") * place.getValue("place_comfort"),
        "deepwork_task_match" to pref.getValue("pref_deepwork") * taskFocusWeight(taskType),
        "distance_penalty" to "%.1f".format(pref.getValue("pref_distance") * distanceMinutes).toFloat(),
        "task_place_fit_match" to place.getValue("place_task_fit"),
        "time_match" to if (generalTimeSlot == timeSlot) 1 else 0,
    )
}

private fun taskFocusWeight(taskType: String): Int =
    when (taskType) {
        "deep_study", "coding", "report_writing" -> 5
        "light_reading", "online_class" -> 3
        else -> 1
    }
