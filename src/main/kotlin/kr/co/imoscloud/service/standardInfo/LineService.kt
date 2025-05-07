package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Line
import kr.co.imoscloud.fetcher.standardInfo.LineFilter
import kr.co.imoscloud.fetcher.standardInfo.LineInput
import kr.co.imoscloud.fetcher.standardInfo.LineUpdate
import kr.co.imoscloud.repository.LineRep
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class LineService(
    val lineRep: LineRep
)
{
    fun getLines(filter: LineFilter): List<LineResponseModel?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return lineRep.getLines(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            factoryCode = filter.factoryCode,
            lineId = filter.lineId,
            lineName = filter.lineName,
        )
    }

    @Transactional
    fun saveLine( createdRows:List<LineInput?>,updatedRows:List<LineUpdate?>){
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createLine(it, userPrincipal)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateLine(it, userPrincipal)}
    }

    fun createLine(createdRows:List<LineInput>, userPrincipal: UserPrincipal){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val lineList = createdRows.map{
            Line(
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                factoryId = it.factoryId,
                lineId = "L" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                lineName = it.lineName,
                lineDesc = it.lineDesc,
            ).apply {
                createCommonCol(userPrincipal)
            }
        }

        lineRep.saveAll(lineList)
    }

    fun updateLine(updatedRows:List<LineUpdate>, userPrincipal: UserPrincipal) {
        val lineListIds = updatedRows.map {
            it.lineId
        }

        val lineList = lineRep.getLineListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
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
                it.updateCommonCol(userPrincipal)
            }
        }

        lineRep.saveAll(lineList)
    }

    fun deleteLine(lineId:String):Boolean {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return lineRep.deleteByLineId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            lineId = lineId,
            updateUser = userPrincipal.loginId
        ) > 0
    }

    fun getLineOptions(): List<Line?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return lineRep.getLineOptions(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd
        )
    }

}

data class LineResponseModel(
    val factoryId: String?,
    val factoryName: String?,
//    val factoryCode: String?,
    val lineId: String?,
    val lineName: String?,
    val lineDesc: String?,
//    val flagActive: String? = null,
    val createUser: String?,
    val createDate: LocalDateTime?,
    val updateUser: String?,
    val updateDate: LocalDateTime?
)