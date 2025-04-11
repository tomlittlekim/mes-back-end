package kr.co.imoscloud.repository.material

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.entity.standardInfo.Factory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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
        AND (:flagActive IS NULL OR m.flagActive = :flagActive)
        AND (:fromDate IS NULL OR m.createDate >= :fromDate)
        AND (:toDate IS NULL OR m.createDate <= :toDate)
        """
    )
    fun getMaterialList(
        site: String,
        compCd: String,
        materialType: String?,
        userMaterialId: String?,
        materialName: String?,
        flagActive: Boolean?,
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
        AND (:flagActive IS NULL OR m.flagActive = :flagActive)
        AND (:fromDate IS NULL OR m.createDate >= :fromDate)
        AND (:toDate IS NULL OR m.createDate <= :toDate)
    """
    )
    fun getRawSubMaterialList(
        site: String,
        compCd: String,
        materialType: String?,
        userMaterialId: String?,
        materialName: String?,
        flagActive: Boolean?,
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
    fun findBySiteAndCompCdAndFlagActiveOrderByMaterialNameAsc(
        site: String,
        compCd: String,
        flagActive: Boolean
    ): List<MaterialMaster?>

}
