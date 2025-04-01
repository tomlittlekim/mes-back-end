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
        planStartDate: LocalDate?,
        planEndDate: LocalDate?,
        flagActive: Boolean?
    ): List<ProductionPlan> {
        val productionPlan = QProductionPlan.productionPlan

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

        // planStartDate 필터링
        planStartDate?.let {
            val startOfDay = LocalDateTime.of(it, LocalTime.MIN)
            query.where(productionPlan.planStartDate.goe(startOfDay))
        }

        // planEndDate 필터링
        planEndDate?.let {
            val endOfDay = LocalDateTime.of(it, LocalTime.MAX)
            query.where(productionPlan.planEndDate.loe(endOfDay))
        }

        // flagActive 필터링
        flagActive?.let {
            query.where(productionPlan.flagActive.eq(it))
        }

        return query.fetch()
    }
}