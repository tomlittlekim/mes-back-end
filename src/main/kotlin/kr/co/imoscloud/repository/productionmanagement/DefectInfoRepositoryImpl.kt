package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.entity.productionmanagement.QDefectInfo
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import kr.co.imoscloud.entity.productionmanagement.QWorkOrder
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDateTime

class DefectInfoRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : DefectInfoRepositoryCustom, QuerydslRepositorySupport(DefectInfo::class.java) {

    override fun getDefectInfoByProdResultId(
        site: String,
        compCd: String,
        prodResultId: String
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo

        val query = queryFactory
            .selectFrom(defectInfo)
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.prodResultId.eq(prodResultId),
                defectInfo.flagActive.eq(true)
            )
            .orderBy(defectInfo.createDate.desc())

        return query.fetch()
    }

    override fun getDefectInfoByWorkOrderId(
        site: String,
        compCd: String,
        workOrderId: String
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo

        val query = queryFactory
            .selectFrom(defectInfo)
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.workOrderId.eq(workOrderId),
                defectInfo.flagActive.eq(true)
            )
            .orderBy(defectInfo.createDate.desc())

        return query.fetch()
    }

    override fun getDefectInfoForStats(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo

        val query = queryFactory
            .selectFrom(defectInfo)
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.createDate.between(fromDate, toDate),
                defectInfo.flagActive.eq(true)
            )
            .orderBy(defectInfo.createDate.asc())

        return query.fetch()
    }

    override fun getDefectInfoList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        defectId: String?,
        productId: String?,
        state: String?,
        defectType: String?,
        equipmentId: String?,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?,
        flagActive: Boolean?
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo
        val workOrder = QWorkOrder.workOrder
        val productionResult = QProductionResult.productionResult

        // 조인 필요 여부 체크
        val needsWorkOrderJoin = defectType != null
        val needsProductionResultJoin = equipmentId != null

        // 기본 쿼리 생성
        val query = queryFactory.selectFrom(defectInfo)

        // 기본 조건 추가
        query.where(
            defectInfo.site.eq(site),
            defectInfo.compCd.eq(compCd)
        )

        // workOrderId 필터링
        workOrderId?.takeIf { it.isNotBlank() }?.let {
            query.where(defectInfo.workOrderId.eq(it))
        }

        // prodResultId 필터링
        prodResultId?.takeIf { it.isNotBlank() }?.let {
            query.where(defectInfo.prodResultId.eq(it))
        }

        // defectId 필터링
        defectId?.takeIf { it.isNotBlank() }?.let {
            query.where(defectInfo.defectId.like("%$it%"))
        }

        // productId 필터링
        productId?.takeIf { it.isNotBlank() }?.let {
            query.where(defectInfo.productId.like("%$it%"))
        }

        // state 필터링
        state?.takeIf { it.isNotBlank() }?.let {
            query.where(defectInfo.state.eq(it))
        }

        // defectType 필터링 (결과 정보에서 검색)
        defectType?.takeIf { it.isNotBlank() }?.let {
            query.where(defectInfo.resultInfo.like("%$it%"))
        }

        // equipmentId 필터링 - ProductionResult와 조인 필요
        equipmentId?.takeIf { it.isNotBlank() }?.let {
            // ProductionResult와 조인하여 equipmentId로 필터링
            query.join(productionResult)
                .on(defectInfo.prodResultId.eq(productionResult.prodResultId))
                .where(productionResult.equipmentId.like("%$it%"))
        }

        // 기간 필터링
        if (fromDate != null && toDate != null) {
            query.where(defectInfo.createDate.between(fromDate, toDate))
        } else {
            fromDate?.let {
                query.where(defectInfo.createDate.goe(it))
            }

            toDate?.let {
                query.where(defectInfo.createDate.loe(it))
            }
        }

        // flagActive 필터링
        flagActive?.let {
            query.where(defectInfo.flagActive.eq(it))
        }

        // 정렬 조건
        query.orderBy(defectInfo.createDate.desc())

        return query.fetch()
    }
}