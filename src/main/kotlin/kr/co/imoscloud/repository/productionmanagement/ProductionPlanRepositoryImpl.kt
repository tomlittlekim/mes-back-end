package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.QProductionPlan
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ProductionPlanRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionPlanRepositoryCustom, QuerydslRepositorySupport(ProductionPlan::class.java) {

    override fun getProductionPlanList(
        site: String,
        compCd: String,
        prodPlanId: String?,
        orderId: String?,
        productId: String?,
        planStartDateFrom: LocalDate?,
        planStartDateTo: LocalDate?,
        flagActive: Boolean?
    ): List<ProductionPlan> {
        val productionPlan = QProductionPlan.productionPlan
        val log = org.slf4j.LoggerFactory.getLogger(this::class.java)

        // 로깅 추가: 입력 파라미터 출력
        log.debug(
            "쿼리 파라미터 - site: {}, compCd: {}, prodPlanId: {}, orderId: {}, productId: {}, planStartDateFrom: {}, planStartDateTo: {}, flagActive: {}",
            site,
            compCd,
            prodPlanId,
            orderId,
            productId,
            planStartDateFrom,
            planStartDateTo,
            flagActive
        )

        val query = queryFactory
            .selectFrom(productionPlan)
            .where(
                productionPlan.site.eq(site),
                productionPlan.compCd.eq(compCd)
            )

        // prodPlanId 필터링
        prodPlanId?.let {
            if (it.isNotBlank()) {
                query.where(productionPlan.prodPlanId.like("%$it%"))
            }
        }

        // orderId 필터링
        orderId?.let {
            if (it.isNotBlank()) {
                query.where(productionPlan.orderId.like("%$it%"))
            }
        }

        // productId 필터링
        productId?.let {
            if (it.isNotBlank()) {
                query.where(productionPlan.productId.like("%$it%"))
            }
        }

        // planStartDateFrom 필터링 (계획시작일 범위 시작)
        var startDateTime: LocalDateTime? = null
        planStartDateFrom?.let {
            startDateTime = LocalDateTime.of(it, LocalTime.MIN)
            query.where(productionPlan.planStartDate.goe(startDateTime))
            log.debug("계획시작일 하한값: {}", startDateTime)
        }

        // planStartDateTo 필터링 (계획시작일 범위 끝)
        var endDateTime: LocalDateTime? = null
        planStartDateTo?.let {
            endDateTime = LocalDateTime.of(it, LocalTime.MAX)
            query.where(productionPlan.planStartDate.loe(endDateTime))
            log.debug("계획시작일 상한값: {}", endDateTime)
        }

        // flagActive 필터링
        flagActive?.let {
            query.where(productionPlan.flagActive.eq(it))
        }

        val results = query.fetch()

        // 결과 로깅
        log.debug("조회 결과 - 레코드 수: {}", results.size)
        if (startDateTime != null && endDateTime != null) {
            results.forEach { plan ->
                if (plan.planStartDate != null) {
                    val isInRange =
                        !plan.planStartDate!!.isBefore(startDateTime) && !plan.planStartDate!!.isAfter(
                            endDateTime
                        )
                    log.debug(
                        "계획 ID: {}, 계획시작일: {}, 범위 내 여부: {}",
                        plan.prodPlanId, plan.planStartDate, isInRange
                    )

                    // 범위를 벗어난 날짜가 있으면 경고 로그
                    if (!isInRange) {
                        log.warn(
                            "범위를 벗어난 날짜 발견! 계획 ID: {}, 계획시작일: {}, 범위: {} ~ {}",
                            plan.prodPlanId, plan.planStartDate, startDateTime, endDateTime
                        )
                    }
                }
            }
        }

        return results
    }
}