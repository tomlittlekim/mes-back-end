package kr.co.imoscloud.repository.drive

import kr.co.imoscloud.entity.drive.FileManagement
import org.springframework.data.jpa.repository.JpaRepository

interface DriveRepository: JpaRepository<FileManagement, Long>{

    fun existsByNameAndExtensionAndMenuIdAndFlagActiveIsTrue(
        name: String,
        extension: String,
        menuId: String
    ): Boolean

    fun findAllByIdInAndFlagActiveIsTrue(ids: List<Long>): List<FileManagement>
}