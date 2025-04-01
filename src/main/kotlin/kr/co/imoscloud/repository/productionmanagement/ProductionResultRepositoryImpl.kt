package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport

class ProductionResultRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionResultRepositoryCustom, QuerydslRepositorySupport(ProductionResult::class.java) {

    override fun getProductionResultsByWorkOrderId(
        site: String,
        compCd: String,
        workOrderId: String
    ): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult

        val query = queryFactory
            .selectFrom(productionResult)
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.workOrderId.eq(workOrderId),
                productionResult.flagActive.eq(true)
            )
            .orderBy(productionResult.createDate.desc())

        return query.fetch()
    }

    override fun getProductionResultList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        equipmentId: String?,
        flagActive: Boolean?
    ): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult

        val query = queryFactory
            .selectFrom(productionResult)
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd)
            )

        // workOrderId 필터링
        workOrderId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.workOrderId.eq(it))
            }
        }

        // prodResultId 필터링
        prodResultId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.prodResultId.like("%$it%"))
            }
        }

        // equipmentId 필터링
        equipmentId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.equipmentId.like("%$it%"))
            }
        }

        // flagActive 필터링
        flagActive?.let {
            query.where(productionResult.flagActive.eq(it))
        }

        return query.fetch()
    }
}