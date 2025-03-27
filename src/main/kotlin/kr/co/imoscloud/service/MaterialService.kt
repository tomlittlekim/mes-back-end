package kr.co.imoscloud.service

import kr.co.imoscloud.entity.material.Material
import kr.co.imoscloud.entity.material.MaterialFilter
import org.springframework.stereotype.Service

@Service
class MaterialService(
    private val materialRepository: MaterialRepository
) {
    fun getMaterials(filter: MaterialFilter): List<MaterialResponseModel> {
        return materialRepository.getMaterialList(
            site = "imos",
            compCd = "eightPin",
            materialType = filter.materialType,
            materialId = filter.materialId,
            materialName = filter.materialName,
            useYn = filter.useYn,
            fromDate = filter.fromDate,
            toDate = filter.toDate
        )
    }

    fun saveMaterials(materials: List<Material>): List<Material> {
        return materialRepository.saveAll(materials)
    }

    fun deleteMaterials(ids: List<String>): Boolean {
        try {
            val materials = materialRepository.findAllById(ids.map { it.toInt() })
            materialRepository.deleteAll(materials)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

data class MaterialResponseModel(
    val systemMaterialId: String?,
    val type: String?,
    val name: String?,
    val spec: String?,
    val unit: String?,
    val minQuantity: Int?,
    val maxQuantity: Int?,
    val manufacturer: String?,
    val supplier: String?,
    val warehouse: String?,
    val flagActive: Boolean?
)