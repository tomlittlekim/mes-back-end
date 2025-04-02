package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Line
import kr.co.imoscloud.fetcher.standardInfo.LineFilter
import kr.co.imoscloud.fetcher.standardInfo.LineInput
import kr.co.imoscloud.fetcher.standardInfo.LineUpdate
import kr.co.imoscloud.repository.LineRep
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class LineService(
    val lineRep: LineRep
)
{
    fun getLines(filter: LineFilter): List<LineResponseModel?> {
        return lineRep.getLines(
            site = "imos",
            compCd = "eightPin",
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            factoryCode = filter.factoryCode,
            lineId = filter.lineId,
            lineName = filter.lineName,
            flagActive = filter.flagActive?.let { it == "Y" }
        )
    }

    @Transactional
    fun saveLine( createdRows:List<LineInput?>,updatedRows:List<LineUpdate?>){
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createLine(it)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateLine(it)}
    }

    fun createLine(createdRows:List<LineInput>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val lineList = createdRows.map{
            Line(
                site = "imos",
                compCd = "eightPin",
                factoryId = it.factoryId,
                lineId = "L" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                lineName = it.lineName,
                lineDesc = it.lineDesc,
                flagActive = it.flagActive.equals("Y" )
            )
        }

        lineRep.saveAll(lineList)
    }

    fun updateLine(updatedRows:List<LineUpdate>) {
        val lineListIds = updatedRows.map {
            it.lineId
        }

        val lineList = lineRep.getLineListByIds(
            site = "imos",
            compCd = "eightPin",
            lineIds = lineListIds
        )

        val updateList = lineList.associateBy { it?.lineId }

        updatedRows.forEach{ x->
            val lineId = x.lineId
            val line = updateList[lineId]

            line?.let{
                it.factoryId = x.factoryId
                it.lineName = x.lineName
                it.lineDesc = x.lineDesc
                it.flagActive = x.flagActive.equals("Y" )
            }
        }

        lineRep.saveAll(lineList)
    }

    fun deleteLine(lineId:String):Boolean {
        return lineRep.deleteByLineId(
            site = "imos",
            compCd = "eightPin",
            lineId = lineId
        ) > 0
    }

    fun getLineOptions(): List<Line?> {
        return lineRep.getLineOptions(
            site = "imos",
            compCd = "eightPin"
        )
    }

}

data class LineResponseModel(
    val factoryId: String?,
    val factoryName: String?,
    val factoryCode: String?,
    val lineId: String?,
    val lineName: String?,
    val lineDesc: String?,
    val flagActive: String? = null,
    val createUser: String?,
    val createDate: LocalDate?,
    val updateUser: String?,
    val updateDate: LocalDate?
)