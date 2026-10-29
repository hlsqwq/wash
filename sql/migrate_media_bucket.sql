-- 兼容旧库的安全迁移：
-- 1) 若 media 表存在拼写错误的旧列 backet 且不存在 bucket，则把数据改名迁移；
-- 2) 若缺少 content_type，则补充该列。
-- 不删除旧列、不清空数据；可重复执行。
-- 用法： mysql -h <host> -P 3306 -uroot -p wash < sql/migrate_media_bucket.sql

USE wash;

SET @has_old = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'media' AND column_name = 'backet'
);
SET @has_new = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'media' AND column_name = 'bucket'
);
SET @sql = IF(@has_old > 0 AND @has_new = 0,
    'ALTER TABLE media CHANGE COLUMN backet bucket VARCHAR(128) NULL COMMENT ''RustFS 桶名''',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_content_type = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'media' AND column_name = 'content_type'
);
SET @sql = IF(@has_content_type = 0,
    'ALTER TABLE media ADD COLUMN content_type VARCHAR(128) NULL COMMENT ''MIME 类型'' AFTER path',
    'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
