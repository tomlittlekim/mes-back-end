package kr.co.imoscloud.repository.productionmanagement

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.imoscloud.entity.material.QMaterialMaster
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.QProductionPlan
import kr.co.imoscloud.model.productionmanagement.ProductionPlanDTO
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ProductionPlanRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductionPlanRepositoryCustom, QuerydslRepositorySupport(ProductionPlan::class.java) {

    @Transactional(readOnly = true)
    override fun getProductionPlanList(
        site: String,
        compCd: String,
        prodPlanId: String?,
        orderId: String?,
        orderDetailId: String?,
        productId: String?,
        productName: String?,
        materialCategory: String?,
        shiftType: String?,
        planStartDateFrom: LocalDate?,
        planStartDateTo: LocalDate?,
        planEndDateFrom: LocalDate?,
        planEndDateTo: LocalDate?,
        flagActive: Boolean?
    ): List<ProductionPlanDTO> {
        val plan = QProductionPlan.productionPlan
        val material = QMaterialMaster.materialMaster

        // 기본 쿼리 구성 - DTO 프로젝션 사용하여 한번에 모든 필드 조회
        var query = queryFactory
            .select(Projections.constructor(
                ProductionPlanDTO::class.java,
                plan.id,
                plan.site,
                plan.compCd,
                plan.prodPlanId,
                plan.orderId,
                plan.orderDetailId,
                plan.productId,
                plan.shiftType,
                plan.planQty,
                plan.planStartDate,
                plan.planEndDate,
                plan.createDate,
                plan.createUser,
                plan.updateDate,
                plan.updateUser,
                plan.flagActive,
                material.materialName,
                material.materialCategory
            ))
            .from(plan)
            .leftJoin(material)
            .on(
                plan.site.eq(material.site),
                plan.compCd.eq(material.compCd),
                plan.productId.eq(material.systemMaterialId)
            )
            .where(
                plan.site.eq(site),
                plan.compCd.eq(compCd)
            )

        // prodPlanId 필터링
        prodPlanId?.let {
            if (it.isNotBlank()) {
                query = query.where(plan.prodPlanId.like("%$it%"))
            }
        }

        // orderId 필터링
        orderId?.let {
            if (it.isNotBlank()) {
                query = query.where(plan.orderId.like("%$it%"))
            }
        }

        // orderDetailId 필터링
        orderDetailId?.let {
            if (it.isNotBlank()) {
                query = query.where(plan.orderDetailId.like("%$it%"))
            }
        }

        // productId 필터링
        productId?.let {
            if (it.isNotBlank()) {
                query = query.where(plan.productId.like("%$it%"))
            }
        }

        // productName 필터링 (제품명 - MaterialMaster 테이블과 조인하여 필터링)
        productName?.let {
            if (it.isNotBlank()) {
                query = query.where(material.materialName.like("%$it%"))
            }
        }

        // materialCategory 필터링 (제품 유형 - MaterialMaster 테이블과 조인하여 필터링)
        materialCategory?.let {
            if (it.isNotBlank()) {
                query = query.where(material.materialCategory.like("%$it%"))
            }
        }

        // shiftType 필터링
        shiftType?.let {
            if (it.isNotBlank()) {
                query = query.where(plan.shiftType.eq(it))
            }
        }

        // planStartDateFrom 필터링 (계획시작일 범위 시작)
        planStartDateFrom?.let {
            val startOfDay = LocalDateTime.of(it, LocalTime.MIN)
            query = query.where(plan.planStartDate.goe(startOfDay))
        }

        // planStartDateTo 필터링 (계획시작일 범위 끝)
        planStartDateTo?.let {
            // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
            val nextDay = it.plusDays(1)
            val startOfNextDay = LocalDateTime.of(nextDay, LocalTime.MIN)
            query = query.where(plan.planStartDate.lt(startOfNextDay))
        }

        // planEndDateFrom 필터링 (계획종료일 범위 시작)
        planEndDateFrom?.let {
            val startOfDay = LocalDateTime.of(it, LocalTime.MIN)
            query = query.where(plan.planEndDate.goe(startOfDay))
        }

        // planEndDateTo 필터링 (계획종료일 범위 끝)
        planEndDateTo?.let {
            // 다음 날 자정(00:00:00)으로 설정하고 그 값보다 작은 값만 포함
            val nextDay = it.plusDays(1)
            val startOfNextDay = LocalDateTime.of(nextDay, LocalTime.MIN)
            query = query.where(plan.planEndDate.lt(startOfNextDay))
        }

        // flagActive 필터링 (기본값은 true)
        query = query.where(plan.flagActive.eq(flagActive ?: true))

        // seq 역순 정렬 추가
        query = query.orderBy(plan.id.desc())

        // 쿼리 실행 및 DTO 결과 반환
        return query.fetch()
    }
}