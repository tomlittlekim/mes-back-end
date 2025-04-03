package kr.co.imoscloud

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test") // 테스트 프로파일 사용
class ProductionPlanRepositoryTest {

    @Autowired
    private lateinit var productionPlanRepository: ProductionPlanRepository

    @Test
    fun `날짜 범위 쿼리 테스트`() {
        // 테스트 데이터 준비
        val site = "TEST"
        val compCd = "TEST"

        // 날짜 범위 설정
        val planStartDateFrom = LocalDate.of(2025, 3, 31)
        val planStartDateTo = LocalDate.of(2025, 4, 1)

        // 쿼리 실행
        val result = productionPlanRepository.getProductionPlanList(
            site = site,
            compCd = compCd,
            prodPlanId = null,
            orderId = null,
            productId = null,
            planStartDateFrom = planStartDateFrom,
            planStartDateTo = planStartDateTo,
            flagActive = null
        )

        // 결과 검증
        println("조회된 레코드 수: ${result.size}")

        // 조회된 모든 레코드의 planStartDate가 지정된 범위 내에 있는지 확인
        result.forEach { plan ->
            val planStartDate = plan.planStartDate
            println("계획 시작일: $planStartDate, 계획 ID: ${plan.prodPlanId}")

            assertTrue(planStartDate != null, "계획 시작일이 null이 아니어야 합니다.")

            val startBoundary = LocalDateTime.of(planStartDateFrom, LocalTime.MIN)
            val endBoundary = LocalDateTime.of(planStartDateTo, LocalTime.MAX)

            assertTrue(
                !planStartDate!!.isBefore(startBoundary) && !planStartDate.isAfter(endBoundary),
                "계획 시작일($planStartDate)이 범위($startBoundary ~ $endBoundary) 내에 있어야 합니다."
            )

            // 2025-04-02 날짜가 포함되지 않는지 확인
            val outsideDate = LocalDateTime.of(2025, 4, 2, 0, 0)
            assertTrue(
                planStartDate.isBefore(outsideDate),
                "계획 시작일($planStartDate)이 $outsideDate 이전이어야 합니다."
            )
        }
    }

    @Test
    fun `날짜 변환 및 비교 테스트`() {
        // 날짜 설정
        val from = LocalDate.of(2025, 3, 31)
        val to = LocalDate.of(2025, 4, 1)
        val testDate = LocalDate.of(2025, 4, 2)

        val fromTime = LocalDateTime.of(from, LocalTime.MIN)
        val toTime = LocalDateTime.of(to, LocalTime.MAX)
        val testDateTime = LocalDateTime.of(testDate, LocalTime.MIN)

        println("From date: $fromTime")
        println("To date: $toTime")
        println("Test date: $testDateTime")

        // 2025-04-02 00:00:00은 2025-04-01 23:59:59.999999999 이후여야 함
        assertTrue(testDateTime.isAfter(toTime), "테스트 날짜는 종료일 이후여야 합니다.")
    }
}