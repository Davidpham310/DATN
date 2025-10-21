import com.example.datn.core.network.service.minio.MinIOService
import com.example.datn.domain.repository.IFileRepository
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val minIOService: MinIOService
) : IFileRepository {

    override suspend fun uploadFile(objectName: String, inputStream: InputStream, size: Long, contentType: String) {
        minIOService.uploadFile(objectName, inputStream, size, contentType)
    }

    override suspend fun getFile(objectName: String): InputStream {
        return minIOService.getFile(objectName)
    }

    override suspend fun getFileUrl(objectName: String, expirySeconds: Int): String {
        return minIOService.getFileUrl(objectName, expirySeconds)
    }

    override suspend fun updateFile(objectName: String, newStream: InputStream, size: Long, contentType: String) {
        // Nếu bạn có hàm updateFile trong MinIOService
        minIOService.uploadFile(objectName, newStream, size, contentType)
    }

    override suspend fun deleteFile(objectName: String) {
        minIOService.deleteFile(objectName)
    }

    override suspend fun getDirectFileUrl(objectName: String): String {
        return minIOService.getDirectFileUrl(objectName)
    }

    override suspend fun fileExists(objectName: String): Boolean {
        return minIOService.fileExists(objectName)
    }
}
