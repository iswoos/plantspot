package com.studio.plantspot.domain.repository

interface FileRepository {
    /**
     * 비트맵 데이터를 갤러리(MediaStore)에 저장합니다.
     * @param image 저장할 이미지 데이터
     * @param fileName 파일명 (null일 경우 자동 생성)
     * @return 저장 성공 여부
     */
    suspend fun saveImageToGallery(image: ByteArray, fileName: String? = null): Result<Unit>
    suspend fun uploadFile(bucket: String, path: String, data: ByteArray): String
}
