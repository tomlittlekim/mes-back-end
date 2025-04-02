package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Equipment
import kr.co.imoscloud.entity.standardInfo.Line
import kr.co.imoscloud.fetcher.standardInfo.EquipmentFilter
import kr.co.imoscloud.fetcher.standardInfo.EquipmentInput
import kr.co.imoscloud.fetcher.standardInfo.EquipmentUpdate
import kr.co.imoscloud.repository.EquipmentRep
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class EquipmentService(
    val equipmentRep: EquipmentRep
) {

    fun getEquipments(filter: EquipmentFilter): List<EquipmentResponseModel?> {
        return equipmentRep.getEquipments(
            site = "imos",
            compCd = "eightPin",
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
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createEquipment(it)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateEquipment(it)}
    }

    private fun createEquipment(createdRows: List<EquipmentInput>) {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val equipmentList = createdRows.map{
            Equipment(
                site = "imos",
                compCd = "eightPin",
                equipmentId = "E" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                factoryId = it.factoryId,
                lineId = it.lineId,
                equipmentSn = it.equipmentSn,
                equipmentType = it.equipmentType,
                equipmentName = it.equipmentName,
                equipmentStatus = it.equipmentStatus,
                flagActive = it.flagActive.equals("Y" )
            )
        }

        equipmentRep.saveAll(equipmentList)
    }

    private fun updateEquipment(updatedRows: List<EquipmentUpdate>) {
        val equipmentListIds = updatedRows.map {
            it.equipmentId
        }

        val equipmentList = equipmentRep.getEquipmentListByIds(
            site = "imos",
            compCd = "eightPin",
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
            }
        }

        equipmentRep.saveAll(equipmentList)
    }

    fun deleteEquipment(equipmentId: String):Boolean {
        return equipmentRep.deleteByEquipmentId(
            site = "imos",
            compCd = "eightPin",
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
    val createDate: LocalDate?,
    val updateUser: String?,
    val updateDate: LocalDate?
)