package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.entity.productionmanagement.QDefectInfo
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDateTime

class DefectInfoRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : DefectInfoRepositoryCustom, QuerydslRepositorySupport(DefectInfo::class.java) {

    /**
     * 다중 생산실적 ID로 불량정보 목록 조회 (배치 삭제용)
     */
    override fun getDefectInfosByProdResultIds(
        site: String,
        compCd: String,
        prodResultIds: List<String>
    ): List<DefectInfo> {
        if (prodResultIds.isEmpty()) return emptyList()
        
        val defectInfo = QDefectInfo.defectInfo

        return queryFactory
            .selectFrom(defectInfo)
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.prodResultId.`in`(prodResultIds),
                defectInfo.flagActive.eq(true) // 활성화된 데이터만 조회
            )
            .fetch()
    }

    /**
     * 다중 생산실적 ID로 불량정보 배치 소프트 삭제 (QueryDSL + @Transactional)
     */
    @org.springframework.transaction.annotation.Transactional
    override fun batchSoftDeleteDefectInfosByProdResultIds(
        site: String,
        compCd: String,
        prodResultIds: List<String>,
        updateUser: String,
        updateDate: LocalDateTime
    ): Long {
        if (prodResultIds.isEmpty()) return 0L
        
        val defectInfo = QDefectInfo.defectInfo

        return queryFactory
            .update(defectInfo)
            .set(defectInfo.flagActive, false)
            .set(defectInfo.updateUser, updateUser)
            .set(defectInfo.updateDate, updateDate)
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.prodResultId.`in`(prodResultIds),
                defectInfo.flagActive.eq(true) // 이미 삭제된 것은 제외
            )
            .execute()
    }

}