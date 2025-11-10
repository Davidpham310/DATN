# 🔄 Sync Implementation Status

## ❌ VẤN ĐỀ ĐÃ PHÁT HIỆN

### Root Cause: SyncManager Được Inject Nhưng KHÔNG Được Sử Dụng!

**File:** `TestRepositoryImpl.kt`

```kotlin
@Singleton
class TestRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val testDao: TestDao,
    private val testQuestionDao: TestQuestionDao,
    private val studentTestResultDao: StudentTestResultDao,
    private val testOptionDao: TestOptionDao,
    private val studentTestAnswerDao: StudentTestAnswerDao,
    private val syncManager: FirebaseRoomSyncManager  // ← INJECTED but NEVER CALLED!
) : ITestRepository {
```

**Kết quả:** Dữ liệu KHÔNG BAO GIỜ được đồng bộ từ Firebase về Room!

---

## ✅ ĐÃ SỬA

### 1. `getTestDetails()` - ✅ FIXED

**Before:**
```kotlin
override fun getTestDetails(testId: String): Flow<Resource<Test>> = flow {
    emit(Resource.Loading())
    // Không gọi sync, chỉ fetch trực tiếp từ Firebase
    when (val result = firebaseDataSource.getTestById(testId)) {
        // ...
    }
}
```

**After:**
```kotlin
override fun getTestDetails(testId: String): Flow<Resource<Test>> = flow {
    emit(Resource.Loading())
    
    // ✅ STEP 1: SYNC từ Firebase → Room
    syncManager.syncTestData(testId, forceSync = false)
    
    // ✅ STEP 2: Fetch từ Firebase (source of truth)
    when (val result = firebaseDataSource.getTestById(testId)) {
        is Resource.Success -> {
            val remote = result.data
            if (remote != null) {
                testDao.insert(remote.toEntity())
                emit(Resource.Success(remote))
            } else {
                // ✅ Fallback to Room cache
                val cached = testDao.getById(testId)
                if (cached != null) {
                    emit(Resource.Success(cached.toDomain()))
                } else {
                    emit(Resource.Error("Không tìm thấy bài kiểm tra"))
                }
            }
        }
        is Resource.Error -> {
            // ✅ Network error → Fallback to Room
            val cached = testDao.getById(testId)
            if (cached != null) {
                emit(Resource.Success(cached.toDomain()))
            } else {
                emit(Resource.Error(result.message))
            }
        }
        else -> {}
    }
}
```

**Benefits:**
- ✅ Auto-sync Test + Questions + Options khi gọi `getTestDetails()`
- ✅ Fallback to Room cache khi Firebase error
- ✅ Offline support với cached data

---

## ⚠️ CẦN SỬA THÊM

### 2. `getStudentResult()` - ❌ CHƯA CÓ SYNC

**Vấn đề:** Không sync student test result

**Cần thêm:**
```kotlin
override fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>> = flow {
    emit(Resource.Loading())
    
    // ❌ MISSING: Should sync test data first
    // syncManager.syncTestData(testId, forceSync = false)
    
    when (val remote = firebaseDataSource.getStudentResult(studentId, testId)) {
        // ... existing code
    }
}
```

### 3. `getTestQuestions()` - ❌ CHƯA CÓ SYNC

**Vấn đề:** Không sync questions nếu gọi trực tiếp

**Có fallback nhưng nên thêm sync:**
```kotlin
override fun getTestQuestions(testId: String): Flow<Resource<List<TestQuestion>>> = flow {
    emit(Resource.Loading())
    
    // ❌ MISSING: Should sync first
    // syncManager.syncTestData(testId, forceSync = false)
    
    when (val result = firebaseDataSource.getTestQuestions(testId)) {
        // ... existing code có fallback to Room
    }
}
```

### 4. `getQuestionOptions()` - ❌ CHƯA CÓ SYNC  

**Vấn đề:** Không sync options

**Cần sync toàn bộ test data trước:**
```kotlin
override fun getQuestionOptions(questionId: String): Flow<Resource<List<TestOption>>> = flow {
    emit(Resource.Loading())
    
    // ❌ MISSING: Should sync test data
    // Need testId to sync - có thể cần refactor
    
    when (val result = firebaseDataSource.getTestOptionsByQuestion(questionId)) {
        // ... existing code có fallback to Room
    }
}
```

---

## 📊 SYNC FLOW HIỆN TẠI

### Test Details Flow (✅ Đã sửa)

```
User opens Test Details
  ↓
getTestDetails(testId)
  ↓
syncManager.syncTestData(testId, forceSync=false)
  ├─ Check Room cache
  │   ├─ Has data? → Skip sync ✓
  │   └─ Empty? → Continue
  ├─ Fetch Test from Firebase → Save to Room
  ├─ Fetch Questions from Firebase → Save to Room  
  └─ Fetch Options from Firebase → Save to Room
  ↓
Fetch latest from Firebase (source of truth)
  ├─ Success? → Return Firebase data
  ├─ Error/Null? → Fallback to Room cache
  └─ Not found? → Error
  ↓
Display Test Details ✅
```

### Student Result Flow (❌ Chưa sửa)

```
User views Test Result
  ↓
getStudentResult(studentId, testId)
  ↓
❌ NO SYNC! Goes straight to Firebase
  ↓
Firebase.getStudentResult()
  ├─ Success? → Save to Room, return
  └─ Error? → Return error (NO fallback!)
  ↓
May fail if offline ❌
```

---

## 🎯 KHUYẾN NGHỊ

### Immediate Actions (Cần làm ngay)

1. **✅ DONE:** `getTestDetails()` - Đã có sync
2. **TODO:** `getStudentResult()` - Thêm fallback to Room khi Firebase error
3. **TODO:** `getTestQuestions()` - Thêm sync (hoặc dựa vào sync của getTestDetails)
4. **TODO:** `getQuestionOptions()` - Thêm sync hoặc require testId

### Optional Improvements

1. **Cache Strategy:**
   - Thêm timestamp để check cache expiration
   - Force refresh khi data cũ hơn X phút

2. **Batch Sync:**
   - Sync multiple tests cùng lúc cho lesson
   - Background sync khi app start

3. **Error Handling:**
   - Retry logic khi sync fail
   - Exponential backoff

---

## 🧪 CÁCH TEST

### Test Case 1: Normal Flow (Online)
```
1. Login
2. Navigate to Test Details
3. Check Logcat:
   [SyncManager] syncTestData START
   [SyncManager] Fetching from Firebase...
   [SyncManager] ✅ Test synced to Room
   [SyncManager] ✅ 10 questions synced
   [SyncManager] ✅ 40 options synced
   [SyncManager] ✅ COMPLETE
4. Should display test details ✅
```

### Test Case 2: Offline Mode
```
1. Online: View test (sync happens)
2. Turn OFF WiFi
3. Go back, then reopen test
4. Check Logcat:
   [SyncManager] Found in cache, skip sync
   [TestRepository] Firebase error, using Room cache
5. Should display from cache ✅
```

### Test Case 3: Clear Cache
```
1. Settings → Clear app data
2. Login again
3. View test (with network)
4. Check Logcat:
   [SyncManager] Fetching from Firebase... (no cache)
   [SyncManager] ✅ COMPLETE
5. Data should be recovered from Firebase ✅
```

### Test Case 4: View Result (Current Issue)
```
1. Submit test
2. View result
3. Turn OFF WiFi
4. Try to view result again
5. ❌ PROBLEM: Fails because no fallback to Room!
6. Need to add Room fallback
```

---

## 📝 CODE CHANGES NEEDED

### Fix `getStudentResult()`:

```kotlin
override fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>> = flow {
    try {
        emit(Resource.Loading())
        
        when (val remote = firebaseDataSource.getStudentResult(studentId, testId)) {
            is Resource.Success -> {
                val res = remote.data
                if (res != null) studentTestResultDao.insert(res.toEntity())
                emit(Resource.Success(res))
            }
            is Resource.Error -> {
                // ✅ ADD FALLBACK to Room
                val cached = studentTestResultDao.getByStudentAndTest(studentId, testId)
                if (cached != null) {
                    emit(Resource.Success(cached.toDomain()))
                } else {
                    emit(Resource.Error(remote.message))
                }
            }
            is Resource.Loading -> emit(Resource.Loading())
        }
    } catch (e: Exception) {
        emit(Resource.Error("Lỗi khi lấy kết quả: ${e.message}"))
    }
}
```

**NOTE:** Cần thêm method `getByStudentAndTest()` vào `StudentTestResultDao`!

---

## ✅ SUMMARY

### Đã Fix:
- ✅ `getTestDetails()` - Có sync + fallback

### Cần Fix:
- ❌ `getStudentResult()` - Thêm fallback to Room
- ❌ `getTestQuestions()` - Đảm bảo sync được gọi
- ❌ `getQuestionOptions()` - Đảm bảo sync được gọi
- ❌ Add DAO method: `StudentTestResultDao.getByStudentAndTest()`

### Impact:
- **Before:** Không sync, không có offline support
- **After (getTestDetails):** Có sync, có offline support
- **Still Missing:** Student result vẫn không có offline support

---

**Next Steps:**
1. Fix `getStudentResult()` với Room fallback
2. Add missing DAO method
3. Test offline mode
4. Build & verify

**Status:** 🟡 PARTIALLY FIXED - Core sync works, need to complete remaining methods
