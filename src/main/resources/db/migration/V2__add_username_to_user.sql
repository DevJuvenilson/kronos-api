ALTER TABLE tb_user
    ADD COLUMN username VARCHAR(50);

CREATE UNIQUE INDEX uk_tb_user_username_lower
    ON tb_user (LOWER(username))
    WHERE username IS NOT NULL;
