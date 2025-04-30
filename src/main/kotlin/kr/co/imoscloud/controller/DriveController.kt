package kr.co.imoscloud.controller

import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.service.drive.DriveService
import kr.co.imoscloud.service.drive.LabelExcelRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/drive")
class DriveController(
    private val driveService: DriveService,
) {

    @PostMapping("/add")
    fun addFile(@ModelAttribute req: LabelExcelRequest): String = driveService.addFile(req)

    @GetMapping("/get")
    fun downloadFile(id: Long, response: HttpServletResponse) = driveService.downloadFile(id, response)
}