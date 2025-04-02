package kr.co.imoscloud.service.material

import jakarta.transaction.Transactional
import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.fetcher.material.MaterialFilter
import kr.co.imoscloud.fetcher.material.MaterialInput
import kr.co.imoscloud.fetcher.material.MaterialUpdate
import kr.co.imoscloud.repository.Material.MaterialRepository
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class MaterialService(
    private val materialRep: MaterialRepository,
) {
    private val DEFAULT_SITE = "imos"
    private val DEFAULT_COMP_CD = "eightPin"

    private fun getCurrentUser() = try {
        SecurityUtils.getCurrentUserPrincipalOrNull()
    } catch (e: SecurityException) {
        null
    }

    fun getRawSubMaterials(filter: MaterialFilter): List<MaterialResponseModel?> {
        val userPrincipal = getCurrentUser()
        val materialList = materialRep.getRawSubMaterialList(
            site = userPrincipal?.getSite() ?: DEFAULT_SITE,
            compCd = userPrincipal?.compCd ?: DEFAULT_COMP_CD,
            materialType = filter.materialType,
            userMaterialId = filter.userMaterialId,
            materialName = filter.materialName,
            flagActive = filter.flagActive?.let { it == "Y" },
            fromDate = DateUtils.parseDateTime(filter.fromDate),
            toDate = DateUtils.parseDateTime(filter.toDate)
        )

        return entityToResponse(materialList)
    }

    fun getCompleteMaterials(filter: MaterialFilter): List<MaterialResponseModel?> {
        val userPrincipal = getCurrentUser()
        val materialList = materialRep.getMaterialList(
            site = userPrincipal?.getSite() ?: DEFAULT_SITE,
            compCd = userPrincipal?.compCd ?: DEFAULT_COMP_CD,
            materialType = CoreEnum.MaterialType.COMPLETE_PRODUCT.key,
            userMaterialId = filter.userMaterialId,
            materialName = filter.materialName,
            flagActive = filter.flagActive?.let { it == "Y" },
            fromDate = DateUtils.parseDateTime(filter.fromDate),
            toDate = DateUtils.parseDateTime(filter.toDate)
        )
        return entityToResponse(materialList)
    }

    fun getHalfMaterials(filter: MaterialFilter): List<MaterialResponseModel?> {
        val userPrincipal = getCurrentUser()
        val materialList = materialRep.getMaterialList(
            site = userPrincipal?.getSite() ?: DEFAULT_SITE,
            compCd = userPrincipal?.compCd ?: DEFAULT_COMP_CD,
            materialType = CoreEnum.MaterialType.HALF_PRODUCT.key,
            userMaterialId = filter.userMaterialId,
            materialName = filter.materialName,
            flagActive = filter.flagActive?.let { it == "Y" },
            fromDate = DateUtils.parseDateTime(filter.fromDate),
            toDate = DateUtils.parseDateTime(filter.toDate)
        )
        return entityToResponse(materialList)
    }

    private fun entityToResponse(materialList: List<MaterialMaster?>): List<MaterialResponseModel?> {
        return materialList.map {
            MaterialResponseModel(
                it?.systemMaterialId,
                it?.userMaterialId,
                it?.materialType,
                it?.materialCategory,
                it?.materialName,
                it?.materialStandard,
                it?.unit,
                it?.minQuantity,
                it?.maxQuantity,
                it?.baseQuantity,
                it?.manufacturerName,
                it?.supplierId,
                it?.supplierName,
                it?.materialStorage,
                if (it?.flagActive == true) "Y" else "N",
                it?.createUser,
                DateUtils.formatLocalDate(it?.createDate),
                it?.updateUser,
                DateUtils.formatLocalDate(it?.updateDate)
            )
        }
    }

    @Transactional
    fun saveMaterials(createdRows: List<MaterialInput?>, updatedRows: List<MaterialUpdate?>) {
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { createMaterials(it) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { updateMaterials(it) }
    }

    fun createMaterials(createdRows: List<MaterialInput?>) {
        val userPrincipal = getCurrentUser()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val materialList = createdRows.map {
            MaterialMaster(
                systemMaterialId = "MAT" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                site = userPrincipal?.getSite() ?: DEFAULT_SITE,
                compCd = userPrincipal?.compCd ?: DEFAULT_COMP_CD,
                materialType = it?.materialType,
                materialCategory = it?.materialCategory,
                userMaterialId = it?.userMaterialId,
                materialName = it?.materialName,
                materialStandard = it?.materialStandard,
                unit = it?.unit,
                minQuantity = it?.minQuantity,
                maxQuantity = it?.maxQuantity,
                baseQuantity = it?.baseQuantity,
                manufacturerName = it?.manufacturerName,
                supplierId = it?.supplierId,
                materialStorage = it?.materialStorage,
            ).apply {
                flagActive = it?.flagActive.equals("Y" )
                createCommonCol(userPrincipal!!)
            }
        }

        materialRep.saveAll(materialList)
    }

    fun updateMaterials(updatedRows: List<MaterialUpdate?>) {
        val userPrincipal = getCurrentUser()
        val systemMaterialIds = updatedRows.map {
            it?.systemMaterialId
        }

        val materialList = materialRep.getMaterialListByIds(
            site = userPrincipal?.getSite() ?: DEFAULT_SITE,
            compCd = userPrincipal?.compCd ?: DEFAULT_COMP_CD,
            systemMaterialIds = systemMaterialIds
        )

        val updateList = materialList.associateBy { it?.systemMaterialId }

        updatedRows.forEach { x ->
            val systemMaterialId = x?.systemMaterialId
            val material = updateList[systemMaterialId]

            material?.let {
                it.materialType = x?.materialType
                it.materialCategory = x?.materialCategory
                it.userMaterialId = x?.userMaterialId
                it.materialName = x?.materialName
                it.materialStandard = x?.materialStandard
                it.unit = x?.unit
                it.minQuantity = x?.minQuantity
                it.maxQuantity = x?.maxQuantity
                it.baseQuantity = x?.baseQuantity
                it.manufacturerName = x?.manufacturerName
                it.supplierId = x?.supplierId
                it.materialStorage = x?.materialStorage
                it.flagActive = x?.flagActive.equals("Y")
                it.updateCommonCol(userPrincipal!!)
            }
        }

        materialRep.saveAll(materialList)
    }

    fun deleteMaterials(systemMaterialIds: List<String>): Boolean {
        val userPrincipal = getCurrentUser()
        return materialRep.deleteMaterialsByIds(
            site = userPrincipal?.getSite() ?: DEFAULT_SITE,
            compCd = userPrincipal?.compCd ?: DEFAULT_COMP_CD,
            systemMaterialIds = systemMaterialIds
        ) > 0
    }
}

data class MaterialResponseModel(
    val systemMaterialId: String? = null,
    val userMaterialId: String? = null,
    val materialType: String? = null,
    val materialCategory: String? = null,
    val materialName: String? = null,
    val materialStandard: String? = null,
    val unit: String? = null,
    val minQuantity: Int? = null,
    val maxQuantity: Int? = null,
    val baseQuantity: Int? = null,
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