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

    @Test
    fun `센서 raw sample을 v2 summary로 변환한다`() {
        val summary =
            buildSensorSummaryPayload(
                noiseSamples = listOf(30.0, 32.0, 60.0, 62.0),
                lightSamples = listOf(400f, 500f, 600f),
                vibrationSamples = listOf(0.01, 0.09, 0.04, 0.10, 0.03),
                measurementDurationSec = 127,
            )

        assertEquals(46.0, summary.noiseMeanDb, 0.0001)
        assertEquals(62.0, summary.noiseMaxDb, 0.0001)
        assertEquals(61.4, summary.noiseP90Db, 0.0001)
        assertEquals(1, summary.noiseSpikeCount)
        assertEquals(500.0, summary.lightMeanLux, 0.0001)
        assertEquals(400.0, summary.lightMinLux, 0.0001)
        assertEquals(600.0, summary.lightMaxLux, 0.0001)
        assertEquals(0.098, summary.vibrationP95, 0.0001)
        assertEquals(2, summary.vibrationSpikeCount)
        assertEquals(127, summary.measurementDurationSec)
        assertEquals(1.0, summary.validSampleRatio ?: 0.0, 0.0001)
        assertEquals(0.4, summary.phoneMovementRatio ?: 0.0, 0.0001)
    }

    @Test
    fun `v2 modelInput은 설문 장소 피드백 센서 요약값을 flat payload로 합친다`() {
        val input =
            buildSensorTargetV2ModelInput(
                surveyModelInput =
                    mapOf(
                        "user_type" to "balanced",
                        "pref_quiet" to 4,
                    ),
                placeFeedback =
                    mapOf(
                        "place_type" to "home",
                        "place_quiet" to 5,
                    ),
                sensorSummary =
                    buildSensorSummaryPayload(
                        noiseSamples = listOf(30.0),
                        lightSamples = listOf(500f),
                        vibrationSamples = listOf(0.02),
                        measurementDurationSec = 60,
                    ),
            )

        assertEquals("balanced", input["user_type"])
        assertEquals(4, input["pref_quiet"])
        assertEquals("home", input["place_type"])
        assertEquals(5, input["place_quiet"])
        assertEquals(30.0, input["noise_mean_db"])
        assertEquals(500.0, input["light_mean_lux"])
        assertEquals(0.02, input["vibration_mean"])
        assertEquals(60, input["measurement_duration_sec"])
    }
}
