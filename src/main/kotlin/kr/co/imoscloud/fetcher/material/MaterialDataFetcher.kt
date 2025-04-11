package kr.co.imoscloud.fetcher.material


import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.material.MaterialResponseModel
import kr.co.imoscloud.service.material.MaterialService
import kr.co.imoscloud.service.material.MaterialTypeGroupResponseModel

@DgsComponent
class MaterialDataFetcher(
    private val materialService: MaterialService
) {
    @DgsQuery
    fun getRawSubMaterials(@InputArgument filter: MaterialFilter): List<MaterialResponseModel?> {
        return materialService.getRawSubMaterials(filter)
    }

    @DgsQuery
    fun getCompleteMaterials(@InputArgument filter: MaterialFilter): List<MaterialResponseModel?> {
        return materialService.getCompleteMaterials(filter)
    }

    @DgsQuery
    fun getHalfMaterials(@InputArgument filter: MaterialFilter): List<MaterialResponseModel?> {
        return materialService.getHalfMaterials(filter)
    }

    @DgsQuery
    fun getMaterialsByType(@InputArgument materialType: String): List<MaterialResponseModel?> {
        return materialService.getMaterialsByType(materialType)
    }

    @DgsMutation
    fun saveMaterials(
        @InputArgument("createdRows") createdRows: List<MaterialInput?>,
        @InputArgument("updatedRows") updatedRows:List<MaterialUpdate?>
    ): Boolean {
        materialService.saveMaterials(createdRows,updatedRows)
        return true
    }

    @DgsMutation
    fun deleteMaterials(@InputArgument systemMaterialIds: List<String>): Boolean {
        return materialService.deleteMaterials(systemMaterialIds)
    }

    //드롭다운용 코드 조회
    @DgsQuery
    fun getMaterialCode(): List<MaterialResponseModel?> {
        return materialService.getMaterialCode()
    }

    @DgsQuery
    fun getAllMaterials(): List<MaterialTypeGroupResponseModel> {
        return materialService.getAllMaterials()
    }
}

data class MaterialFilter(
    var materialType: String? = null,
    var userMaterialId: String? = null,
    var materialName: String? = null,
    var flagActive: String? = null,
    var fromDate: String? = null,
    var toDate: String? = null
)

data class MaterialInput(
    var materialType: String? = null,
    var materialCategory: String? = null,
    var userMaterialId: String,
    var materialName: String? = null,
    var materialStandard: String? = null,
    var unit: String? = null,
    var minQuantity: Int? = null,
    var maxQuantity: Int? = null,
    var baseQuantity: Int? = null,
    var manufacturerName: String? = null,
    var supplierId: String? = null,
    var materialStorage: String? = null,
    var flagActive: String? = null
)

data class MaterialUpdate(
    val systemMaterialId: String,
    var materialType: String? = null,
    var materialCategory: String? = null,
    var userMaterialId: String? = null,
    var materialName: String? = null,
    var materialStandard: String? = null,
    var unit: String? = null,
    var minQuantity: Int? = null,
    var maxQuantity: Int? = null,
    var baseQuantity: Int? = null,
    var manufacturerName: String? = null,
    var supplierId: String? = null,
    var materialStorage: String? = null,
    var flagActive: String? = null
)
