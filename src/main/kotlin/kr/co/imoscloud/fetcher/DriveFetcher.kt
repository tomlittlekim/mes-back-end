package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.drive.FileManagement
import kr.co.imoscloud.service.drive.DriveService

@DgsComponent
class DriveFetcher(
    private val driveService: DriveService
) {

    @DgsQuery
    fun getAllFiles(): List<FileManagement> = driveService.getFiles()

    @DgsMutation
    fun deleteFile(@InputArgument id: Long) = driveService.deleteFile(id)
}