package kr.co.imoscloud.service.material

import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.fetcher.material.MaterialFilter
import kr.co.imoscloud.repository.Material.MaterialRepository
import kr.co.imoscloud.util.DateUtils
import org.springframework.stereotype.Service

@Service
class MaterialService(
    private val materialRepository: MaterialRepository
) {
    fun getMaterials(filter: MaterialFilter): List<MaterialResponseModel?> {
        val materialList = materialRepository.getMaterialList(
            site = "imos",
            compCd = "eightPin",
            materialType = filter.materialType,
            systemMaterialId = filter.systemMaterialId,
            userMaterialId = filter.userMaterialId,
            materialName = filter.materialName,
            flagActive = filter.flagActive?.let { it == "Y" },
            fromDate = DateUtils.parseDate(filter.fromDate),
            toDate = DateUtils.parseDate(filter.toDate)
        )

        val result = materialList.map {
            MaterialResponseModel(
                it?.systemMaterialId,
                it?.userMaterialId,
                it?.materialType,
                it?.materialName,
                it?.materialStandard,
                it?.unit,
                it?.minQuantity,
                it?.maxQuantity,
                it?.manufacturerName,
                it?.supplierName,
                it?.materialStorage,
                if (it?.flagActive == true) "Y" else "N",
                it?.createUser,
                it?.createDate?.toString(),
                it?.updateUser,
                it?.updateDate?.toString()
            )
        }

        return result
    }

    fun saveMaterials(materials: List<MaterialMaster>): List<MaterialMaster> {
        return materialRepository.saveAll(materials)
    }

    fun deleteMaterials(ids: List<String>): Boolean {
        return try {
            val materials = materialRepository.findAllById(ids.map { it.toInt() })
            materialRepository.deleteAll(materials)
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class MaterialResponseModel(
    val systemMaterialId: String? = null,
    val userMaterialId: String? = null,
    val materialType: String? = null,
    val materialName: String? = null,
    val materialStandard: String? = null,
    val unit: String? = null,
    val minQuantity: Int? = null,
    val maxQuantity: Int? = null,
    val manufacturerName: String? = null,
    val supplierName: String? = null,
    val materialStorage: String? = null,
    val flagActive: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null,
)