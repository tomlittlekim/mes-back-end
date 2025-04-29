package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDateTime
import java.time.LocalTime

class ProductionResultRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionResultRepositoryCustom, QuerydslRepositorySupport(ProductionResult::class.java) {
    private val log = LoggerFactory.getLogger(ProductionResultRepositoryImpl::class.java)

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
        warehouseId: String?,
        prodStartTimeFrom: LocalDateTime?,
        prodStartTimeTo: LocalDateTime?,
        prodEndTimeFrom: LocalDateTime?,
        prodEndTimeTo: LocalDateTime?,
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
                query.where(productionResult.workOrderId.like("%$it%"))
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
                query.where(productionResult.productId.eq(it))
            }
        }

        // equipmentId 필터링
        equipmentId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.equipmentId.like("%$it%"))
            }
        }

        warehouseId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.warehouseId.eq(it))
            }
        }

        // 생산시작일시 범위 필터링
        prodStartTimeFrom?.let {
            query.where(productionResult.prodStartTime.goe(it))
            log.debug("생산시작일시 하한값: {}", it)
        }
        
        prodStartTimeTo?.let {
            // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
            val nextDay = it.plusDays(1)
            val startOfNextDay = LocalDateTime.of(nextDay.toLocalDate(), LocalTime.MIN)
            query.where(productionResult.prodStartTime.lt(startOfNextDay))
            log.debug("생산시작일시 상한값: {}", startOfNextDay)
        }
        
        // 생산종료일시 범위 필터링
        prodEndTimeFrom?.let {
            query.where(productionResult.prodEndTime.goe(it))
            log.debug("생산종료일시 하한값: {}", it)
        }
        
        prodEndTimeTo?.let {
            // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
            val nextDay = it.plusDays(1)
            val startOfNextDay = LocalDateTime.of(nextDay.toLocalDate(), LocalTime.MIN)
            query.where(productionResult.prodEndTime.lt(startOfNextDay))
            log.debug("생산종료일시 상한값: {}", startOfNextDay)
        }

        // flagActive 필터링 (기본값은 true)
        query.where(productionResult.flagActive.eq(flagActive ?: true))

        // 생산실적ID 역순 정렬 추가
        query.orderBy(productionResult.id.desc())

        return query.fetch()
    }

    override fun getProductionResultsAtMobile(site: String, compCd: String, filter: ProductionResultFilter?): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult

        val query = queryFactory
            .selectFrom(productionResult)
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.flagActive.eq(true),
                productionResult.prodStartTime.isNotNull,
                productionResult.prodEndTime.isNull
            )

        // productId 필터링
        filter?.productId?.let {
            if (it.isNotBlank()) {
                query.where(productionResult.productId.like("%$it%"))
            }
        }
        
        // 생산시작일시 범위 필터링
        filter?.prodStartTimeFrom?.let {
            query.where(productionResult.prodStartTime.goe(it))
            log.debug("모바일에서 생산시작일시 하한값: {}", it)
        }
        
        filter?.prodStartTimeTo?.let {
            // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
            val nextDay = it.plusDays(1)
            val startOfNextDay = LocalDateTime.of(nextDay.toLocalDate(), LocalTime.MIN)
            query.where(productionResult.prodStartTime.lt(startOfNextDay))
            log.debug("모바일에서 생산시작일시 상한값: {}", startOfNextDay)
        }
        
        // 생산종료일시 범위 필터링
        filter?.prodEndTimeFrom?.let {
            query.where(productionResult.prodEndTime.goe(it))
            log.debug("모바일에서 생산종료일시 하한값: {}", it)
        }
        
        filter?.prodEndTimeTo?.let {
            // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
            val nextDay = it.plusDays(1)
            val startOfNextDay = LocalDateTime.of(nextDay.toLocalDate(), LocalTime.MIN)
            query.where(productionResult.prodEndTime.lt(startOfNextDay))
            log.debug("모바일에서 생산종료일시 상한값: {}", startOfNextDay)
        }

        // 생산실적ID 역순 정렬 추가
        query.orderBy(productionResult.id.desc())

        return query.fetch()
    }

}