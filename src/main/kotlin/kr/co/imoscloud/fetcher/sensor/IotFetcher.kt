package kr.co.imoscloud.fetcher.sensor

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.KpiFilter
import kr.co.imoscloud.service.sensor.*

/**
 * IoT 및 KPI 데이터 조회를 위한 GraphQL Fetcher
 */
@DgsComponent
class IotFetcher(
    private val equipmentPowerService: EquipmentPowerService,
    private val equipmentOperationService: EquipmentOperationService,
    private val kpiProductionService: KpiProductionService
) {
    /**
     * 실시간 전력 데이터 조회
     */
    @DgsQuery
    fun getPowerData(): List<ChartResponseModel?> {
        return equipmentPowerService.getRealPowerData()
    }

    /**
     * 전력 상세 데이터 조회
     */
    @DgsQuery
    fun getPopupPowerData(@InputArgument("filter") filter: KpiFilter): List<ChartResponseModel> {
        return equipmentPowerService.getPopupPowerData(filter)
    }

    /**
     * 설비 가동률 데이터 조회
     */
    @DgsQuery
    fun getEquipmentOperationData(@InputArgument("filter") filter: KpiFilter): List<ChartResponseModel> {
        return equipmentOperationService.getEquipmentOperationData(filter)
    }

    /**
     * 제품 불량률 데이터 조회
     */
    @DgsQuery
    fun getProductDefect(@InputArgument("filter") filter: KpiFilter): List<ChartResponseModel> {
        return kpiProductionService.getProductionDefectRate(filter)
    }

    /**
     * 제품 생산률 데이터 조회
     */
    @DgsQuery
    fun getProductionYieldRate(@InputArgument("filter") filter: KpiFilter): List<ChartResponseModel> {
        return kpiProductionService.getProductionYieldRate(filter)
    }
}