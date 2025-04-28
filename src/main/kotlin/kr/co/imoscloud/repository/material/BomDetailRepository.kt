package kr.co.imoscloud.repository.material

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.material.BomDetail
import kr.co.imoscloud.model.material.BomDetailMaterialDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface BomDetailRepository : JpaRepository<BomDetail, Int> {
    @Query(
        """
        SELECT new kr.co.imoscloud.model.material.BomDetailMaterialDto(
            d,
            m.materialType,
            m.materialCategory,
            m.userMaterialId,
            m.materialName,
            m.materialStandard,
            m.unit,
            pm.userMaterialId,
            pm.materialType,
            pm.materialName
        )
        FROM BomDetail d
            LEFT JOIN MaterialMaster m 
                ON m.site = d.site 
                AND m.compCd = d.compCd 
                AND m.systemMaterialId = d.itemCd
            LEFT JOIN MaterialMaster pm 
                ON pm.site = d.site 
                AND pm.compCd = d.compCd 
                AND pm.systemMaterialId = d.parentItemCd
        WHERE d.site = :site
        AND d.compCd = :compCd
        AND d.bomId = :bomId
        AND d.flagActive = true
        ORDER BY d.bomLevel ASC, d.createDate DESC
        """
    )
    fun getBomDetailListByBomId(
        site: String,
        compCd: String,
        bomId: String
    ): List<BomDetailMaterialDto>

    @Query(
        """
        SELECT new kr.co.imoscloud.model.material.BomDetailMaterialDto(
            d,
            m.materialType,
            m.materialCategory,
            m.userMaterialId,
            m.materialName,
            m.materialStandard,
            m.unit,
            pm.userMaterialId,
            pm.materialType,
            pm.materialName
        )
        FROM BomDetail d
            LEFT JOIN MaterialMaster m 
                ON m.site = d.site 
                AND m.compCd = d.compCd 
                AND m.systemMaterialId = d.itemCd
            LEFT JOIN MaterialMaster pm 
                ON pm.site = d.site 
                AND pm.compCd = d.compCd 
                AND pm.systemMaterialId = d.parentItemCd
        WHERE d.site = :site
        AND d.compCd = :compCd
        AND d.bomDetailId IN :bomDetailIds
        AND d.flagActive = true
        ORDER BY d.bomLevel ASC, d.createDate DESC
        """
    )
    fun getBomDetailListByBomDetailIds(
        site: String,
        compCd: String,
        bomDetailIds: List<String>
    ): List<BomDetailMaterialDto>

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE BomDetail bd 
        SET bd.flagActive = false
        WHERE bd.site = :site 
        AND bd.compCd = :compCd 
        AND bd.bomDetailId IN :bomDetailIds
    """)
    fun updateBomDetailsFlagActiveByBomDetailIds(
        site: String,
        compCd: String,
        bomDetailIds: List<String>
    ): Int
}