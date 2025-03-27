package kr.co.imoscloud.service

import kr.co.imoscloud.entity.material.Material
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface MaterialRepository : JpaRepository<Material, Int> {
    @Query(
        """
        SELECT NEW kr.co.imoscloud.service.MaterialResponseModel(
            m.systemMaterialId,
            m.type,
            m.name,
            m.spec,
            m.unit,
            m.minQuantity,
            m.maxQuantity,
            m.manufacturer,
            m.supplier,
            m.warehouse,
            m.flagActive
        )
        FROM Material m
        WHERE m.site = :site
        AND m.compCd = :compCd
        AND (:materialType = '' OR m.type LIKE CONCAT('%', :materialType, '%'))
        AND (:materialId = '' OR m.systemMaterialId LIKE CONCAT('%', :materialId, '%'))
        AND (:materialName = '' OR m.name LIKE CONCAT('%', :materialName, '%'))
        AND (:useYn = '' OR CASE WHEN m.flagActive = true THEN 'Y' ELSE 'N' END = :useYn)
        AND (:fromDate IS NULL OR m.createDate >= :fromDate)
        AND (:toDate IS NULL OR m.createDate <= :toDate)
        """
    )
    fun getMaterialList(
        site: String,
        compCd: String,
        materialType: String,
        materialId: String,
        materialName: String,
        useYn: String,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): List<MaterialResponseModel>
}