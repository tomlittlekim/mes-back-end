package kr.co.imoscloud.iface

import kr.co.imoscloud.entity.drive.FileManagement
import java.util.*

interface IDrive {

    fun getLibreOfficeExecutable(): String {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        return when {
            osName.contains("mac") -> "/Applications/LibreOffice.app/Contents/MacOS/soffice"
            osName.contains("win") -> "C:\\Program Files\\LibreOffice\\program\\soffice.exe"
            else -> "libreoffice"
        }
    }

    fun getNameWithExtension(originalFileName: String?): Pair<String, String> {
        originalFileName ?: throw IllegalArgumentException("잘못된 파일 형식 입니다.")

        val list = originalFileName.split("\\.".toRegex())
        val name = if(list.size > 2) list.subList(0, list.size - 1).joinToString(".") else list.first()
        return Pair(name, list.last())
    }

    fun getSavePath(file: FileManagement): String = "${file.path}/${file.name}.${file.extension}}"
}