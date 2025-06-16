package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.exception.company.CompanyNotFoundException
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.KpiFilter
import kr.co.imoscloud.repository.productionmanagement.ProductionRateDayRep
import kr.co.imoscloud.repository.productionmanagement.ProductionRateHourRep
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.util.KpiUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

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
        val companyName = userPrincipal.companyName ?: throw CompanyNotFoundException()
        val (startDate, endDate) = KpiUtils.getDateRange(filter)

        return when (filter.range) {
            CoreEnum.DateRangeType.WEEK.value, CoreEnum.DateRangeType.MONTH.value -> {
                val entity = productionResultRep.findDayDefectRates(site, compCd, startDate, endDate)
                KpiUtils.fillGroupData(
                    entity, companyName,
                    generateSequence(startDate.toLocalDate()) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(endDate.toLocalDate().minusDays(1)) }
                        .asIterable()
                ) { it.format(KpiUtils.dateFormatter) }
            }
            else -> {
                val entity = productionResultRep.findHourlyDefectRates(site, compCd, startDate, endDate)
                KpiUtils.fillGroupData(
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
        val companyName = userPrincipal.companyName ?: throw CompanyNotFoundException()
        val (startDate, endDate) = KpiUtils.getDateRange(filter)

        return when (filter.range) {
            CoreEnum.DateRangeType.WEEK.value, CoreEnum.DateRangeType.MONTH.value -> {
                val entity = productionRateDayRep.findDayProductionYieldRates(site, compCd, startDate, endDate)
                KpiUtils.fillGroupData(
                    entity, companyName,
                    generateSequence(startDate.minusDays(1).toLocalDate()) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(endDate.toLocalDate().minusDays(2)) }
                        .asIterable()
                ) { it.format(KpiUtils.dateFormatter) }
            }
            else -> {
                val entity = productionRateHourRep.findHourProductionYieldRates(site, compCd, startDate, endDate)
                KpiUtils.fillGroupData(
                    entity, companyName,
                    0..23
                ) { it.toString().padStart(2, '0') }
            }
        }
    }

    /**
     * 제품 불량률 데이터 조회
     */
    fun getProductionDefectRateDummy(filter: KpiFilter): List<ChartResponseModel> {
        val localDate = LocalDate.parse(filter.date, KpiUtils.dateFormatter)
        val params = KpiUtils.getParams(filter.range)
        
        // 더미 데이터 생성 (실제 구현에서는 DB에서 데이터를 가져옴)
        val timeLabels = generateTimeLabels(filter.date, filter.range)
        return timeLabels.map { timeLabel ->
            ChartResponseModel(
                timeLabel = timeLabel,
                label = "에잇핀",
                value = Random.nextDouble(0.0, 0.07) // 0~7% 사이의 불량률
            )
        }
    }
    
    /**
     * 제품 생산률 데이터 조회
     */
    fun getProductionYieldRateDummy(filter: KpiFilter): List<ChartResponseModel> {
        val localDate = LocalDate.parse(filter.date, KpiUtils.dateFormatter)
        val params = KpiUtils.getParams(filter.range)
        
        // 더미 데이터 생성 (실제 구현에서는 DB에서 데이터를 가져옴)
        val timeLabels = generateTimeLabels(filter.date, filter.range)
        return timeLabels.map { timeLabel ->
            ChartResponseModel(
                timeLabel = timeLabel,
                label = "생산률",
                value = Random.nextDouble(75.0, 98.0) // 75~98% 사이의 생산률
            )
        }
    }
    
    /**
     * 시간 라벨 생성
     */
    private fun generateTimeLabels(date: String, range: String): List<String> {
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val targetDate = LocalDate.parse(date, format)
        
        return when (range.lowercase()) {
            "day" -> (0..23).map { "$it:00" }
            "week" -> (0..6).map { 
                targetDate.minusDays(it.toLong()).format(format) 
            }.reversed()
            "month" -> (0..29).map { 
                targetDate.minusDays(it.toLong()).format(format) 
            }.reversed()
            else -> listOf(date)
        }
    }
}