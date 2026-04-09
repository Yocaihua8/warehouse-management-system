SET @user_role_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'role'
);

SET @user_role_sql = IF(
    @user_role_exists = 0,
    'ALTER TABLE user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''OPERATOR'' COMMENT ''角色 ADMIN/OPERATOR'' AFTER status',
    'SELECT 1'
);

PREPARE stmt_user_role FROM @user_role_sql;
EXECUTE stmt_user_role;
DEALLOCATE PREPARE stmt_user_role;

UPDATE user
SET role = CASE
    WHEN LOWER(username) = 'admin' THEN 'ADMIN'
    WHEN role IS NULL OR TRIM(role) = '' THEN 'OPERATOR'
    ELSE role
END;
