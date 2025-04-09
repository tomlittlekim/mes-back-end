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
    override fun getProductionResultList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        equipmentId: String?,
        planStartDateFrom: LocalDate?,  // 날짜 필드 타입 변경
        planStartDateTo: LocalDate?,    // 날짜 필드 타입 변경
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

        // 계획시작일 범위 필터링 (시작)
        planStartDateFrom?.let {
            val startOfDay = LocalDateTime.of(it, LocalTime.MIN)
            query.where(productionResult.createDate.goe(startOfDay))
        }

        // 계획시작일 범위 필터링 (종료)
        planStartDateTo?.let {
            val endOfDay = LocalDateTime.of(it, LocalTime.MAX)
            query.where(productionResult.createDate.loe(endOfDay))
        }

        // flagActive 필터링 (기본값은 true)
        query.where(productionResult.flagActive.eq(flagActive ?: true))

        // 생산실적ID 역순 정렬 추가
        query.orderBy(productionResult.prodResultId.desc())

        return query.fetch()
    }

    /**
     * 생산실적 목록 조회 (상세 조회용)
     */
    override fun getProductionResultListWithDetails(
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