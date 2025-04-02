package kr.co.imoscloud.util

import kr.co.imoscloud.constants.CoreEnum
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
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
     * 문자열을 LocalDate로 변환하는 함수, 디폴트 날짜 설정 가능
     * @param dateStr "yyyy-MM-dd" 형식의 날짜 문자열
     * @param defaultDate 변환 실패 시 반환할 기본값
     * @return 변환된 LocalDate 객체, 변환 실패 시 defaultDate
     */
    fun parseDateWithDefault(dateStr: String?, defaultDate: LocalDate?): LocalDate? {
        if (dateStr.isNullOrEmpty()) {
            return defaultDate
        }

        return try {
            LocalDate.parse(dateStr, formatter)
        } catch (e: DateTimeParseException) {
            logger.error("날짜 변환 실패: {}", dateStr)
            defaultDate
        }
    }

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
}