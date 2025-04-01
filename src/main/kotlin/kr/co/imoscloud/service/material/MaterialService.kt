package kr.co.imoscloud.service.material

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.fetcher.material.MaterialFilter
import kr.co.imoscloud.fetcher.material.MaterialInput
import kr.co.imoscloud.fetcher.material.MaterialUpdate
import kr.co.imoscloud.repository.Material.MaterialRepository
import kr.co.imoscloud.util.DateUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class MaterialService(
    private val materialRep: MaterialRepository
) {
    fun getMaterials(filter: MaterialFilter): List<MaterialResponseModel?> {
        val materialList = materialRep.getMaterialList(
            site = "imos",
            compCd = "eightPin",
            materialType = filter.materialType,
            userMaterialId = filter.userMaterialId,
            materialName = filter.materialName,
            flagActive = filter.flagActive?.let { it == "Y" },
            fromDate = DateUtils.parseDate(filter.fromDate),
            toDate = DateUtils.parseDate(filter.toDate)
        )

        return entityToResponse(materialList)
    }

    private fun entityToResponse(materialList: List<MaterialMaster?>): List<MaterialResponseModel?> {
        return materialList.map {
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
                it?.supplierId,
                it?.supplierName,
                it?.materialStorage,
                if (it?.flagActive == true) "Y" else "N",
                it?.createUser,
                it?.createDate?.toString(),
                it?.updateUser,
                it?.updateDate?.toString()
            )
        }
    }

    @Transactional
    fun saveMaterials(createdRows: List<MaterialInput?>, updatedRows:List<MaterialUpdate?>){
        //TODO 저장 ,수정시 공통 으로 작성자 ,작성일 ,수정자 ,수정일 변경 저장이 필요함
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createMaterials(it)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateMaterials(it)}
    }

    fun createMaterials(createdRows: List<MaterialInput?>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val materialList = createdRows.map {
            MaterialMaster().apply {
                systemMaterialId = "MAT" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3)
                site = "imos"
                compCd = "eightPin"
                materialType = it?.materialType
                userMaterialId = it?.userMaterialId
                materialName = it?.materialName
                materialStandard = it?.materialStandard
                unit = it?.unit
                minQuantity = it?.minQuantity
                maxQuantity = it?.maxQuantity
                manufacturerName = it?.manufacturerName
                supplierId = it?.supplierId
                materialStorage = it?.materialStorage
                flagActive = it?.flagActive == "Y"
                createUser = "phj"
                createDate = LocalDate.now()
            }
        }

        materialRep.saveAll(materialList)
    }

    fun updateMaterials(updatedRows: List<MaterialUpdate?>){
        val systemMaterialIds = updatedRows.map {
            it?.systemMaterialId
        }

        val materialList = materialRep.getMaterialListByIds(
            site = "imos",
            compCd = "eightPin",
            systemMaterialIds = systemMaterialIds
        )

        val updateList = materialList.associateBy { it?.systemMaterialId }

        updatedRows.forEach{ x ->
            val systemMaterialId = x?.systemMaterialId
            val material = updateList[systemMaterialId]

            material?.let{
                it.materialType = x?.materialType
                it.userMaterialId = x?.userMaterialId
                it.materialName = x?.materialName
                it.materialStandard = x?.materialStandard
                it.unit = x?.unit
                it.minQuantity = x?.minQuantity
                it.maxQuantity = x?.maxQuantity
                it.manufacturerName = x?.manufacturerName
                it.supplierId = x?.supplierId
                it.materialStorage = x?.materialStorage
                it.flagActive = x?.flagActive == "Y"
                it.updateUser = "phj"
                it.updateDate = LocalDate.now()
            }
        }

        materialRep.saveAll(materialList)
    }

    fun deleteMaterials(systemMaterialIds: List<String>): Boolean {
        return materialRep.deleteMaterialsByIds(
            site = "imos",
            compCd = "eightPin",
            systemMaterialIds = systemMaterialIds
        ) > 0
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
    val supplierId: String? = null,
    val supplierName: String? = null,
    val materialStorage: String? = null,
    val flagActive: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null,
)