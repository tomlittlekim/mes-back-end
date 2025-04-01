package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import kr.co.imoscloud.entity.productionmanagement.QWorkOrder
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDateTime

/**
 * 생산실적조회 리포지토리 구현체
 * - QueryDSL을 활용한 고급 쿼리 처리
 */
class ProductionResultInquiryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionResultInquiryRepositoryCustom, QuerydslRepositorySupport(ProductionResult::class.java) {

    /**
     * 다양한 조건으로 생산실적 목록 조회
     */
    override fun getProductionResultList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        productId: String?,
        equipmentId: String?,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?,
        status: String?,
        flagActive: Boolean
    ): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult
        val workOrder = QWorkOrder.workOrder

        // 기본 조건
        val whereClause = BooleanBuilder()
            .and(productionResult.site.eq(site))
            .and(productionResult.compCd.eq(compCd))
            .and(productionResult.flagActive.eq(flagActive))

        // 선택적 조건 추가
        workOrderId?.let {
            if (it.isNotBlank()) {
                whereClause.and(productionResult.workOrderId.eq(it))
            }
        }

        prodResultId?.let {
            if (it.isNotBlank()) {
                whereClause.and(productionResult.prodResultId.like("%$it%"))
            }
        }

        equipmentId?.let {
            if (it.isNotBlank()) {
                whereClause.and(productionResult.equipmentId.like("%$it%"))
            }
        }

        // 날짜 범위 조건
        if (fromDate != null && toDate != null) {
            whereClause.and(productionResult.createDate.between(fromDate, toDate))
        } else if (fromDate != null) {
            whereClause.and(productionResult.createDate.goe(fromDate))
        } else if (toDate != null) {
            whereClause.and(productionResult.createDate.loe(toDate))
        }

        // 제품 ID로 조회 - JOIN 필요
        productId?.let {
            if (it.isNotBlank()) {
                return queryFactory
                    .selectFrom(productionResult)
                    .innerJoin(workOrder)
                    .on(productionResult.workOrderId.eq(workOrder.workOrderId))
                    .where(whereClause.and(workOrder.productId.eq(productId)))
                    .orderBy(productionResult.createDate.desc())
                    .fetch()
            }
        }

        // 상태로 조회 - JOIN 필요
        status?.let {
            if (it.isNotBlank()) {
                return queryFactory
                    .selectFrom(productionResult)
                    .innerJoin(workOrder)
                    .on(productionResult.workOrderId.eq(workOrder.workOrderId))
                    .where(whereClause.and(workOrder.state.eq(status)))
                    .orderBy(productionResult.createDate.desc())
                    .fetch()
            }
        }

        // 기본 쿼리 - JOIN 없음
        return queryFactory
            .selectFrom(productionResult)
            .where(whereClause)
            .orderBy(productionResult.createDate.desc())
            .fetch()
    }

    /**
     * 특정 생산실적 ID로 상세 정보 조회
     */
    override fun findDetailByProdResultId(
        site: String,
        compCd: String,
        prodResultId: String
    ): ProductionResult? {
        val productionResult = QProductionResult.productionResult

        return queryFactory
            .selectFrom(productionResult)
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.prodResultId.eq(prodResultId),
                productionResult.flagActive.eq(true)
            )
            .fetchOne()
    }

    /**
     * 기간별 생산실적 목록 조회
     */
    override fun getProductionResultListByDateRange(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult

        return queryFactory
            .selectFrom(productionResult)
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.createDate.between(fromDate, toDate),
                productionResult.flagActive.eq(true)
            )
            .orderBy(productionResult.createDate.asc())
            .fetch()
    }

    /**
     * 설비별 생산실적 집계 조회
     */
    override fun getProductionResultByEquipment(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult

        return queryFactory
            .selectFrom(productionResult)
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.createDate.between(fromDate, toDate),
                productionResult.flagActive.eq(true),
                productionResult.equipmentId.isNotNull
            )
            .orderBy(productionResult.equipmentId.asc())
            .fetch()
    }

    /**
     * 제품별 생산실적 집계 조회
     */
    override fun getProductionResultByProduct(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult
        val workOrder = QWorkOrder.workOrder

        return queryFactory
            .selectFrom(productionResult)
            .innerJoin(workOrder)
            .on(productionResult.workOrderId.eq(workOrder.workOrderId))
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.createDate.between(fromDate, toDate),
                productionResult.flagActive.eq(true)
            )
            .orderBy(workOrder.productId.asc())
            .fetch()
    }
}