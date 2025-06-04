package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.entity.productionmanagement.QDefectInfo
import kr.co.imoscloud.entity.productionmanagement.QProductionResult
import kr.co.imoscloud.entity.standardInfo.QCode
import kr.co.imoscloud.entity.system.QUser
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

    /**
     * 필터 조건으로 불량 정보 조회 (QueryDSL + JOIN으로 createUserName 포함)
     */
    override fun getDefectInfosWithUserName(
        site: String,
        compCd: String,
        defectId: String?,
        prodResultId: String?,
        productId: String?,
        equipmentId: String?,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo
        val productionResult = QProductionResult.productionResult
        val code = QCode.code
        val user = QUser.user

        val results = queryFactory
            .select(defectInfo, code.codeName, productionResult.equipmentId, user.userName)
            .from(defectInfo)
            .leftJoin(productionResult).on(
                defectInfo.site.eq(productionResult.site)
                    .and(defectInfo.compCd.eq(productionResult.compCd))
                    .and(defectInfo.prodResultId.eq(productionResult.prodResultId))
            )
            .leftJoin(code).on(
                code.site.eq("default")
                    .and(code.compCd.eq("default"))
                    .and(defectInfo.defectCause.eq(code.codeId))
                    .and(code.codeClassId.eq("DEFECT_TYPE"))
                    .and(code.flagActive.eq(true))
            )
            .leftJoin(user).on(
                defectInfo.createUser.eq(user.loginId)
                    .and(user.flagActive.eq(true))
            )
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.flagActive.eq(true),
                defectId?.let { defectInfo.defectId.like("%${it}%") },
                prodResultId?.let { defectInfo.prodResultId.like("%${it}%") },
                productId?.let { defectInfo.productId.eq(it) },
                equipmentId?.let { productionResult.equipmentId.eq(it) },
                fromDate?.let { defectInfo.createDate.goe(it) },
                toDate?.let { defectInfo.createDate.loe(it) }
            )
            .orderBy(defectInfo.id.desc())
            .fetch()

        return results.map { 
            val defectInfoEntity = it.get(0, DefectInfo::class.java)!!
            val defectCauseName = it.get(1, String::class.java)
            val equipmentIdValue = it.get(2, String::class.java)
            val createUserName = it.get(3, String::class.java)

            defectInfoEntity.apply {
                this.defectCauseName = defectCauseName
                this.equipmentId = equipmentIdValue
                this.createUserName = createUserName
            }
        }
    }

    /**
     * 생산실적 ID로 불량 정보 조회 (QueryDSL + JOIN으로 createUserName 포함)
     */
    override fun getDefectInfosByProdResultIdWithUserName(
        site: String,
        compCd: String,
        prodResultId: String
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo
        val productionResult = QProductionResult.productionResult
        val code = QCode.code
        val user = QUser.user

        val results = queryFactory
            .select(defectInfo, code.codeName, productionResult.equipmentId, user.userName)
            .from(defectInfo)
            .leftJoin(productionResult).on(
                defectInfo.site.eq(productionResult.site)
                    .and(defectInfo.compCd.eq(productionResult.compCd))
                    .and(defectInfo.prodResultId.eq(productionResult.prodResultId))
            )
            .leftJoin(code).on(
                code.site.eq("default")
                    .and(code.compCd.eq("default"))
                    .and(defectInfo.defectCause.eq(code.codeId))
                    .and(code.codeClassId.eq("DEFECT_TYPE"))
                    .and(code.flagActive.eq(true))
            )
            .leftJoin(user).on(
                defectInfo.createUser.eq(user.loginId)
                    .and(user.flagActive.eq(true))
            )
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.prodResultId.eq(prodResultId),
                defectInfo.flagActive.eq(true)
            )
            .orderBy(defectInfo.createDate.desc())
            .fetch()

        return results.map { 
            val defectInfoEntity = it.get(0, DefectInfo::class.java)!!
            val defectCauseName = it.get(1, String::class.java)
            val equipmentIdValue = it.get(2, String::class.java)
            val createUserName = it.get(3, String::class.java)

            defectInfoEntity.apply {
                this.defectCauseName = defectCauseName
                this.equipmentId = equipmentIdValue
                this.createUserName = createUserName
            }
        }
    }

    /**
     * 일자별 불량 통계용 조회 (QueryDSL + JOIN으로 createUserName 포함)
     */
    override fun getDefectInfosForStatsWithUserName(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<DefectInfo> {
        val defectInfo = QDefectInfo.defectInfo
        val productionResult = QProductionResult.productionResult
        val code = QCode.code
        val user = QUser.user

        val results = queryFactory
            .select(defectInfo, code.codeName, productionResult.equipmentId, user.userName)
            .from(defectInfo)
            .leftJoin(productionResult).on(
                defectInfo.site.eq(productionResult.site)
                    .and(defectInfo.compCd.eq(productionResult.compCd))
                    .and(defectInfo.prodResultId.eq(productionResult.prodResultId))
            )
            .leftJoin(code).on(
                code.site.eq("default")
                    .and(code.compCd.eq("default"))
                    .and(defectInfo.defectCause.eq(code.codeId))
                    .and(code.codeClassId.eq("DEFECT_TYPE"))
                    .and(code.flagActive.eq(true))
            )
            .leftJoin(user).on(
                defectInfo.createUser.eq(user.loginId)
                    .and(user.flagActive.eq(true))
            )
            .where(
                defectInfo.site.eq(site),
                defectInfo.compCd.eq(compCd),
                defectInfo.createDate.between(fromDate, toDate),
                defectInfo.flagActive.eq(true)
            )
            .orderBy(defectInfo.createDate.asc())
            .fetch()

        return results.map { 
            val defectInfoEntity = it.get(0, DefectInfo::class.java)!!
            val defectCauseName = it.get(1, String::class.java)
            val equipmentIdValue = it.get(2, String::class.java)
            val createUserName = it.get(3, String::class.java)

            defectInfoEntity.apply {
                this.defectCauseName = defectCauseName
                this.equipmentId = equipmentIdValue
                this.createUserName = createUserName
            }
        }
    }

}