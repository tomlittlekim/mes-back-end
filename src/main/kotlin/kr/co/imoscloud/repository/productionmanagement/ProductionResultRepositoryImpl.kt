package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import kr.co.imoscloud.entity.productionmanagement.QWorkOrder
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ProductionResultRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionResultRepositoryCustom, QuerydslRepositorySupport(ProductionResult::class.java) {

    /**
     * 작업지시ID로 생산실적 목록 조회
     */
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
                productionResult.flagActive.eq(true) // 활성화된 데이터만 조회
            )
            .orderBy(productionResult.createDate.desc())

        return query.fetch()
    }

    /**
     * 기본 생산실적 목록 조회
     */
    override fun getProductionResults(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        productId: String?,
        equipmentId: String?,
        prodStartTimeFrom: LocalDate?,
        prodStartTimeTo: LocalDate?,
        prodEndTimeFrom: LocalDate?,
        prodEndTimeTo: LocalDate?,
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

        productId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.productId.like("%$it%"))
            }
        }

        // equipmentId 필터링
        equipmentId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.equipmentId.like("%$it%"))
            }
        }

        // flagActive 필터링 (기본값은 true)
        query.where(productionResult.flagActive.eq(flagActive ?: true))

        // 생산실적ID 역순 정렬 추가
        query.orderBy(productionResult.prodResultId.desc())

        return query.fetch()
    }

}