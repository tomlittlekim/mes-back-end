package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.QProductionPlan
import kr.co.imoscloud.service.productionmanagement.ProductionPlanResponseModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ProductionPlanRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionPlanRepositoryCustom {

    override fun getProductionPlanList(
        site: String,
        compCd: String,
        prodPlanId: String?,
        orderId: String?,
        productId: String?,
        planStartDate: LocalDate?,
        planEndDate: LocalDate?
    ): List<ProductionPlanResponseModel?> {
        val productionPlan = QProductionPlan.productionPlan

        val query = queryFactory
            .select(
                Projections.constructor(
                    ProductionPlanResponseModel::class.java,
                    productionPlan.site,
                    productionPlan.compCd,
                    productionPlan.prodPlanId,
                    productionPlan.orderId,
                    productionPlan.productId,
                    productionPlan.planQty,
                    productionPlan.planStartDate,
                    productionPlan.planEndDate
                )
            )
            .from(productionPlan)
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

        return query.fetch()
    }
}