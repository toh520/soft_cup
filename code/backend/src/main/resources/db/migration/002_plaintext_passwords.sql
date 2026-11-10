-- 用户明确要求课设中保存原密码以便查看。旧哈希无法还原，留作首次登录兼容。
ALTER TABLE users ADD COLUMN password TEXT;
