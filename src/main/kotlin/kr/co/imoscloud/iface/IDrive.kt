package kr.co.imoscloud.iface

import kr.co.imoscloud.entity.drive.FileManagement
import java.net.URLEncoder

interface IDrive {

    fun getNameWithExtension(originalFileName: String?): Pair<String, String> {
        originalFileName ?: throw IllegalArgumentException("잘못된 파일 형식 입니다.")

        val list = originalFileName.split("\\.".toRegex())
        val name = if(list.size > 2) list.subList(0, list.size - 1).joinToString(".") else list.first()
        return Pair(name, list.last())
    }

    fun getSavePath(file: FileManagement): String = "${file.path}/${file.name}.${file.extension}"
    fun getOriginalFilename(file: FileManagement): String = "${file.name}.${file.extension}"
    fun encodeToString(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}