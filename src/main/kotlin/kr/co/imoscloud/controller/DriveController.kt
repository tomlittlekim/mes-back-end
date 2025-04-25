package kr.co.imoscloud.controller

import kr.co.imoscloud.service.drive.DriveService
import kr.co.imoscloud.service.drive.LabelExcelRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/drive")
class DriveController(
    private val driveService: DriveService,
) {

    @PostMapping("")
    fun addFile(@ModelAttribute req: LabelExcelRequest): String = driveService.addFile(req)
}