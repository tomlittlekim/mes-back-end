package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Equipment
import kr.co.imoscloud.fetcher.standardInfo.EquipmentFilter
import kr.co.imoscloud.fetcher.standardInfo.EquipmentInput
import kr.co.imoscloud.fetcher.standardInfo.EquipmentUpdate
import kr.co.imoscloud.repository.EquipmentRep
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class EquipmentService(
    val equipmentRep: EquipmentRep
) {

    fun getEquipments(filter: EquipmentFilter): List<EquipmentResponseModel?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return equipmentRep.getEquipments(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            lineId = filter.lineId,
            lineName = filter.lineName,
            equipmentId = filter.equipmentId,
            equipmentName = filter.equipmentName,
            equipmentSn = filter.equipmentSn,
            equipmentType = filter.equipmentType,
            flagActive = filter.flagActive?.let{ it == "Y" }
        )
    }

    @Transactional
    fun saveEquipment(createdRows: List<EquipmentInput?>, updatedRows: List<EquipmentUpdate?>) {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createEquipment(it, userPrincipal)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateEquipment(it, userPrincipal)}
    }

    private fun createEquipment(createdRows: List<EquipmentInput>, userPrincipal: UserPrincipal) {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val equipmentList = createdRows.map{
            Equipment(
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                equipmentId = "E" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                factoryId = it.factoryId,
                lineId = it.lineId,
                equipmentSn = it.equipmentSn,
                equipmentType = it.equipmentType,
                equipmentName = it.equipmentName,
                equipmentStatus = it.equipmentStatus,
            ).apply {
                flagActive = it.flagActive.equals("Y" )
                createCommonCol(userPrincipal)
            }
        }

        equipmentRep.saveAll(equipmentList)
    }

    private fun updateEquipment(updatedRows: List<EquipmentUpdate>, userPrincipal: UserPrincipal) {
        val equipmentListIds = updatedRows.map {
            it.equipmentId
        }

        val equipmentList = equipmentRep.getEquipmentListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            equipmentIds = equipmentListIds
        )

        val updateList = equipmentList.associateBy { it?.equipmentId }

        updatedRows.forEach{ x->
            val equipmentId = x.equipmentId
            val equipment = updateList[equipmentId]

            equipment?.let{
                it.factoryId = x.factoryId
                it.equipmentId = x.equipmentId
                it.equipmentSn = x.equipmentSn
                it.equipmentType = x.equipmentType
                it.equipmentName = x.equipmentName
                it.equipmentStatus = x.equipmentStatus
                it.flagActive = x.flagActive.equals("Y" )
                it.updateCommonCol(userPrincipal)
            }
        }

        equipmentRep.saveAll(equipmentList)
    }

    fun deleteEquipment(equipmentId: String):Boolean {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return equipmentRep.deleteByEquipmentId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            equipmentId = equipmentId
        ) > 0
    }

}

data class EquipmentResponseModel(
    val factoryId: String?,
    val factoryName: String?,
    val lineId: String?,
    val lineName: String?,
    val equipmentId: String?,
    val equipmentSn: String?,
    val equipmentType: String?,
    val equipmentName: String?,
    val equipmentStatus: String?,
    val flagActive: String? = null,
    val createUser: String?,
    val createDate: LocalDateTime?,
    val updateUser: String?,
    val updateDate: LocalDateTime?
)