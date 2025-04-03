package kr.co.imoscloud.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import org.hibernate.engine.jdbc.internal.FormatStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P6spy SQL 출력을 가독성 있게 포맷팅하는 클래스
 * ANSI 색상 코드를 사용하여 로그에 색상을 추가합니다.
 */
class P6spyPrettySqlFormatter : MessageFormattingStrategy {

    // ANSI 색상 코드
    companion object {
        private const val ANSI_RESET = "\u001B[0m"
        private const val ANSI_CYAN = "\u001B[36m"
        private const val ANSI_GREEN = "\u001B[32m"
        private const val ANSI_YELLOW = "\u001B[33m"
        private const val ANSI_RED = "\u001B[31m"
        private const val ANSI_BLUE = "\u001B[34m"
        private const val ANSI_PURPLE = "\u001B[35m"
        private const val ANSI_BOLD = "\u001B[1m"
    }

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

        // 실행 시간에 따른 색상 설정
        val timeColor = when {
            elapsed < 10 -> ANSI_GREEN       // 10ms 미만: 녹색
            elapsed < 100 -> ANSI_YELLOW     // 10-100ms: 노란색
            else -> ANSI_RED                 // 100ms 이상: 빨간색
        }

        // 로그 메시지 구성
        return StringBuilder()
            .append("\n\n")
            .append("$ANSI_BOLD$ANSI_CYAN/* ")
            .append("실행시간: ").append(currentTime).append(" | ")
            .append("소요시간: ").append(timeColor).append(elapsed).append("ms").append(ANSI_CYAN).append(" | ")
            .append("연결ID: ").append(connectionId).append(" */$ANSI_RESET\n")
            .append(colorSql(formattedSql))
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
     * SQL 쿼리에 색상 적용
     */
    private fun colorSql(sql: String): String {
        // SQL 키워드에 색상 적용
        return sql.replace(
            // SQL 주요 키워드 강조
            Regex("(?i)(SELECT|FROM|WHERE|JOIN|LEFT|RIGHT|INNER|OUTER|ON|GROUP BY|ORDER BY|HAVING|LIMIT|OFFSET|INSERT|UPDATE|DELETE|SET|VALUES)", RegexOption.IGNORE_CASE),
            "$ANSI_BOLD$ANSI_BLUE$1$ANSI_RESET"
        ).replace(
            // 연산자 강조
            Regex("(=|>|<|>=|<=|<>|!=|AND|OR|LIKE|IN|NOT|IS NULL|IS NOT NULL)", RegexOption.IGNORE_CASE),
            "$ANSI_YELLOW$1$ANSI_RESET"
        ).replace(
            // 함수 강조
            Regex("(?i)(COUNT|SUM|AVG|MIN|MAX|COALESCE|CONCAT|SUBSTRING|DATE_FORMAT)\\(", RegexOption.IGNORE_CASE),
            "$ANSI_GREEN$1$ANSI_RESET("
        ).replace(
            // 문자열 강조
            Regex("'([^']*)'"),
            "$ANSI_RED'$1'$ANSI_RESET"
        ).replace(
            // 숫자 강조
            Regex("\\b(\\d+)\\b"),
            "$ANSI_PURPLE$1$ANSI_RESET"
        )
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