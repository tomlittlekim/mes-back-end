package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.entity.productionmanagement.ProductionRateDay
import kr.co.imoscloud.entity.productionmanagement.ProductionRateHour
import kr.co.imoscloud.repository.productionmanagement.ProductionRateDayRep
import kr.co.imoscloud.repository.productionmanagement.ProductionRateHourRep
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class KpiBatchSchedulerLauncher(
    private val productionRateDayRep: ProductionRateDayRep,
    private val productionRateHourRep: ProductionRateHourRep,
    private val productionResultRep: ProductionResultRepository,
) {
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "ProductionRateHourTask", lockAtMostFor = "2m", lockAtLeastFor = "30s")
    fun batchSaveProductionYieldHourRate() {
        saveProductionYieldRate(productionRateHourRep){ row, now, user ->
            ProductionRateHour(
                site = row.site,
                compCd = row.compCd,
                planSum = row.planSum,
                workOrderSum = row.workOrderSum,
                notWorkOrderSum = row.notWorkOrderSum,
                productionRate = row.productionRate,
                aggregationTime = now,
                createUser = user,
                createDate = now,
                updateUser = user,
                updateDate = now
            )
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "ProductionRateDayTask", lockAtMostFor = "2m", lockAtLeastFor = "30s")
    fun batchSaveProductionYieldDayRate() {
        saveProductionYieldRate(productionRateDayRep){ row, now, user ->
            ProductionRateDay(
                site = row.site,
                compCd = row.compCd,
                planSum = row.planSum,
                workOrderSum = row.workOrderSum,
                notWorkOrderSum = row.notWorkOrderSum,
                productionRate = row.productionRate,
                aggregationTime = now,
                createUser = user,
                createDate = now,
                updateUser = user,
                updateDate = now
            )
        }
    }

    private fun <T> saveProductionYieldRate(
        repository: CrudRepository<T, *>,
        entityMapper: (row: ProductionRateModel, now: LocalDateTime, user: String) -> T
    ) {
        val results = productionResultRep.findProductionYieldRate()
        val now = LocalDateTime.now()
        val user = "SYSTEM"
        val rows = results.map { row -> entityMapper(row, now, user) }
        repository.saveAll(rows)
    }
}