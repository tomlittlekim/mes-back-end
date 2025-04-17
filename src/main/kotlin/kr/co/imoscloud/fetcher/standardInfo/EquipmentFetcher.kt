package kr.co.imoscloud.fetcher.standardInfo

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.standardInfo.EquipmentResponseModel
import kr.co.imoscloud.service.standardInfo.EquipmentService

@DgsComponent
class EquipmentFetcher(
    private val equipmentService: EquipmentService,
) {
    @DgsQuery
    fun getEquipments(@InputArgument("filter") filter:EquipmentFilter): List<EquipmentResponseModel?> {
        return equipmentService.getEquipments(filter)
    }

    @DgsMutation
    fun saveEquipment(
        @InputArgument("createdRows") createdRows: List<EquipmentInput?>,
        @InputArgument("updatedRows") updatedRows:List<EquipmentUpdate?>
    ): Boolean{
        equipmentService.saveEquipment(createdRows, updatedRows)
        return true
    }

    @DgsMutation
    fun deleteEquipment(@InputArgument("equipmentId") equipmentId: String): Boolean{
        return equipmentService.deleteEquipment(equipmentId)
    }

}

data class EquipmentFilter(
    val factoryId: String,
    val factoryName: String,
    val lineId: String,
    val lineName: String,
    val equipmentId: String,
    val equipmentName: String,
    val equipmentSn: String,
    val equipmentType: String,
//    val flagActive: String ?= null
)

data class EquipmentInput(
    val factoryId: String,
    val lineId: String,
    val equipmentBuyDate: String,
    val equipmentBuyVendor: String,
    val equipmentSn: String,
    val equipmentType: String,
    val equipmentName: String,
    val equipmentStatus: String,
    val remark: String?="",
)

data class EquipmentUpdate(
    val factoryId: String,
    val lineId: String,
    val equipmentId: String,
    val equipmentBuyDate: String?,
    val equipmentBuyVendor: String?,
    val equipmentSn: String,
    val equipmentType: String,
    val equipmentName: String,
    val equipmentStatus: String,
    val remark: String?="",
)