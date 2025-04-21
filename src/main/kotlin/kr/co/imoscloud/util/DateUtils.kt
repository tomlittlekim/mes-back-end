package kr.co.imoscloud.util

import kr.co.imoscloud.constants.CoreEnum
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {
    private val logger = LoggerFactory.getLogger(DateUtils::class.java)
    private val formatter = DateTimeFormatter.ISO_DATE
    private val isoFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    private val yyyyMMddssHHmm = DateTimeFormatter.ofPattern(CoreEnum.DateTimeFormat.DATE_TIME_VIEW.value)
    private val yyyyMMdd = DateTimeFormatter.ofPattern(CoreEnum.DateTimeFormat.DATE_VIEW.value)

    /**
     * ISO 8601 형식의 날짜 문자열을 LocalDate로 변환하는 함수
     * :: 현재 프론트에서 반환되는 날짜의 형식이 ISO 8601 형식임
     * @param dateStr "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" 형식의 날짜 문자열
     * @return 변환된 LocalDate 객체, 변환 실패 시 null
     */
    fun parseISODate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrEmpty()) {
            return null
        }

        return try {
            ZonedDateTime.parse(dateStr, isoFormatter).toLocalDate()
        } catch (e: DateTimeParseException) {
            logger.error("ISO 날짜 변환 실패: {}", dateStr)
            null
        }
    }

    /**
     * LocalDateTime을 표시용 문자열로 변환하는 함수
     * @param dateTime LocalDateTime 객체
     * @return "yyyy-MM-dd HH:mm:ss" 형식의 문자열, null이면 null 반환
     */
    fun formatLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(yyyyMMddssHHmm)
    }

    /**
     * LocalDateTime을 화면 표시용 문자열로 변환하는 함수
     * @param dateTime LocalDateTime 객체
     * @return "yyyy-MM-dd" 형식의 문자열, null이면 null 반환
     */
    fun formatLocalDate(dateTime: LocalDateTime?): String? {
        return dateTime?.format(yyyyMMdd)
    }

    /**
     * 문자열을 LocalDate로 변환하는 함수
     * @param dateStr "yyyy-MM-dd" 형식의 날짜 문자열
     * @return 변환된 LocalDate 객체, 변환 실패 시 null
     */
    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrEmpty()) {
            return null
        }

        return try {
            LocalDate.parse(dateStr, formatter)
        } catch (e: DateTimeParseException) {
            logger.error("날짜 변환 실패: {}", dateStr)
            null
        }
    }

    /**
     * 문자열을 LocalDateTime으로 변환하는 함수
     * @param dateStr "yyyy-MM-dd" 형식의 날짜 문자열
     * @return 변환된 LocalDateTime 객체 (시간은 00:00:00으로 설정), 변환 실패 시 null
     */
    fun parseDateTime(dateStr: String?): LocalDateTime? {
        if (dateStr.isNullOrEmpty()) {
            return null
        }

        return try {
            LocalDate.parse(dateStr, formatter).atStartOfDay()
        } catch (e: DateTimeParseException) {
            logger.error("날짜 변환 실패: {}", dateStr)
            null
        }
    }

    fun getSearchDateRange(formDateStr: String?, toDateStr: String?): Pair<LocalDateTime, LocalDateTime> {
        val fromDateTime: LocalDateTime? = formDateStr
            ?.let { dateStr -> parseDate(dateStr) }
            ?.let { LocalDateTime.of(it, LocalTime.MIN) }

        val toDateTime: LocalDateTime? = toDateStr
            ?.let { dateStr -> parseDate(dateStr) }
            ?.let { LocalDateTime.of(it, LocalTime.MAX) }

        val today = LocalDateTime.now()
        return when {
            fromDateTime != null && toDateTime == null -> Pair(fromDateTime, LocalDateTime.now().plusHours(1))
            fromDateTime == null && toDateTime != null -> Pair(LocalDateTime.of(2024,1,1,0,0,0), toDateTime)
            fromDateTime != null && toDateTime != null -> Pair(fromDateTime, toDateTime)
            else -> Pair(LocalDateTime.of(today.year, today.monthValue, 1,0,0,0), today.plusMinutes(1))
        }
    }
}