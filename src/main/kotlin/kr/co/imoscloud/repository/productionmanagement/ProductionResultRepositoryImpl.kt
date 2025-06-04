package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import kr.co.imoscloud.entity.system.QUser
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
     * 작업지시ID로 생산실적 목록 조회 (User LeftJoin으로 createUserName 포함 - 단일 쿼리)
     */
    override fun getProductionResultsByWorkOrderId(
        site: String,
        compCd: String,
        workOrderId: String
    ): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult
        val user = QUser.user

        // 단일 쿼리로 ProductionResult와 userName을 함께 조회
        val tuples = queryFactory
            .select(productionResult, user.userName)
            .from(productionResult)
            .leftJoin(user).on(
                productionResult.createUser.eq(user.loginId)
                    .and(user.flagActive.eq(true))
            )
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.workOrderId.eq(workOrderId),
                productionResult.flagActive.eq(true) // 활성화된 데이터만 조회
            )
            .orderBy(productionResult.createDate.desc())
            .fetch()

        // 튜플에서 ProductionResult와 userName을 추출하여 설정
        return tuples.map { tuple ->
            val pr = tuple.get(productionResult)!!
            val userName = tuple.get(user.userName)
            pr.createUserName = userName
            pr
        }
    }

    /**
     * 기본 생산실적 목록 조회 (User LeftJoin으로 createUserName 포함 - 진짜 단일 쿼리)
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
        val user = QUser.user

        val query = queryFactory
            .select(productionResult, user.userName)
            .from(productionResult)
            .leftJoin(user).on(
                productionResult.createUser.eq(user.loginId)
                    .and(user.flagActive.eq(true))
            )
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

        // 단일 쿼리로 ProductionResult와 userName을 함께 조회
        val tuples = query.fetch()

        // 튜플에서 ProductionResult와 userName을 추출하여 설정
        return tuples.map { tuple ->
            val pr = tuple.get(productionResult)!!
            val userName = tuple.get(user.userName)
            pr.createUserName = userName
            pr
        }
    }

    override fun getProductionResultsAtMobile(site: String, compCd: String, filter: ProductionResultFilter?): List<ProductionResult> {
        val productionResult = QProductionResult.productionResult
        val user = QUser.user

        val query = queryFactory
            .select(productionResult, user.userName)
            .from(productionResult)
            .leftJoin(user).on(
                productionResult.createUser.eq(user.loginId)
                    .and(user.flagActive.eq(true))
            )
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

        // 단일 쿼리로 ProductionResult와 userName을 함께 조회
        val tuples = query.fetch()

        // 튜플에서 ProductionResult와 userName을 추출하여 설정
        return tuples.map { tuple ->
            val pr = tuple.get(productionResult)!!
            val userName = tuple.get(user.userName)
            pr.createUserName = userName
            pr
        }
    }

    /**
     * 다중 생산실적 배치 소프트 삭제 (QueryDSL + @Transactional)
     */
    @org.springframework.transaction.annotation.Transactional
    override fun batchSoftDeleteProductionResults(
        site: String,
        compCd: String,
        prodResultIds: List<String>,
        updateUser: String,
        updateDate: LocalDateTime
    ): Long {
        if (prodResultIds.isEmpty()) return 0L
        
        log.debug("QueryDSL 배치 삭제 시작 - site: {}, compCd: {}, prodResultIds: {}", site, compCd, prodResultIds)
        
        val productionResult = QProductionResult.productionResult

        val result = queryFactory
            .update(productionResult)
            .set(productionResult.flagActive, false)
            .set(productionResult.updateUser, updateUser)
            .set(productionResult.updateDate, updateDate)
            .where(
                productionResult.site.eq(site),
                productionResult.compCd.eq(compCd),
                productionResult.prodResultId.`in`(prodResultIds),
                productionResult.flagActive.eq(true) // 이미 삭제된 것은 제외
            )
            .execute()
            
        log.debug("QueryDSL 배치 삭제 완료 - 업데이트된 레코드 수: {}", result)
        return result
    }
}