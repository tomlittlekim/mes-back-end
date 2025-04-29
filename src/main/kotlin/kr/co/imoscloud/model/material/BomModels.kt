package kr.co.imoscloud.model.material

import kr.co.imoscloud.entity.material.Bom
import kr.co.imoscloud.entity.material.BomDetail

/**
 * BOM 정보(좌측그리드) 호출 시 필요한 필드만 조회하는 dto
 */
data class BomMaterialDto(
    val bom: Bom,
    val materialType: String?,
    val materialCategory: String?,
    val userMaterialId: String?,
    val materialName: String?,
    val materialStandard: String?,
    val unit: String?
)

/**
 * BOM 디테일 정보(우측그리드) 호출 시 필요한 필드만 조회하는 dto
 */
data class BomDetailMaterialDto(
    val bomDetail: BomDetail,
    val materialType: String?,
    val materialCategory: String?,
    val userMaterialId: String?,
    val materialName: String?,
    val materialStandard: String?,
    val unit: String?,
    val userParentItemCd: String?,
    val parentMaterialType: String?,
    val parentMaterialName: String?
)