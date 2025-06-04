package kr.co.imoscloud.controller

import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.service.drive.DriveService
import kr.co.imoscloud.service.drive.LabelExcelRequest
import org.springframework.web.bind.annotation.*

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