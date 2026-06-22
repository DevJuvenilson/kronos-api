UPDATE tb_user
SET username = CONCAT('user_', id)
WHERE username IS NULL OR BTRIM(username) = '';

UPDATE tb_user
SET username = LOWER(BTRIM(username));

DROP INDEX IF EXISTS uk_tb_user_username_lower;

CREATE UNIQUE INDEX uk_tb_user_username_lower
    ON tb_user (LOWER(username));

ALTER TABLE tb_user
    ALTER COLUMN username SET NOT NULL;
