package net.focustation.myapplication.util

/**
 * 좌표가 대한민국 영역(여유 포함) 안에 있는지 검사한다.
 * 위도 33.0~39.0 / 경도 124.0~132.0 (본토 + 제주·도서). null이거나 검색·좌표 변환 오류로 생긴
 * 비정상 좌표(예: 89.99, 113.01)면 false. 저장·표시 단계의 방어용 검증이다.
 */
fun isPlausibleKoreanCoordinate(
    latitude: Double?,
    longitude: Double?,
): Boolean = latitude != null && longitude != null && latitude in 33.0..39.0 && longitude in 124.0..132.0
