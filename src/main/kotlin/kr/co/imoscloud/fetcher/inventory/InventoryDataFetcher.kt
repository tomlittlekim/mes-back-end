package kr.co.imoscloud.fetcher.inventory

import kr.co.imoscloud.entity.Inventory.InventoryInMFilter
import kr.co.imoscloud.service.InventoryInMResponseModel
import kr.co.imoscloud.service.InventoryService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class InventoryDataFetcher(
    val inventoryService: InventoryService,
) {
    @DgsQuery
    fun getInventoryList(@InputArgument("filter") filter: InventoryInMFilter): List<InventoryInMResponseModel?> {
        return inventoryService.getInventoryList(filter)
    }
    
    @DgsQuery
    fun testString(): String {
        println("테스트 쿼리 호출됨")
        return "테스트가 성공했습니다!"
    }
}
