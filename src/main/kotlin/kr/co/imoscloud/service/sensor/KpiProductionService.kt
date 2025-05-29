package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.fetcher.sensor.KpiFilter
import kr.co.imoscloud.repository.productionmanagement.ProductionRateDayRep
import kr.co.imoscloud.repository.productionmanagement.ProductionRateHourRep
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.util.KpiUtils.dateFormatter
import kr.co.imoscloud.util.KpiUtils.fillGroupData
import kr.co.imoscloud.util.KpiUtils.getDateRange
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class KpiProductionService(
    val productionRateDayRep: ProductionRateDayRep,
    val productionRateHourRep: ProductionRateHourRep,
    val productionResultRep: ProductionResultRepository,
) {
    /**
     * 제품 불량률 구하기
     * */
    fun getProductionDefectRate(filter: KpiFilter): List<ChartResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        val site = userPrincipal.getSite()
        val compCd = userPrincipal.compCd
        val companyName = userPrincipal.companyName ?: "UNKNOWN"
        val (startDate, endDate) = getDateRange(filter)

        return when (filter.range) {
            CoreEnum.DateRangeType.WEEK.value, CoreEnum.DateRangeType.MONTH.value -> {
                val entity = productionResultRep.findDayDefectRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    generateSequence(startDate.toLocalDate()) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(endDate.toLocalDate().minusDays(1)) }
                        .asIterable()
                ) { it.format(dateFormatter) }
            }
            else -> {
                val entity = productionResultRep.findHourlyDefectRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    0..23
                ) { it.toString().padStart(2, '0') }
            }
        }
    }

    /**
     * 제품 생산률 구하기
     * */
    fun getProductionYieldRate(filter: KpiFilter): List<ChartResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        val site = userPrincipal.getSite()
        val compCd = userPrincipal.compCd
        val companyName = userPrincipal.companyName ?: "UNKNOWN"
        val (startDate, endDate) = getDateRange(filter)

        return when (filter.range) {
            CoreEnum.DateRangeType.WEEK.value, CoreEnum.DateRangeType.MONTH.value -> {
                val entity = productionRateDayRep.findDayProductionYieldRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    generateSequence(startDate.minusDays(1).toLocalDate()) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(endDate.toLocalDate().minusDays(2)) }
                        .asIterable()
                ) { it.format(dateFormatter) }
            }
            else -> {
                val entity = productionRateHourRep.findHourProductionYieldRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    0..23
                ) { it.toString().padStart(2, '0') }
            }
        }
    }
}