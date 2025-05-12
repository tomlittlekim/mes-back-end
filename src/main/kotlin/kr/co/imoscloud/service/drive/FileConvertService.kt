package kr.co.imoscloud.service.drive

import org.jodconverter.core.office.OfficeManager
import org.jodconverter.local.LocalConverter
import org.springframework.stereotype.Service
import java.io.File

@Service
class FileConvertService(
    private val officeManager: OfficeManager,
) {
    fun excelToFods(savePath: String): File {
        val inputFile = File(savePath)
        val outputFile = File(savePath.replaceAfterLast('.', "fods"))

        LocalConverter.make(officeManager)
            .convert(inputFile)
            .to(outputFile)
            .execute()

        return outputFile
    }

    fun fodsToPdf(fods: File): File {
        val outputPdf = File(fods.absolutePath.replaceAfterLast('.', "pdf"))

        LocalConverter.make(officeManager)
            .convert(fods)
            .to(outputPdf)
            .execute()

        fods.delete()
        return outputPdf
    }

    fun fodsToPng(fods: File): File {
        val outputPng = File(fods.absolutePath.replaceAfterLast('.', "png"))

        LocalConverter.make(officeManager)
            .convert(fods)
            .to(outputPng)
            .execute()

        return outputPng
    }

}