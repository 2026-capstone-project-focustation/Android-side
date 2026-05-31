package net.focustation.myapplication.ui.screen.session

import java.time.DayOfWeek

// placeFeedback의 일부 필드는 사용자에게 묻지 않고 세션 시각·길이·장소 메타에서 자동 도출한다.
// 도출 규칙은 옵션 value 문자열과 정확히 일치해야 한다(timeSlotOptions/durationOptions/dayTypeOptions).
// 순수 함수로 분리해 단위 테스트한다.

internal fun timeSlotForHour(hour: Int): String =
    when (hour) {
        in 0..5 -> "early_morning"
        in 6..11 -> "morning"
        in 12..17 -> "afternoon"
        in 18..21 -> "evening"
        else -> "late_night"
    }

internal fun dayTypeForDayOfWeek(dayOfWeek: DayOfWeek): String =
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) "weekend" else "weekday"

internal fun stayDurationForMinutes(minutes: Int): String =
    when {
        minutes < 60 -> "under_1h"
        minutes < 120 -> "1_2h"
        minutes < 240 -> "2_4h"
        else -> "over_4h"
    }

// 네이버 검색 카테고리 문자열에서 place_type 후보를 추정한다(사용자가 화면에서 수정 가능).
internal fun mapNaverCategoryToPlaceType(category: String): String =
    when {
        category.isBlank() -> "cafe"
        category.contains("도서관") -> "library"
        category.contains("독서실") || category.contains("스터디") -> "study_room"
        category.contains("카페") || category.contains("커피") -> "cafe"
        category.contains("코워킹") || category.contains("공유오피스") -> "coworking_space"
        category.contains("강의") ||
            category.contains("학원") ||
            category.contains("학교") ||
            category.contains("대학") -> "classroom"
        category.contains("피시") || category.contains("PC") || category.contains("컴퓨터") -> "computer_lab"
        else -> "cafe"
    }

// place_type에서 실내/실외를 유도한다(사용자가 화면에서 수정 가능).
internal fun indoorOutdoorForPlaceType(placeType: String): String = if (placeType == "outdoor") "outdoor" else "indoor"
