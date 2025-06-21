CREATE TABLE blogs
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    slug       VARCHAR(255) NOT NULL,
    tags       VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE
);