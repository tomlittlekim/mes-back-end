package kr.co.imoscloud.util

import kr.co.imoscloud.constants.CoreEnum
import org.slf4j.LoggerFactory
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {
    private val logger = LoggerFactory.getLogger(DateUtils::class.java)
    private val formatter = DateTimeFormatter.ISO_DATE
    private val isoFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    private val yyyyMMddssHHmm = DateTimeFormatter.ofPattern(CoreEnum.DateTimeFormat.DATE_TIME_VIEW.value)
    private val yyyyMMdd = DateTimeFormatter.ofPattern(CoreEnum.DateTimeFormat.DATE_VIEW.value)
    private val isoDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    private val dateTimeViewShort = DateTimeFormatter.ofPattern(CoreEnum.DateTimeFormat.DATE_TIME_VIEW_SHORT.value)

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
     * LocalDateTime을 시간 정보(시, 분)가 포함된 표시용 문자열로 변환하는 함수
     * @param dateTime LocalDateTime 객체
     * @return "yyyy-MM-dd HH:mm" 형식의 문자열, null이면 null 반환
     */
    fun formatLocalDateTimeShort(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeViewShort)
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
     * 시간 정보(시, 분)를 포함한 날짜/시간 문자열을 LocalDateTime으로 변환하는 함수
     * @param dateTimeStr 날짜와 시간 문자열 (다양한 형식 지원)
     *   - "yyyy-MM-dd HH:mm" 형식
     *   - "yyyy-MM-ddTHH:mm" 형식
     *   - "yyyy-MM-dd" 형식 (시간은 00:00으로 설정)
     * @return 변환된 LocalDateTime 객체, 변환 실패 시 null
     */
    fun parseDateTimeWithHourMinute(dateTimeStr: String?): LocalDateTime? {
        if (dateTimeStr.isNullOrEmpty()) {
            return null
        }

        return try {
            when {
                dateTimeStr.contains("T") -> {
                    // ISO 형태 (2024-01-01T14:30)
                    if (dateTimeStr.length == 16) {
                        // 초 없이 분까지만 (2024-01-01T14:30)
                        LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                    } else {
                        // 기본 ISO 파싱 시도
                        LocalDateTime.parse(dateTimeStr)
                    }
                }
                dateTimeStr.contains(" ") -> {
                    // 공백으로 구분된 형태 (2024-01-01 14:30)
                    LocalDateTime.parse(dateTimeStr, dateTimeViewShort)
                }
                else -> {
                    // 날짜만 있는 경우 (2024-01-01) - 00:00:00으로 설정
                    val date = LocalDate.parse(dateTimeStr)
                    LocalDateTime.of(date, LocalTime.MIN)
                }
            }
        } catch (e: DateTimeParseException) {
            logger.error("날짜/시간 변환 실패: {}", dateTimeStr)
            null
        }
    }

    /**
     * 문자열을 LocalDateTime으로 변환하는 함수 (날짜와 시간 모두 포함)
     * @param dateTimeStr 날짜와 시간 문자열 (ISO 형식 "yyyy-MM-ddTHH:mm:ss" 또는 "yyyy-MM-dd")
     * @return 변환된 LocalDateTime 객체
     *   - ISO 형식 날짜+시간: 해당 시간으로 설정
     *   - 날짜만 있는 경우: 시작 시간은 00:00:00, 종료 시간은 23:59:59로 설정
     */
    fun parseDateTimeExact(dateTimeStr: String?): LocalDateTime? {
        if (dateTimeStr.isNullOrEmpty()) {
            return null
        }

        return try {
            // ISO DateTime 형식 (yyyy-MM-ddTHH:mm:ss) 파싱 시도
            LocalDateTime.parse(dateTimeStr, isoDateTimeFormatter)
        } catch (e: DateTimeParseException) {
            try {
                // 날짜만 있는 경우 (yyyy-MM-dd) 00:00:00 시간으로 파싱
                LocalDate.parse(dateTimeStr, formatter).atStartOfDay()
            } catch (e2: DateTimeParseException) {
                logger.error("날짜/시간 변환 실패: {}", dateTimeStr)
                null
            }
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

    /**
     * 문자열을 LocalDateTime으로 변환하는 헬퍼 함수
     */
    fun parseDateTimeFromString(dateString: String?): LocalDateTime? {
        if (dateString.isNullOrBlank()) return null

        return try {
            // ISO 날짜 형식 (예: 2025-04-18T01:52:00)을 파싱
            LocalDateTime.parse(dateString)
        } catch (e: Exception) {
            // 파싱 실패 시 로깅 및 null 반환
            println("날짜 파싱 실패: $dateString - ${e.message}")
            null
        }
    }

    fun getSearchDateTimeRange(formDateStr: String?, toDateStr: String?): Pair<LocalDateTime, LocalDateTime> {
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

    fun getSearchDateRange(formDateStr: String?, toDateStr: String?): Pair<LocalDate, LocalDate> {
        val fromDate: LocalDate? = formDateStr
            ?.let { dateStr -> parseDate(dateStr) }

        val toDate: LocalDate? = toDateStr
            ?.let { dateStr -> parseDate(dateStr) }

        val today = LocalDate.now()
        return when {
            fromDate != null && toDate == null -> Pair(fromDate, LocalDate.now().plusDays(1))
            fromDate == null && toDate != null -> Pair(LocalDate.of(2024,1,1), toDate)
            fromDate != null && toDate != null -> Pair(fromDate, toDate)
            else -> Pair(today.withDayOfMonth(1), today.plusDays(1))
        }
    }

    fun <D> formattedDate(date: D, format: CoreEnum.DateTimeFormat): String {
        val formatter = DateTimeFormatter.ofPattern(format.value)

        return when (date) {
            is LocalDate -> formatter.format(date.atStartOfDay())
            is LocalDateTime -> formatter.format(date)
            is YearMonth -> formatter.format(LocalDate.of(date.year, date.monthValue, 1).atStartOfDay())
            else -> ""
        }
    }
}