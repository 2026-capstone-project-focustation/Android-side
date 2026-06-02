package net.focustation.myapplication.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinatesTest {
    @Test
    fun `대한민국 좌표는 유효하다`() {
        assertTrue(isPlausibleKoreanCoordinate(37.4979, 127.0276)) // 강남역
        assertTrue(isPlausibleKoreanCoordinate(35.1796, 129.0756)) // 부산
        assertTrue(isPlausibleKoreanCoordinate(33.4996, 126.5312)) // 제주
    }

    @Test
    fun `한국 밖이거나 비정상 좌표는 거른다`() {
        assertFalse(isPlausibleKoreanCoordinate(89.99, 113.01)) // 좌표 변환 오류 garbage(북극 인근)
        assertFalse(isPlausibleKoreanCoordinate(37.7749, -122.4194)) // 샌프란시스코
        assertFalse(isPlausibleKoreanCoordinate(0.0, 0.0)) // null island
    }

    @Test
    fun `null 좌표는 거른다`() {
        assertFalse(isPlausibleKoreanCoordinate(null, 127.0))
        assertFalse(isPlausibleKoreanCoordinate(37.5, null))
        assertFalse(isPlausibleKoreanCoordinate(null, null))
    }
}
