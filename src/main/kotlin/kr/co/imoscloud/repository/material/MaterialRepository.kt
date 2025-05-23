package kr.co.imoscloud.repository.Material

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.material.MaterialMaster
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
}
