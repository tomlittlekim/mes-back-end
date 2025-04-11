package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.QProductionPlan
import kr.co.imoscloud.entity.material.QMaterialMaster
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ProductionPlanRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionPlanRepositoryCustom, QuerydslRepositorySupport(ProductionPlan::class.java) {

    @Transactional(readOnly = true)
    override fun getProductionPlanList(
        site: String,
        compCd: String,
        prodPlanId: String?,
        orderId: String?,
        productId: String?,
        productName: String?, // 제품명 필드 추가
        shiftType: String?,
        planStartDateFrom: LocalDate?,
        planStartDateTo: LocalDate?,
        flagActive: Boolean?
    ): List<ProductionPlan> {
        val productionPlan = QProductionPlan.productionPlan
        val materialMaster = QMaterialMaster.materialMaster

        // 기본 쿼리 구성
        var query = queryFactory
            .selectFrom(productionPlan)
            .where(
                productionPlan.site.eq(site),
                productionPlan.compCd.eq(compCd)
            )

        // prodPlanId 필터링
        prodPlanId?.let {
            if (it.isNotBlank()) {
                query = query.where(productionPlan.prodPlanId.like("%$it%"))
            }
        }

        // orderId 필터링
        orderId?.let {
            if (it.isNotBlank()) {
                query = query.where(productionPlan.orderId.like("%$it%"))
            }
        }

        // productId 필터링
        productId?.let {
            if (it.isNotBlank()) {
                query = query.where(productionPlan.productId.like("%$it%"))
            }
        }

        // shiftType 필터링
        shiftType?.let {
            if (it.isNotBlank()) {
                query = query.where(productionPlan.shiftType.eq(it))
            }
        }

        // planStartDateFrom 필터링 (계획시작일 범위 시작)
        planStartDateFrom?.let {
            val startOfDay = LocalDateTime.of(it, LocalTime.MIN)
            query = query.where(productionPlan.planStartDate.goe(startOfDay))
        }

        // planStartDateTo 필터링 (계획시작일 범위 끝)
        planStartDateTo?.let {
            // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
            val nextDay = it.plusDays(1)
            val startOfNextDay = LocalDateTime.of(nextDay, LocalTime.MIN)
            query = query.where(productionPlan.planStartDate.lt(startOfNextDay))
        }

        // flagActive 필터링 (기본값은 true)
        query = query.where(productionPlan.flagActive.eq(flagActive ?: true))

        // 생산계획ID 역순 정렬 추가
        query = query.orderBy(productionPlan.prodPlanId.desc())

        // 제품명으로 필터링 (조인 사용)
        if (productName != null && productName.isNotBlank()) {
            // 먼저 필터링된 생산계획 ID 목록 가져오기
            val plans = query.fetch()

            // 제품ID 목록 추출
            val productIds = plans.mapNotNull { it.productId }.distinct()

            if (productIds.isNotEmpty()) {
                // 제품명으로 필터링된 제품ID 목록
                val filteredProductIds = queryFactory
                    .select(materialMaster.systemMaterialId)
                    .from(materialMaster)
                    .where(
                        materialMaster.site.eq(site),
                        materialMaster.compCd.eq(compCd),
                        materialMaster.materialName.like("%$productName%"),
                        materialMaster.systemMaterialId.`in`(productIds)
                    )
                    .fetch()

                // 필터링된 제품ID에 해당하는 생산계획만 반환
                return plans.filter { plan ->
                    plan.productId in filteredProductIds
                }
            }
            return emptyList()
        }

        return query.fetch()
    }
}