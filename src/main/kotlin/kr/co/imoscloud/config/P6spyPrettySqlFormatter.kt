package kr.co.imoscloud.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import org.hibernate.engine.jdbc.internal.FormatStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P6spy SQL 출력을 가독성 있게 포맷팅하는 클래스
 *
 * SQL 쿼리를 보기 좋게 포맷팅하여 로그에 출력합니다.
 */
class P6spyPrettySqlFormatter : MessageFormattingStrategy {

    override fun formatMessage(
        connectionId: Int,
        now: String,
        elapsed: Long,
        category: String,
        prepared: String,
        sql: String?,
        url: String
    ): String {
        // SQL이 없는 경우 빈 문자열 반환
        if (sql.isNullOrBlank()) {
            return ""
        }

        // SQL 포맷팅
        val formattedSql = formatSql(sql)

        // 현재 시간 포맷팅
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())

        // 로그 메시지 구성
        return StringBuilder()
            .append("\n\n")
            .append("/* 실행시간: ").append(currentTime).append(" | ")
            .append("소요시간: ").append(elapsed).append("ms | ")
            .append("연결ID: ").append(connectionId).append(" */\n")
            .append(formattedSql)
            .append("\n\n")
            .toString()
    }

    /**
     * SQL 쿼리를 포맷팅
     */
    private fun formatSql(sql: String): String {
        // 쿼리 타입 확인
        val trimmedSQL = sql.trim().lowercase(Locale.ROOT)

        // DML 쿼리인 경우 포맷팅 적용
        return if (isDmlStatement(trimmedSQL)) {
            try {
                // Hibernate 포맷터 사용
                FormatStyle.BASIC.formatter.format(sql)
            } catch (e: Exception) {
                // 포맷팅 실패 시 원본 반환
                sql
            }
        } else {
            sql
        }
    }

    /**
     * DML(Data Manipulation Language) 쿼리인지 확인
     */
    private fun isDmlStatement(sql: String): Boolean {
        return sql.startsWith("select") ||
                sql.startsWith("insert") ||
                sql.startsWith("update") ||
                sql.startsWith("delete")
    }
}