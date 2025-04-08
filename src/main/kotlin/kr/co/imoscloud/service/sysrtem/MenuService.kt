package kr.co.imoscloud.service.sysrtem

import kr.co.imoscloud.dto.MenuRequest
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.repository.system.MenuRepository
import kr.co.imoscloud.util.AuthLevel
import org.springframework.stereotype.Service

@Service
class MenuService(
    private val menuRepo: MenuRepository
) {

    @AuthLevel(minLevel = 5)
    fun getMenus(): List<Menu> = menuRepo.findAll()

    @AuthLevel(minLevel = 5)
    fun upsertMenus(req: MenuRequest): String =
        try {
            val modifyMenu = req.id
                ?.let { id ->
                    menuRepo.findByIdAndFlagActiveIsTrue(id)
                        ?.let { menu -> menu.apply {
                            menuId = req.menuId ?: this.menuId
                            upMenuId = req.upMenuId ?: this.upMenuId
                            menuName = req.menuName ?: this.menuName
                            flagSubscribe = req.flagSubscribe ?: this.flagSubscribe
                            sequence = req.sequence ?: this.sequence
                            flagActive = req.flagActive
                        }  }
                        ?: throw IllegalArgumentException("수정하려는 대상이 존재하지 않습니다. ")
                }
                ?:run {  Menu(
                    menuId = req.menuId!!,
                    upMenuId = req.upMenuId!!,
                    menuName = req.menuName!!,
                    flagSubscribe = req.flagSubscribe!!,
                    flagActive = req.flagActive,
                    sequence = req.sequence!!
                ) }

            menuRepo.save(modifyMenu)
            "${modifyMenu.menuName} 메뉴 수정 성공"
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("신규 메뉴 생성 실패. : 매개변수 값을 비어있습니다. ")
        }
}