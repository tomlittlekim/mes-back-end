package kr.co.imoscloud.fetcher

import kr.co.imoscloud.entity.Material
import kr.co.imoscloud.entity.MaterialFilter
import kr.co.imoscloud.service.MaterialResponseModel
import kr.co.imoscloud.service.MaterialService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class MaterialDataFetcher(
    private val materialService: MaterialService
) {
    @DgsQuery
    fun materials(@InputArgument filter: MaterialFilter): List<MaterialResponseModel> {
        return materialService.getMaterials(filter)
    }

    @DgsMutation
    fun saveMaterials(@InputArgument materials: List<Material>): List<Material> {
        return materialService.saveMaterials(materials)
    }

    @DgsMutation
    fun deleteMaterials(@InputArgument ids: List<String>): Boolean {
        return materialService.deleteMaterials(ids)
    }
} 