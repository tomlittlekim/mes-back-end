package kr.co.imoscloud.repository.material

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.service.material.MaterialNameAndSysIdResponseModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface MaterialRepository : JpaRepository<MaterialMaster, Int> {
    @Query(
        """
        SELECT m
        FROM MaterialMaster m
        WHERE m.site = :site
        AND m.compCd = :compCd
        AND (:materialType = '' OR m.materialType LIKE CONCAT('%', :materialType, '%'))
        AND (:userMaterialId = '' OR m.userMaterialId LIKE CONCAT('%', :userMaterialId, '%'))
        AND (:materialName = '' OR m.materialName LIKE CONCAT('%', :materialName, '%'))
        AND (:fromDate IS NULL OR m.createDate >= :fromDate)
        AND (:toDate IS NULL OR m.createDate <= :toDate)
        AND m.flagActive IS true 
        """
    )
    fun getMaterialList(
        site: String,
        compCd: String,
        materialType: String?,
        userMaterialId: String?,
        materialName: String?,
//        flagActive: Boolean?,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?
    ): List<MaterialMaster?>

    @Query(
        """
        SELECT m
        FROM MaterialMaster m
        WHERE m.site = :site
        AND m.compCd = :compCd
        AND m.materialType IN ('RAW_MATERIAL', 'SUB_MATERIAL')
        AND (:materialType = '' OR m.materialType = :materialType)
        AND (:userMaterialId = '' OR m.userMaterialId LIKE CONCAT('%', :userMaterialId, '%'))
        AND (:materialName = '' OR m.materialName LIKE CONCAT('%', :materialName, '%'))
        AND (:fromDate IS NULL OR m.createDate >= :fromDate)
        AND (:toDate IS NULL OR m.createDate <= :toDate)
        AND m.flagActive IS true 
    """
    )
    fun getRawSubMaterialList(
        site: String,
        compCd: String,
        materialType: String?,
        userMaterialId: String?,
        materialName: String?,
//        flagActive: Boolean?,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?
    ): List<MaterialMaster?>

    @Query(
        value = """
            SELECT m
            FROM MaterialMaster m
            WHERE m.site = :site
            AND   m.compCd = :compCd
            AND   m.systemMaterialId IN (:systemMaterialIds)
        """
    )
    fun getMaterialListByIds(
        site: String,
        compCd: String,
        systemMaterialIds: List<String?>
    ): List<MaterialMaster?>

    @Transactional
    @Modifying
    @Query(
        """
        DELETE 
        FROM MaterialMaster m
        WHERE m.systemMaterialId IN (:systemMaterialIds)
        AND   m.site = :site
        AND   m.compCd = :compCd
        """
    )
    fun deleteMaterialsByIds(
        site: String,
        compCd: String,
        systemMaterialIds: List<String?>
    ): Int

    //단순 조회용 메서드 추가
    fun findBySystemMaterialId(
        systemMaterialId: String?
    ): MaterialMaster?

    @Query(
        """
        SELECT m
        FROM MaterialMaster m
        WHERE m.site = :site
        AND m.compCd = :compCd
        AND m.materialType = :materialType
        AND m.flagActive = true
        ORDER BY m.createDate DESC
        """
    )
    fun getMaterialsByType(
        site: String,
        compCd: String,
        materialType: String
    ): List<MaterialMaster>

    @Query(
        """
        SELECT m
        FROM MaterialMaster m
        WHERE m.site = :site
        AND m.compCd = :compCd
        AND m.flagActive = true
        ORDER BY m.createDate DESC
        """
    )
    fun getMaterialCode(
        site: String,
        compCd: String,
    ): List<MaterialMaster>

    //단순 조회용 name, sysId 반환
    @Query(
        value = """
        SELECT
            mm.SYSTEM_MATERIAL_ID AS systemMaterialId,
            mm.MATERIAL_NAME AS materialName
        FROM MATERIAL_MASTER mm
        WHERE mm.MATERIAL_TYPE IN ('HALF_PRODUCT', 'COMPLETE_PRODUCT')
          AND mm.FLAG_ACTIVE = true
          AND mm.COMP_CD = :compCd
          AND mm.SITE = :site
    """,
        nativeQuery = true
    )
    fun findMaterialNameAndSysId(
        @Param("compCd") compCd: String?,
        @Param("site") site: String?,
        @Param("materialTypes") materialTypes: List<String>?,
        @Param("flagActive") flagActive: Boolean?
    ): List<MaterialNameAndSysIdResponseModel>

    @Query(
        """
        SELECT m
        FROM MaterialMaster m
        WHERE m.site = :site
        AND m.compCd = :compCd
        AND m.flagActive = true
        ORDER BY m.materialType, m.materialCategory, m.materialName
        """
    )
    fun getAllMaterials(
        site: String,
        compCd: String
    ): List<MaterialMaster>

    // 사이트, 회사코드, 활성화 여부로 조회 (정렬 포함)
    fun findBySiteAndCompCdAndMaterialTypeInAndFlagActiveOrderByMaterialNameAsc(
        site: String,
        compCd: String,
        materialTypes: List<String>,
        flagActive: Boolean
    ): List<MaterialMaster?>

    fun findByCompCdAndSiteAndSystemMaterialId(compCd: String, site: String, systemMaterialId: String): MaterialMaster?


    @Query("""
        select m
        from MaterialMaster m
        where m.site = :site
            and m.compCd = :compCd
            and m.materialType in ('HALF_PRODUCT','COMPLETE_PRODUCT')
            and m.flagActive is true 
    """)
    fun getProductsBySameCompany(site:String, compCd:String): List<MaterialMaster>
}
