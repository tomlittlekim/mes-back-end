package kr.co.imoscloud.service.material

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.material.Bom
import kr.co.imoscloud.entity.material.BomDetail
import kr.co.imoscloud.fetcher.material.*
import kr.co.imoscloud.model.material.BomDetailMaterialDto
import kr.co.imoscloud.model.material.BomMaterialDto
import kr.co.imoscloud.repository.material.BomDetailRepository
import kr.co.imoscloud.repository.material.BomRepository
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BomService(
    private val bomRepository: BomRepository,
    private val bomDetailRepository: BomDetailRepository
) {

    private fun getCurrentUser() = SecurityUtils.getCurrentUserPrincipal()

    // 좌측 그리드 호출
    fun getBomList(filter: BomFilter): List<BomResponseModel> {
        val userPrincipal = getCurrentUser()
        val bomList = bomRepository.getBomList(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            materialType = filter.materialType,
            materialName = filter.materialName,
            bomName = filter.bomName
//            flagActive = filter.flagActive?.let { it == "Y" },
        )

        return entityToResponse(bomList)
    }

    // 우측 그리드 호출
    fun getBomDetail(bomId: String): List<BomDetailResponseModel> {
        val userPrincipal = getCurrentUser()
        val bomDetailList = bomDetailRepository.getBomDetailListByBomId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            bomId = bomId
        )

        return entityToDetailResponse(bomDetailList)
    }

    // 좌측 그리드 저장(등록/수정) -> 화면 상에서는 팝업창으로 처리하는 부분이나 편의상 행추가/수정과 동일하게 처리
    @Transactional
    fun saveBom(createdRows: List<BomInput?>, updatedRows: List<BomUpdate?>) {
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { createBom(it) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { updateBom(it) }
    }

    // 우측 그리드 저장(행추가, 수정) -> 화면 상으로 그리드인 부분
    @Transactional
    fun saveBomDetails(createdRows: List<BomDetailInput?>, updatedRows: List<BomDetailUpdate?>) {
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { createBomDetails(it) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { updateBomDetails(it) }
    }

    fun createBom(createdRows: List<BomInput?>) {
        val userPrincipal = getCurrentUser()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val bomList = createdRows.map {
            Bom(
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                bomId = it?.bomId ?: ("BOM" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3)),
                bomLevel = it?.bomLevel ?: 1,
                itemCd = it?.systemMaterialId!!,
                bomName = it.bomName,
                remark = it.remark
            ).apply {
                flagActive = true
                createCommonCol(userPrincipal)
            }
        }

        bomRepository.saveAll(bomList)
    }

    fun updateBom(updatedRows: List<BomUpdate?>) {
        val userPrincipal = getCurrentUser()
        val bomIds = updatedRows.mapNotNull { it?.bomId }

        val bomList = bomRepository.getBomByBomId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            bomIds = bomIds
        )

        val updateList = bomList.associateBy { it.bom.bomId }

        updatedRows.forEach { x ->
            val bomId = x?.bomId
            val bom = updateList[bomId]

            bom?.let {
                it.bom.bomName = x?.bomName
                it.bom.remark = x?.remark
                it.bom.updateCommonCol(userPrincipal)
            }
        }

        bomRepository.saveAll(bomList.map { it.bom })
    }

    // 좌측 그리드 삭제 - 삭제 시 우측 그리드에 bomId로 연관되어 조회되는 부분까지 모두 삭제되어야 함(Soft Delete)
    @Transactional
    fun deleteBom(bomId: String): Boolean {
        val userPrincipal = getCurrentUser()
        
        // Bom의 flagActive를 false로 업데이트하고 updateCommonCol 처리
        val bom = bomRepository.findBomByBomId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            bomId = bomId
        ) ?: return false

        bom.apply {
            flagActive = false
            updateCommonCol(userPrincipal)
        }
        bomRepository.save(bom)

        // 연관된 BomDetail들의 flagActive를 false로 업데이트하고 updateCommonCol 처리
        val bomDetails = bomDetailRepository.getBomDetailListByBomId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            bomId = bomId
        ).map { it.bomDetail }

        bomDetails.forEach { detail ->
            detail.flagActive = false
            detail.updateCommonCol(userPrincipal)
        }
        bomDetailRepository.saveAll(bomDetails)

        return true
    }

    fun createBomDetails(createdRows: List<BomDetailInput?>) {
        val userPrincipal = getCurrentUser()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val bomDetailList = createdRows.map {
            BomDetail(
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                bomId = it?.bomId!!,
                bomDetailId = it.bomDetailId ?: ("DET" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3)),
                bomLevel = it.bomLevel,
                itemCd = it.systemMaterialId,
                parentItemCd = it.parentItemCd,
                itemQty = it.itemQty,
                remark = it.remark
            ).apply {
                flagActive = true
                createCommonCol(userPrincipal)
            }
        }

        bomDetailRepository.saveAll(bomDetailList)
    }

    fun updateBomDetails(updatedRows: List<BomDetailUpdate?>) {
        val userPrincipal = getCurrentUser()
        val bomDetailIds = updatedRows.mapNotNull { it?.bomDetailId }

        val bomDetailList = bomDetailRepository.getBomDetailListByBomDetailIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            bomDetailIds = bomDetailIds
        )

        val updateList = bomDetailList.associateBy { it.bomDetail.bomDetailId }

        updatedRows.forEach { x ->
            val bomDetailId = x?.bomDetailId
            val bomDetail = updateList[bomDetailId]

            bomDetail?.let {
                it.bomDetail.bomLevel = x?.bomLevel
                it.bomDetail.itemCd = x?.systemMaterialId
                it.bomDetail.parentItemCd = x?.parentItemCd
                it.bomDetail.itemQty = x?.itemQty
                it.bomDetail.remark = x?.remark
                it.bomDetail.updateCommonCol(userPrincipal)
            }
        }

        bomDetailRepository.saveAll(bomDetailList.map { it.bomDetail })
    }

    // 우측 그리드 삭제 - 기존과 동일하게 행 삭제
    @Transactional
    fun deleteBomDetails(bomDetailIds: List<String>): Boolean {
        val userPrincipal = getCurrentUser()

        val bomDetails = bomDetailRepository.getBomDetailListByBomDetailIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            bomDetailIds = bomDetailIds
        ).map { it.bomDetail }

        if (bomDetails.isEmpty()) return false

        bomDetails.forEach { detail ->
            detail.flagActive = false
            detail.updateCommonCol(userPrincipal)
        }

        bomDetailRepository.saveAll(bomDetails)

        return true
    }

    private fun entityToResponse(dtos: List<BomMaterialDto>): List<BomResponseModel> {
        return dtos.map { dto ->
            BomResponseModel(
                bomId = dto.bom.bomId!!,
                bomLevel = dto.bom.bomLevel,
                bomName = dto.bom.bomName,
                materialType = dto.materialType,
                materialCategory = dto.materialCategory,
                systemMaterialId = dto.bom.itemCd!!,
                userMaterialId = dto.userMaterialId,
                materialName = dto.materialName,
                materialStandard = dto.materialStandard,
                unit = dto.unit,
                remark = dto.bom.remark,
                flagActive = if (dto.bom.flagActive) "Y" else "N",
                createUser = dto.bom.createUser,
                createDate = DateUtils.formatLocalDateTime(dto.bom.createDate),
                updateUser = dto.bom.updateUser,
                updateDate = DateUtils.formatLocalDateTime(dto.bom.updateDate),
            )
        }
    }

    private fun entityToDetailResponse(dtos: List<BomDetailMaterialDto>): List<BomDetailResponseModel> {
        return dtos.map { dto ->
            BomDetailResponseModel(
                bomId = dto.bomDetail.bomId!!,
                bomDetailId = dto.bomDetail.bomDetailId!!,
                bomLevel = dto.bomDetail.bomLevel!!,
                materialType = dto.materialType,
                materialCategory = dto.materialCategory,
                systemMaterialId = dto.bomDetail.itemCd!!,
                userMaterialId = dto.userMaterialId,
                materialName = dto.materialName,
                parentItemCd = dto.bomDetail.parentItemCd,
                userParentItemCd = dto.userParentItemCd,
                parentMaterialType = dto.parentMaterialType,
                parentMaterialName = dto.parentMaterialName,
                materialStandard = dto.materialStandard,
                unit = dto.unit,
                itemQty = dto.bomDetail.itemQty,
                remark = dto.bomDetail.remark,
                flagActive = if (dto.bomDetail.flagActive) "Y" else "N",
                createUser = dto.bomDetail.createUser,
                createDate = DateUtils.formatLocalDateTime(dto.bomDetail.createDate),
                updateUser = dto.bomDetail.updateUser,
                updateDate = DateUtils.formatLocalDateTime(dto.bomDetail.updateDate),
            )
        }
    }
}

data class BomResponseModel(
    val bomId: String,
    val bomLevel: Int,
    val bomName: String?,
    val materialType: String?,
    val materialCategory: String?,
    val systemMaterialId: String,
    val userMaterialId: String?,
    val materialName: String?,
    val materialStandard: String?,
    val unit: String?,
    val remark: String?,
    val flagActive: String,
    val createUser: String?,
    val createDate: String?,
    val updateUser: String?,
    val updateDate: String?
)

data class BomDetailResponseModel(
    val bomId: String,
    val bomDetailId: String,
    val bomLevel: Int,
    val materialType: String?,
    val materialCategory: String?,
    val systemMaterialId: String,
    val userMaterialId: String?,
    val materialName: String?,
    val parentItemCd: String?,
    val userParentItemCd: String?,
    val parentMaterialType: String?,
    val parentMaterialName: String?,
    val materialStandard: String?,
    val unit: String?,
    val itemQty: Double?,
    val remark: String?,
    val flagActive: String,
    val createUser: String?,
    val createDate: String?,
    val updateUser: String?,
    val updateDate: String?
)