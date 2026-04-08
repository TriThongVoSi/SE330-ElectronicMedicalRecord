SET @drop_google_index_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'user_accounts'
              AND index_name = 'uk_user_accounts_google_id'
        ),
        'ALTER TABLE user_accounts DROP INDEX uk_user_accounts_google_id',
        'SELECT 1'
    )
);
PREPARE drop_google_index_stmt FROM @drop_google_index_sql;
EXECUTE drop_google_index_stmt;
DEALLOCATE PREPARE drop_google_index_stmt;

SET @drop_google_column_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_accounts'
              AND column_name = 'google_id'
        ),
        'ALTER TABLE user_accounts DROP COLUMN google_id',
        'SELECT 1'
    )
);
PREPARE drop_google_column_stmt FROM @drop_google_column_sql;
EXECUTE drop_google_column_stmt;
DEALLOCATE PREPARE drop_google_column_stmt;
