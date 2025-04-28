package kr.co.imoscloud.repository.drive

import kr.co.imoscloud.entity.drive.FileManagement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface DriveRepository: JpaRepository<FileManagement, Long>{

    fun existsByNameAndExtensionAndMenuIdAndFlagActiveIsTrue(
        name: String,
        extension: String,
        menuId: String
    ): Boolean

    fun findAllByIdInAndFlagActiveIsTrue(ids: List<Long>): List<FileManagement>
}