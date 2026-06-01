package net.focustation.myapplication.ui.screen.session

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class SessionFeedbackDerivationsTest {
    @Test
    fun `timeSlot 경계값이 옵션 value와 일치한다`() {
        assertEquals("early_morning", timeSlotForHour(0))
        assertEquals("early_morning", timeSlotForHour(5))
        assertEquals("morning", timeSlotForHour(6))
        assertEquals("morning", timeSlotForHour(11))
        assertEquals("afternoon", timeSlotForHour(12))
        assertEquals("afternoon", timeSlotForHour(17))
        assertEquals("evening", timeSlotForHour(18))
        assertEquals("evening", timeSlotForHour(21))
        assertEquals("late_night", timeSlotForHour(22))
        assertEquals("late_night", timeSlotForHour(23))
    }

    @Test
    fun `주말과 평일을 구분한다`() {
        assertEquals("weekend", dayTypeForDayOfWeek(DayOfWeek.SATURDAY))
        assertEquals("weekend", dayTypeForDayOfWeek(DayOfWeek.SUNDAY))
        assertEquals("weekday", dayTypeForDayOfWeek(DayOfWeek.MONDAY))
        assertEquals("weekday", dayTypeForDayOfWeek(DayOfWeek.FRIDAY))
    }

    @Test
    fun `체류 시간 버킷 경계가 옵션 value와 일치한다`() {
        assertEquals("under_1h", stayDurationForMinutes(0))
        assertEquals("under_1h", stayDurationForMinutes(59))
        assertEquals("1_2h", stayDurationForMinutes(60))
        assertEquals("1_2h", stayDurationForMinutes(119))
        assertEquals("2_4h", stayDurationForMinutes(120))
        assertEquals("2_4h", stayDurationForMinutes(239))
        assertEquals("over_4h", stayDurationForMinutes(240))
        assertEquals("over_4h", stayDurationForMinutes(600))
    }

    @Test
    fun `네이버 카테고리를 place_type으로 매핑한다`() {
        assertEquals("cafe", mapNaverCategoryToPlaceType("카페,디저트"))
        assertEquals("cafe", mapNaverCategoryToPlaceType("커피전문점"))
        assertEquals("library", mapNaverCategoryToPlaceType("도서관"))
        assertEquals("study_room", mapNaverCategoryToPlaceType("독서실,스터디카페"))
        assertEquals("study_room", mapNaverCategoryToPlaceType("스터디룸"))
        assertEquals("coworking_space", mapNaverCategoryToPlaceType("코워킹스페이스"))
        assertEquals("classroom", mapNaverCategoryToPlaceType("학원>입시"))
        assertEquals("cafe", mapNaverCategoryToPlaceType("")) // 빈 카테고리 기본값
        assertEquals("cafe", mapNaverCategoryToPlaceType("음식점")) // 미분류 기본값
    }

    @Test
    fun `place_type에서 실내외를 유도한다`() {
        assertEquals("outdoor", indoorOutdoorForPlaceType("outdoor"))
        assertEquals("indoor", indoorOutdoorForPlaceType("cafe"))
        assertEquals("indoor", indoorOutdoorForPlaceType("library"))
    }

    @Test
    fun `범위 밖 입력을 안전한 기본값으로 처리한다`() {
        assertEquals("late_night", timeSlotForHour(-1))
        assertEquals("late_night", timeSlotForHour(24))
        assertEquals("under_1h", stayDurationForMinutes(-10))
    }
}
