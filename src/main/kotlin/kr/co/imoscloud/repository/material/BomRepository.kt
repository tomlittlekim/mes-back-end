package kr.co.imoscloud.repository.material

import kr.co.imoscloud.entity.material.Bom
import kr.co.imoscloud.model.material.BomMaterialDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface BomRepository : JpaRepository<Bom, Int> {
    @Query(
        """
        SELECT new kr.co.imoscloud.model.material.BomMaterialDto(
            b, 
            m.materialType,
            m.materialCategory,
            m.userMaterialId,
            m.materialName,
            m.materialStandard,
            m.unit
        )
        FROM Bom b
        LEFT JOIN MaterialMaster m 
            ON m.site = b.site 
            AND m.compCd = b.compCd 
            AND m.systemMaterialId = b.itemCd
        WHERE b.site = :site
        AND b.compCd = :compCd
        AND (:materialType = '' OR m.materialType = :materialType)
        AND (:materialName = '' OR m.materialName LIKE CONCAT('%', :materialName, '%'))
        AND (:bomName = '' OR b.bomName LIKE CONCAT('%', :bomName, '%'))
        AND b.flagActive = true
        ORDER BY b.createDate DESC
        """
    ) // BOM 정보 호출
    fun getBomList(
        site: String,
        compCd: String,
        materialType: String?,
        materialName: String?,
        bomName: String?,
//        flagActive: Boolean?
    ): List<BomMaterialDto>

    @Query(
        """
        SELECT new kr.co.imoscloud.model.material.BomMaterialDto(
            b,
            m.materialType,
            m.materialCategory,
            m.userMaterialId,
            m.materialName,
            m.materialStandard,
            m.unit
        )
        FROM Bom b
        LEFT JOIN MaterialMaster m 
            ON m.site = b.site 
            AND m.compCd = b.compCd 
            AND m.systemMaterialId = b.itemCd
        WHERE b.site = :site
        AND b.compCd = :compCd
        AND b.bomId IN :bomIds
        AND b.flagActive = true
        """
    )
    fun getBomByBomId(
        site: String,
        compCd: String,
        bomIds: List<String>
    ): List<BomMaterialDto>

    @Query("""
        SELECT b FROM Bom b
        WHERE b.site = :site
        AND b.compCd = :compCd
        AND b.bomId = :bomId
        AND b.flagActive = true
    """)
    fun findBomByBomId(
        site: String,
        compCd: String,
        bomId: String
    ): Bom?

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Bom b 
        SET b.flagActive = false
        WHERE b.site = :site 
        AND b.compCd = :compCd 
        AND b.bomId = :bomId
    """)
    fun updateBomFlagActive(
        site: String,
        compCd: String,
        bomId: String
    ): Int
} 