package kr.co.imoscloud.fetcher.material

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.material.BomDetailResponseModel
import kr.co.imoscloud.service.material.BomResponseModel
import kr.co.imoscloud.service.material.BomService

@DgsComponent
class BomDataFetcher(
    private val bomService: BomService
) {
    @DgsQuery
    fun getBomList(@InputArgument filter: BomFilter): List<BomResponseModel?> {
        return bomService.getBomList(filter)
    }

    @DgsQuery
    fun getBomDetails(@InputArgument bomId: String): List<BomDetailResponseModel?> {
        return bomService.getBomDetail(bomId)
    }

    @DgsMutation
    fun saveBom(
        @InputArgument("createdRows") createdRows: List<BomInput?>,
        @InputArgument("updatedRows") updatedRows:List<BomUpdate?>
    ): Boolean {
        bomService.saveBom(createdRows,updatedRows)
        return true
    }

    @DgsMutation
    fun saveBomDetails(
        @InputArgument("createdRows") createdRows: List<BomDetailInput?>,
        @InputArgument("updatedRows") updatedRows:List<BomDetailUpdate?>
    ): Boolean {
        bomService.saveBomDetails(createdRows,updatedRows)
        return true
    }

    @DgsMutation
    fun deleteBom(@InputArgument bomId: String): Boolean {
        return bomService.deleteBom(bomId)
    }

    @DgsMutation
    fun deleteBomDetails(@InputArgument bomDetailIds: List<String>): Boolean {
        return bomService.deleteBomDetails(bomDetailIds)
    }
}

data class BomFilter(
    var materialType: String? = null,
    var materialName: String? = null,
    var bomName: String? = null,
//    var flagActive: String? = null
)

data class BomInput(
    val bomLevel: Int? = 1, //1로 고정
    val materialType: String,
    val bomId: String? = null,
    val bomName: String,
    val systemMaterialId: String?, //itemCd와 동일
    val remark: String?,
//    val flagActive: String //데이터 생성 시에는 무조건 true
)

data class BomUpdate(
    val bomLevel: Int, //변경x
    val materialType: String, //변경x
    val bomId: String, //변경x
    val bomName: String, //변경O
    val systemMaterialId: String?, //itemCd와 동일, 변경x
    val remark: String?, //변경O
//    val flagActive: String //변경x
)

data class BomDetailInput(
    val bomId: String,
    val bomDetailId: String?, // 자동생성
    val bomLevel: Int,
    val systemMaterialId: String,
    val parentItemCd: String,
    val itemQty: Double,
    val remark: String?
)

data class BomDetailUpdate(
    val bomId: String,
    val bomDetailId: String,
    val bomLevel: Int,
    val systemMaterialId: String,
    val parentItemCd: String,
    val itemQty: Double,
    val remark: String?
)
