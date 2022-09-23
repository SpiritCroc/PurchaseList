CREATE DATABASE IF NOT EXISTS yourdatabase;
ALTER DATABASE einkaufsliste charset=utf8mb4;

CREATE TABLE IF NOT EXISTS yourdatabase.pitems(
    ID BIGINT PRIMARY KEY,
    NAME TEXT NOT NULL,
    INFO TEXT,
    USAGE1 TEXT,
    CREATOR TEXT NOT NULL,
    UPDATED_BY TEXT,
    CREATION_DATE BIGINT NOT NULL,
    COMPLETION_DATE BIGINT,
    PICTURE_URL TEXT
);

GRANT INSERT,DELETE,UPDATE,SELECT ON yourdatabase.* to 'yourdatabaseuser'@'localhost' IDENTIFIED BY 'yourpassword';
flush privileges;
