package kr.co.imoscloud.util

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.entity.drive.FileManagement
import kr.co.imoscloud.service.drive.FileConvertService
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class AbstractPrint(
    private val core: Core,
    private val converter: FileConvertService
) {

    //TODO :: 추후 메뉴와 연결해서 제거할 필요가 있음
    abstract fun getFodsFile(): FileManagement

    abstract fun <B> entityToPrintDto(body : B): PrintDto
    abstract fun <H> extractAdditionalHeaderFields(header: H?): MutableMap<String, String>?
    abstract fun <B> extractAdditionalBodyFields(bodies : List<B>): MutableMap<String, String>?

    protected fun <H, B> process(head: H?, body : B, isRowType: Boolean?=null) : File {
        val bodyMap = generateBody(listOf(body), false, isRowType)

        val base: FileManagement = getFodsFile()
        val headerMap = generateHeader(base.menuId!!)
        val etcMap = head?.let { extractAdditionalHeaderFields(it) }

        val uuid = formattedDate(LocalDateTime.now(), CoreEnum.DateTimeFormat.MOS_EVENT_TIME)
        val newFilename = "$uuid.fods"
        val copiedFile = copyToFile(base, newFilename)

        replaceFods(copiedFile, (bodyMap + (etcMap?.let { headerMap + it } ?: headerMap)))
        return converter.fodsToPdf(copiedFile)
    }

    protected fun <H, B> process(head: H?, bodies : List<B>, isRowType: Boolean?=null) : File {
        val bodyMap = generateBody(bodies, true, isRowType)
        val extractBodyMap = extractAdditionalBodyFields(bodies)
        val totalBodyMap = extractBodyMap?.let { bodyMap + it } ?: bodyMap

        val base: FileManagement = getFodsFile()
        val headerMap = generateHeader(base.menuId!!)
        val extractHeaderMap = head?.let { extractAdditionalHeaderFields(it) }
        val totalHeaderMap = extractHeaderMap?.let { headerMap + it } ?: totalBodyMap

        val uuid = formattedDate(LocalDateTime.now(), CoreEnum.DateTimeFormat.MOS_EVENT_TIME)
        val newFilename = "$uuid.fods"
        val copiedFile = copyToFile(base, newFilename)

        replaceFods(copiedFile, (totalBodyMap + totalHeaderMap))
        return converter.fodsToPdf(copiedFile)
    }

    private fun replaceFods(fods: File, map: Map<String, String?>) {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(fods)

        val nodeList = doc.getElementsByTagName("text:p")
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            val textContent = node.textContent
            var updatedTextContent = textContent

            val regex = Regex("""\$\{([^${'$'}{}]*?)\}""")
            val matches = regex.findAll(textContent)

            for (match in matches) {
                val key = match.groupValues[1]
                map[key].let { replacement ->
                    updatedTextContent = updatedTextContent.replace(match.value, replacement?:"")
                }
            }

            node.textContent = updatedTextContent
        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        val source = DOMSource(doc)
        val result = StreamResult(fods)
        transformer.transform(source, result)
    }

    private fun copyToFile(base: FileManagement, newFilename: String): File {
        val originalFile = File(base.path)
        val directory = originalFile.parentFile
        val copiedFile = File(directory, newFilename)
        originalFile.copyTo(copiedFile, overwrite = true)
        return copiedFile
    }

    private fun generateHeader(menuId: String): MutableMap<String, String> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val map: MutableMap<String, String> = core.companyRepo
            .getInitialHeader(loginUser.getSite(), loginUser.compCd, menuId)
            .toMutableMap()

        map["owner"] = core.getUserFromInMemory(map["owner"]!!)?.userName ?: "-"
        return map
    }

    private fun <B> generateBody(
        list: List<B>,
        isList: Boolean,
        isRowType: Boolean? = null
    ): MutableMap<String, String?> {
        val resultMap: MutableMap<String, String?> = mutableMapOf()

        if (isList) {
            list.forEachIndexed { i, value ->
                val dto = entityToPrintDto(value)
                if (isRowType == true) {
                    // 가로 타입 매핑 (1_1, 2_1, ...)
                    dtoToList(dto).forEachIndexed { index, value ->
                        resultMap["${index + 1}_${i + 1}"] = value
                    }
                } else {
                    // 세로 타입 매핑 (1_1, 1_2, ...)
                    dtoToList(dto).forEachIndexed { index, value ->
                        resultMap["${i + 1}_${index + 1}"] = value
                    }
                }
            }
        } else {
            val dto = entityToPrintDto(list.first())
            dtoToList(dto).forEachIndexed { index, value ->
                resultMap["${index + 1}"] = value
            }
        }

        return resultMap
    }

    private fun dtoToList(dto: PrintDto): List<String?> = listOf(
        dto.col1, dto.col2, dto.col3, dto.col4, dto.col5,
        dto.col6, dto.col7, dto.col8, dto.col9, dto.col10,
        dto.col11, dto.col12, dto.col13, dto.col14, dto.col15,
        dto.col16, dto.col17, dto.col18, dto.col19, dto.col20
    )

    protected fun <D> formattedDate(date: D, format: CoreEnum.DateTimeFormat): String {
        val formatter = DateTimeFormatter.ofPattern(format.value)

        return when (date) {
            is LocalDate -> formatter.format(date)
            is LocalDateTime -> formatter.format(date)
            is YearMonth -> formatter.format(date)
            else -> ""
        }
    }

    protected fun stringToInt(str: String?): Int {
        if (str == null) return 0

        val regex = Regex("""^\d+$""")
        return if (regex.matches(str)) str.toInt() else 0
    }
}

data class PrintDto(
    val col1: String? = null,
    val col2: String? = null,
    val col3: String? = null,
    val col4: String? = null,
    val col5: String? = null,
    val col6: String? = null,
    val col7: String? = null,
    val col8: String? = null,
    val col9: String? = null,
    val col10: String? = null,
    val col11: String? = null,
    val col12: String? = null,
    val col13: String? = null,
    val col14: String? = null,
    val col15: String? = null,
    val col16: String? = null,
    val col17: String? = null,
    val col18: String? = null,
    val col19: String? = null,
    val col20: String? = null,
)