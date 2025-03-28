package kr.co.imoscloud.repository.Material

import kr.co.imoscloud.entity.material.MaterialMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface MaterialRepository : JpaRepository<MaterialMaster, Int> {
    @Query(
        """
        SELECT m
        FROM MaterialMaster m
        WHERE m.site = :site
        AND m.compCd = :compCd
        AND (:materialType = '' OR m.materialType LIKE CONCAT('%', :materialType, '%'))
        AND (:systemMaterialId = '' OR m.systemMaterialId LIKE CONCAT('%', :systemMaterialId, '%'))
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
        systemMaterialId: String?,
        userMaterialId: String?,
        materialName: String?,
        flagActive: Boolean?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): List<MaterialMaster>
}