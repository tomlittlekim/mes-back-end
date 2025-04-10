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

}