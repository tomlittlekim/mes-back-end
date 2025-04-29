package kr.co.imoscloud.util

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.entity.drive.FileManagement
import kr.co.imoscloud.service.drive.FileConvertService
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class AbstractPrint(
    private val core: Core,
    private val converter: FileConvertService
) {

    abstract fun getFodsFile(): FileManagement
    abstract fun <B> entityToPrintDto(body : B): PrintDto
    abstract fun <H> getETCFromHeader(header: H?): MutableMap<String, String>

    protected fun <H, B> process(head: H?, body : B, isRowType: Boolean?=null) : File {
        val bodyMap = generateBody(listOf(body), false, isRowType)

        val base: FileManagement = getFodsFile()
        val headerMap = generateHeader(base.menuId!!)
        val etcMap = head?.let { getETCFromHeader(it) }

        val newFilename = "${headerMap["companyName"]}_${headerMap["title"]}.fods"
        val copiedFile = copyToFile(base, newFilename)

        replaceFods(copiedFile, (bodyMap + (etcMap?.let { headerMap + it } ?: headerMap)))
        return converter.convertToPdfUsingLibreOffice(copiedFile)
    }

    protected fun <H, B> process(head: H?, bodies : List<B>, isRowType: Boolean?=null) : File {
        val bodyMap = generateBody(bodies, true, isRowType)

        val base: FileManagement = getFodsFile()
        val headerMap = generateHeader(base.menuId!!)
        val etcMap = head?.let { getETCFromHeader(it) }

        val newFilename = "${headerMap["companyName"]}_${headerMap["title"]}.fods"
        val copiedFile = copyToFile(base, newFilename)

        replaceFods(copiedFile, (bodyMap + (etcMap?.let { headerMap + it } ?: headerMap)))
        return converter.convertToPdfUsingLibreOffice(copiedFile)
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
        isRowType: Boolean?=null
    ): MutableMap<String, String?> {
        val resultMap: MutableMap<String, String?> = mutableMapOf()

        if (isList) {
            list.forEachIndexed { i, value ->
                val dto = entityToPrintDto(value)
                val index = i + 1
                if (isRowType == true) {
                    dto.col1?.let { resultMap["col${index}_${index}_1"] = it }
                    dto.col2?.let { resultMap["col${index}_${index}_2"] = it }
                    dto.col3?.let { resultMap["col${index}_${index}_3"] = it }
                    dto.col4?.let { resultMap["col${index}_${index}_4"] = it }
                    dto.col5?.let { resultMap["col${index}_${index}_5"] = it }
                    dto.col6?.let { resultMap["col${index}_${index}_6"] = it }
                    dto.col7?.let { resultMap["col${index}_${index}_7"] = it }
                    dto.col8?.let { resultMap["col${index}_${index}_8"] = it }
                    dto.col9?.let { resultMap["col${index}_${index}_9"] = it }
                    dto.col10?.let { resultMap["col${index}_${index}_10"] = it }
                    dto.col11?.let { resultMap["col${index}_${index}_11"] = it }
                    dto.col12?.let { resultMap["col${index}_${index}_12"] = it }
                    dto.col13?.let { resultMap["col${index}_${index}_13"] = it }
                    dto.col14?.let { resultMap["col${index}_${index}_14"] = it }
                    dto.col15?.let { resultMap["col${index}_${index}_15"] = it }
                    dto.col16?.let { resultMap["col${index}_${index}_16"] = it }
                    dto.col17?.let { resultMap["col${index}_${index}_17"] = it }
                    dto.col18?.let { resultMap["col${index}_${index}_18"] = it }
                    dto.col19?.let { resultMap["col${index}_${index}_19"] = it }
                    dto.col20?.let { resultMap["col${index}_${index}_20"] = it }
                } else {
                    dto.col1?.let { resultMap["col1_${index}_${index}"] = it }
                    dto.col2?.let { resultMap["col2_${index}_${index}"] = it }
                    dto.col3?.let { resultMap["col3_${index}_${index}"] = it }
                    dto.col4?.let { resultMap["col4_${index}_${index}"] = it }
                    dto.col5?.let { resultMap["col5_${index}_${index}"] = it }
                    dto.col6?.let { resultMap["col6_${index}_${index}"] = it }
                    dto.col7?.let { resultMap["col7_${index}_${index}"] = it }
                    dto.col8?.let { resultMap["col8_${index}_${index}"] = it }
                    dto.col9?.let { resultMap["col9_${index}_${index}"] = it }
                    dto.col10?.let { resultMap["col10_${index}_${index}"] = it }
                    dto.col11?.let { resultMap["col11_${index}_${index}"] = it }
                    dto.col12?.let { resultMap["col12_${index}_${index}"] = it }
                    dto.col13?.let { resultMap["col13_${index}_${index}"] = it }
                    dto.col14?.let { resultMap["col14_${index}_${index}"] = it }
                    dto.col15?.let { resultMap["col15_${index}_${index}"] = it }
                    dto.col16?.let { resultMap["col16_${index}_${index}"] = it }
                    dto.col17?.let { resultMap["col17_${index}_${index}"] = it }
                    dto.col18?.let { resultMap["col18_${index}_${index}"] = it }
                    dto.col19?.let { resultMap["col19_${index}_${index}"] = it }
                    dto.col20?.let { resultMap["col20_${index}_${index}"] = it }
                }
            }
        } else {
            val printDto = entityToPrintDto(list.first())
            printDto.col1?.let { resultMap["col1"] = it }
            printDto.col2?.let { resultMap["col2"] = it }
            printDto.col3?.let { resultMap["col3"] = it }
            printDto.col4?.let { resultMap["col4"] = it }
            printDto.col5?.let { resultMap["col5"] = it }
            printDto.col6?.let { resultMap["col6"] = it }
            printDto.col7?.let { resultMap["col7"] = it }
            printDto.col8?.let { resultMap["col8"] = it }
            printDto.col9?.let { resultMap["col9"] = it }
            printDto.col10?.let { resultMap["col10"] = it }
            printDto.col11?.let { resultMap["col11"] = it }
            printDto.col12?.let { resultMap["col12"] = it }
            printDto.col13?.let { resultMap["col13"] = it }
            printDto.col14?.let { resultMap["col14"] = it }
            printDto.col15?.let { resultMap["col15"] = it }
            printDto.col16?.let { resultMap["col16"] = it }
            printDto.col17?.let { resultMap["col17"] = it }
            printDto.col18?.let { resultMap["col18"] = it }
            printDto.col19?.let { resultMap["col19"] = it }
            printDto.col20?.let { resultMap["col20"] = it }
        }

        return resultMap
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