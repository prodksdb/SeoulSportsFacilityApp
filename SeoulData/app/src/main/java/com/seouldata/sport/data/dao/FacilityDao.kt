package com.seouldata.sport.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seouldata.sport.data.entity.FacilityEntity

// Entity에 접근해서 CRUD 쿼리를 정의하는 인터페이스
@Dao
interface FacilityDao {

    // 1. 여러 개 insert (중복 svcId면 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(facilities: List<FacilityEntity>)

    // 2. 전체 조회
    @Query("SELECT * FROM facility")
    suspend fun getAll(): List<FacilityEntity>

    // 3. 카테고리(소분류)로 필터링
    @Query("SELECT * FROM facility WHERE minClassNm = :category")
    suspend fun getByCategory(category: String): List<FacilityEntity>

    // 4. ID 기준 단일 조회
    @Query("SELECT * FROM facility WHERE svcId = :svcId")
    suspend fun getById(svcId: String): FacilityEntity?

    // 5. 전체 삭제
    @Query("DELETE FROM facility")
    suspend fun deleteAll()
}
