package kr.co.imoscloud.fetcher.material


import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.service.material.MaterialResponseModel
import kr.co.imoscloud.service.material.MaterialService

@DgsComponent
class MaterialDataFetcher(
    private val materialService: MaterialService
) {
    @DgsQuery
    fun materials(@InputArgument filter: MaterialFilter): List<MaterialResponseModel?> {
        return materialService.getMaterials(filter)
    }

    //TODO: CUD 아직 안 되어있음!!
    @DgsMutation
    fun saveMaterials(@InputArgument materials: List<MaterialMaster>): List<MaterialMaster> {
        return materialService.saveMaterials(materials)
    }

    @DgsMutation
    fun deleteMaterials(@InputArgument ids: List<String>): Boolean {
        return materialService.deleteMaterials(ids)
    }
}

data class MaterialFilter(
    var materialType: String,
    var systemMaterialId: String,
    var userMaterialId: String,
    var materialName: String,
    var flagActive: String? = null,
    var fromDate: String? = null,
    var toDate: String? = null
)

//data class MaterialInput(
//    인풋 선언하기
//)
//
//data class MaterialUpdate(
//    업데이트 선언
//)
//

