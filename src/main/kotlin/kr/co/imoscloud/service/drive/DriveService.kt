package kr.co.imoscloud.service.drive

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.entity.drive.FileManagement
import kr.co.imoscloud.iface.IDrive
import kr.co.imoscloud.repository.drive.DriveRepository
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileOutputStream

@Service
class DriveService(
    private val driveRepo : DriveRepository,
): IDrive {

    @AuthLevel(minLevel = 5)
    fun getFiles(): List<FileManagement> = driveRepo.findAll()

    @AuthLevel(minLevel = 5)
    fun deleteFile(id: Long): String {
        driveRepo.deleteById(id)
        return "삭제 성공"
    }

    @AuthLevel(minLevel = 5)
    fun addFile(req: LabelExcelRequest): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val (name, extension) = getNameWithExtension(req.file.originalFilename)

        if (driveRepo.existsByNameAndExtensionAndMenuIdAndFlagActiveIsTrue(name, extension, req.menuId))
            throw IllegalArgumentException("동일한 이름과 확장자 파일이 존재합니다. ")

        val file = generateEntityUsingFile(req.file, loginUser)
        val filePath = getSavePath(file)

        FileOutputStream(filePath).write(req.file.bytes)
        excelToFods(filePath)
        File(filePath).delete()
        driveRepo.save(file.apply { this.extension = "fods" })

        return "저장 성공"
    }

    private fun generateEntityUsingFile(
        file: MultipartFile,
        loginUser: UserPrincipal,
    ): FileManagement {
        val nameAndExtension = getNameWithExtension(file.originalFilename)

        val fileEntity = FileManagement(
            name = nameAndExtension.first,
            extension = nameAndExtension.second,
            path = CoreEnum.DrivePath.HOME_PATH.value,
            size = (file.size / 1024).toInt(),
        ).apply { createCommonCol(loginUser) }

        try {
            FileOutputStream(getSavePath(fileEntity)).use { outputStream -> outputStream.write(file.bytes) }
        } catch (e: Exception) {
            throw IllegalArgumentException("파일 저장 중 오류가 발생했습니다: ${e.message}")
        }; return fileEntity
    }

    private fun excelToFods(savePath: String) {
        val libreOfficePath = getLibreOfficeExecutable()
        val process = ProcessBuilder(
            libreOfficePath,
            "--headless",
            "--convert-to",
            "fods",
            savePath,
            "--outdir",
            CoreEnum.DrivePath.HOME_PATH.value
        ).start()
        process.waitFor()
    }
}

data class LabelExcelRequest(
    val menuId: String,
    val file: MultipartFile
)