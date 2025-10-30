# Database Migration Guide - Version 1 → 2

## Tổng Quan
Migration này thêm hỗ trợ cho PUZZLE và MATCHING game types bằng cách thêm các trường mới vào bảng `minigame_option`.

## Thay Đổi Schema

### Bảng: `minigame_option`

#### Các cột mới được thêm:
| Cột | Kiểu | Nullable | Mô tả |
|-----|------|----------|-------|
| `hint` | TEXT | YES | Gợi ý cho PUZZLE game (VD: "a__le" cho "apple") |
| `pairId` | TEXT | YES | ID của option được ghép (dự phòng cho tương lai) |
| `pairContent` | TEXT | YES | Nội dung ghép cặp cho MATCHING game |

## Files Đã Cập Nhật

### 1. Domain Layer
✅ **MiniGameOption.kt** (Model)
- Thêm `hint: String?`
- Thêm `pairId: String?`
- Thêm `pairContent: String?`

### 2. Data Layer - Entity
✅ **MiniGameOptionEntity.kt**
- Thêm `hint: String? = null`
- Thêm `pairId: String? = null`
- Thêm `pairContent: String? = null`

### 3. Data Layer - Mapper
✅ **MiniGameOptionMapper.kt**
- Cập nhật `toDomain()` để map hint, pairId, pairContent
- Cập nhật `toEntity()` để map hint, pairId, pairContent

### 4. Data Layer - Database
✅ **AppDatabase.kt**
- Tăng version từ 1 → 2
- Thêm `MIGRATION_1_2` với SQL:
  ```sql
  ALTER TABLE minigame_option ADD COLUMN hint TEXT DEFAULT NULL
  ALTER TABLE minigame_option ADD COLUMN pairId TEXT DEFAULT NULL
  ALTER TABLE minigame_option ADD COLUMN pairContent TEXT DEFAULT NULL
  ```
- Thêm migration vào database builder

## Migration SQL

```sql
-- Thêm cột hint cho PUZZLE game
ALTER TABLE minigame_option ADD COLUMN hint TEXT DEFAULT NULL;

-- Thêm cột pairId (dự phòng)
ALTER TABLE minigame_option ADD COLUMN pairId TEXT DEFAULT NULL;

-- Thêm cột pairContent cho MATCHING game
ALTER TABLE minigame_option ADD COLUMN pairContent TEXT DEFAULT NULL;
```

## Backward Compatibility

✅ **Migration an toàn**:
- Tất cả các cột mới đều nullable (`DEFAULT NULL`)
- Dữ liệu cũ sẽ không bị ảnh hưởng
- Các option hiện có sẽ có giá trị NULL cho các trường mới

## Testing Checklist

### Trước khi deploy:
- [ ] Test migration trên database có dữ liệu
- [ ] Verify các option cũ vẫn hoạt động bình thường
- [ ] Test tạo option mới với hint (PUZZLE)
- [ ] Test tạo option mới với pairContent (MATCHING)
- [ ] Test mapper chuyển đổi đúng giữa entity và domain

### Sau khi deploy:
- [ ] Monitor crash reports
- [ ] Verify database integrity
- [ ] Check performance impact

## Rollback Plan

Nếu cần rollback về version 1:
1. Downgrade app version
2. Database sẽ giữ nguyên các cột mới (không ảnh hưởng)
3. App version cũ sẽ ignore các cột mới

**Lưu ý**: Không thể tự động xóa cột trong SQLite. Nếu cần xóa hoàn toàn:
```sql
-- Tạo bảng mới không có các cột
CREATE TABLE minigame_option_new (...);

-- Copy dữ liệu
INSERT INTO minigame_option_new SELECT id, miniGameQuestionId, ... FROM minigame_option;

-- Xóa bảng cũ
DROP TABLE minigame_option;

-- Rename bảng mới
ALTER TABLE minigame_option_new RENAME TO minigame_option;
```

## Lưu Ý Quan Trọng

⚠️ **Khi uninstall/reinstall app**:
- Database sẽ bị xóa hoàn toàn
- Cần sync lại dữ liệu từ server

⚠️ **Khi update app**:
- Migration tự động chạy khi mở app lần đầu
- Không cần can thiệp thủ công

✅ **Best Practices**:
- Luôn test migration trên emulator trước
- Backup database trước khi update (nếu có dữ liệu quan trọng)
- Monitor logs khi migration chạy

## Version History

| Version | Changes | Date |
|---------|---------|------|
| 1 → 2 | Thêm hint, pairId, pairContent cho PUZZLE & MATCHING | 2025-10-29 |
