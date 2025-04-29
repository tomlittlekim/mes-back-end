package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.entity.productionmanagement.QDefectInfo
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import kr.co.imoscloud.model.productionmanagement.DefectInfoFilter
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DefectInfoRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : DefectInfoRepositoryCustom, QuerydslRepositorySupport(DefectInfo::class.java) {

    override fun getDefectInfoByProdResultId(
        site: String,
        compCd: String,
        prodResultId: String
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo
        val productionResult = QProductionResult.productionResult

        // 필요한 모든 필드를 직접 선택하여 한 번의 쿼리로 데이터 가져오기
        return queryFactory
            .select(
                Projections.fields(
                    DefectInfo::class.java,
                    defectInfo.id,
                    defectInfo.site,
                    defectInfo.compCd,
                    defectInfo.prodResultId,
                    defectInfo.defectId,
                    defectInfo.productId,
                    defectInfo.defectQty,
                    defectInfo.resultInfo,
                    defectInfo.state,
                    defectInfo.defectCause,
                    defectInfo.createDate,
                    defectInfo.createUser,
                    defectInfo.updateDate,
                    defectInfo.updateUser,
                    defectInfo.flagActive,
                    productionResult.equipmentId.`as`("equipmentId")
                )
            )
            .from(defectInfo)
            .leftJoin(productionResult)
            .on(
                defectInfo.site.eq(productionResult.site),
                defectInfo.compCd.eq(productionResult.compCd),
                defectInfo.prodResultId.eq(productionResult.prodResultId)
            )
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.prodResultId.eq(prodResultId),
                defectInfo.flagActive.eq(true)
            )
            .orderBy(defectInfo.createDate.desc())
            .fetch()
    }

    override fun getDefectInfoByFilter(
        site: String,
        compCd: String,
        filter: DefectInfoFilter?
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo
        val productionResult = QProductionResult.productionResult

        // 기본 조건 설정
        val whereClause = BooleanBuilder()
            .and(defectInfo.site.eq(site))
            .and(defectInfo.compCd.eq(compCd))
            .and(defectInfo.flagActive.eq(true))

        // 필터 조건 추가
        filter?.let {
            // 불량정보ID 필터 (LIKE 연산)
            it.defectId?.let { defectId ->
                whereClause.and(defectInfo.defectId.like("%$defectId%"))
            }

            // 생산실적ID 필터 (LIKE 연산)
            it.prodResultId?.let { prodResultId ->
                whereClause.and(defectInfo.prodResultId.like("%$prodResultId%"))
            }

            // 제품ID 필터 (정확한 일치)
            it.productId?.let { productId ->
                whereClause.and(defectInfo.productId.eq(productId))
            }

            // 설비ID 필터 - ProductionResult 테이블에서 필터링
            it.equipmentId?.let { equipmentId ->
                whereClause.and(productionResult.equipmentId.eq(equipmentId))
            }

            // 등록일 기간 필터 (fromDate)
            it.fromDate?.let { fromDateStr ->
                try {
                    // yyyy-MM-dd 형식으로 입력된 경우를 처리
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val fromDate = LocalDate.parse(fromDateStr, formatter)
                    val startOfDay = LocalDateTime.of(fromDate, LocalTime.MIN)
                    whereClause.and(defectInfo.createDate.goe(startOfDay))
                } catch (e: Exception) {
                    // yyyy-MM-ddTHH:mm:ss 형식으로 입력된 경우를 처리
                    try {
                        val formatterWithTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        val fromDateTime = LocalDateTime.parse(fromDateStr, formatterWithTime)
                        whereClause.and(defectInfo.createDate.goe(fromDateTime))
                    } catch (e: Exception) {
                        // 날짜 형식이 잘못된 경우 필터링 조건을 적용하지 않음
                    }
                }
            }
            
            // 등록일 기간 필터 (toDate)
            it.toDate?.let { toDateStr ->
                try {
                    // yyyy-MM-dd 형식으로 입력된 경우를 처리
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val toDate = LocalDate.parse(toDateStr, formatter)
                    // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
                    val nextDay = toDate.plusDays(1)
                    val startOfNextDay = LocalDateTime.of(nextDay, LocalTime.MIN)
                    whereClause.and(defectInfo.createDate.lt(startOfNextDay))
                } catch (e: Exception) {
                    // yyyy-MM-ddTHH:mm:ss 형식으로 입력된 경우를 처리
                    try {
                        val formatterWithTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        val toDateTime = LocalDateTime.parse(toDateStr, formatterWithTime)
                        whereClause.and(defectInfo.createDate.loe(toDateTime))
                    } catch (e: Exception) {
                        // 날짜 형식이 잘못된 경우 필터링 조건을 적용하지 않음
                    }
                }
            }
        }

        // 한 번의 쿼리로 조인을 통해 필요한 모든 데이터 가져오기
        return queryFactory
            .select(
                Projections.fields(
                    DefectInfo::class.java,
                    defectInfo.id,
                    defectInfo.site,
                    defectInfo.compCd,
                    defectInfo.prodResultId,
                    defectInfo.defectId,
                    defectInfo.productId,
                    defectInfo.defectQty,
                    defectInfo.resultInfo,
                    defectInfo.state,
                    defectInfo.defectCause,
                    defectInfo.createDate,
                    defectInfo.createUser,
                    defectInfo.updateDate,
                    defectInfo.updateUser,
                    defectInfo.flagActive,
                    productionResult.equipmentId.`as`("equipmentId")
                )
            )
            .from(defectInfo)
            .leftJoin(productionResult)
            .on(
                defectInfo.site.eq(productionResult.site),
                defectInfo.compCd.eq(productionResult.compCd),
                defectInfo.prodResultId.eq(productionResult.prodResultId)
            )
            .where(whereClause)
            .orderBy(defectInfo.createDate.desc())
            .distinct()
            .fetch()
    }

    override fun getDefectInfoForStats(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo
        val productionResult = QProductionResult.productionResult

        // 한 번의 쿼리로 조인을 통해 필요한 모든 데이터 가져오기
        return queryFactory
            .select(
                Projections.fields(
                    DefectInfo::class.java,
                    defectInfo.id,
                    defectInfo.site,
                    defectInfo.compCd,
                    defectInfo.prodResultId,
                    defectInfo.defectId,
                    defectInfo.productId,
                    defectInfo.defectQty,
                    defectInfo.resultInfo,
                    defectInfo.state,
                    defectInfo.defectCause,
                    defectInfo.createDate,
                    defectInfo.createUser,
                    defectInfo.updateDate,
                    defectInfo.updateUser,
                    defectInfo.flagActive,
                    productionResult.equipmentId.`as`("equipmentId")
                )
            )
            .from(defectInfo)
            .leftJoin(productionResult)
            .on(
                defectInfo.site.eq(productionResult.site),
                defectInfo.compCd.eq(productionResult.compCd),
                defectInfo.prodResultId.eq(productionResult.prodResultId)
            )
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.createDate.between(fromDate, toDate),
                defectInfo.flagActive.eq(true)
            )
            .orderBy(defectInfo.createDate.asc())
            .distinct()
            .fetch()
    }
}