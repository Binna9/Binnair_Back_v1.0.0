## TABLE DDL

``` sql
CREATE TABLE boards (
    board_id        character varying(36) NOT null PRIMARY KEY,
    board_type      VARCHAR(10) NOT NULL, -- 공지사항, 커뮤니티, 1:1문의, 자주하는 질문
    title           VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    views           INT DEFAULT 0,
    likes           INT DEFAULT 0,
    writer_id       VARCHAR(36) NOT NULL, -- UUID
    writer_name     VARCHAR(30) NOT NULL,
    create_datetime    timestamp with time zone,
    creator_id    character varying(36),
    creator_login_id    character varying(60),
    creator_name    character varying(50),
    modify_datetime    timestamp with time zone,
    modifier_id    character varying(36),
    modifier_login_id    character varying(60),
    modifier_name    character varying(50),
    CONSTRAINT fk_board_writer FOREIGN KEY (writer_id) REFERENCES users(user_id),
    CONSTRAINT fk_board_creator FOREIGN KEY (creator_id) REFERENCES users(user_id),
    CONSTRAINT fk_board_modifier FOREIGN KEY (modifier_id) REFERENCES users(user_id)
);

ALTER TABLE boards ADD COLUMN file_path VARCHAR(255);

ALTER TABLE boards ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

CREATE TABLE comments (
    comment_id     character varying(36) NOT null PRIMARY KEY,
    board_id       character varying(36) NOT null,
    parent_id      character varying(36),
    writer_id      VARCHAR(36) NOT NULL,
    writer_name    VARCHAR(30) NOT NULL,
    content        TEXT NOT NULL,
    create_datetime    timestamp with time zone NOT NULL,
    creator_id    character varying(36),
    creator_login_id    character varying(60),
    creator_name    character varying(50),
    modify_datetime    timestamp with time zone,
    modifier_id    character varying(36),
    modifier_login_id    character varying(60),
    modifier_name    character varying(50),
    CONSTRAINT fk_comment_board FOREIGN KEY (board_id) REFERENCES boards(board_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments(comment_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_writer FOREIGN KEY (writer_id) REFERENCES users(user_id),
    CONSTRAINT fk_comment_creator FOREIGN KEY (creator_id) REFERENCES users(user_id),
    CONSTRAINT fk_comment_modifier FOREIGN KEY (modifier_id) REFERENCES users(user_id)
);

CREATE TABLE users
(
    user_id    character varying(36) NOT null PRIMARY KEY,
    user_name    character varying(50) NOT NULL,
    login_id    character varying(60) NOT NULL,
    login_password    character varying(100) NOT NULL,
    image_file_path    character varying(200),
    is_active BOOLEAN DEFAULT true,
    failed_login_attempts   numeric(3),
    email    character varying(50),
    phone_number    character varying(20),
    create_datetime    timestamp with time zone NOT NULL,
    creator_id    character varying(36) NOT NULL,
    creator_login_id    character varying(60) NOT NULL,
    creator_name    character varying(50) NOT NULL,
    modify_datetime    timestamp with time zone NOT NULL,
    modifier_id    character varying(36) NOT NULL,
    modifier_login_id    character varying(60) NOT NULL,
    modifier_name    character varying(50) NOT NULL,
    CONSTRAINT fk_board_creator FOREIGN KEY (creator_id) REFERENCES users(user_id),
    CONSTRAINT fk_board_modifier FOREIGN KEY (modifier_id) REFERENCES users(user_id)
);

CREATE TABLE roles
(
    role_id    character varying(36) NOT null PRIMARY KEY,
    role_name    character varying(50) NOT NULL,
    role_description    character varying(1000),
    create_datetime    timestamp with time zone NOT NULL,
    creator_id    character varying(36) NOT NULL,
    creator_login_id    character varying(60) NOT NULL,
    creator_name    character varying(50) NOT NULL,
    modify_datetime    timestamp with time zone NOT NULL,
    modifier_id    character varying(36) NOT NULL,
    modifier_login_id    character varying(60) NOT NULL,
    modifier_name    character varying(50) NOT NULL
);

CREATE TABLE permissions
(
    permission_id    character varying(36) NOT null PRIMARY KEY,
    permission_name    character varying(50) NOT NULL,
    permission_description    character varying(1000),
    create_datetime    timestamp with time zone NOT NULL,
    creator_id    character varying(36) NOT NULL,
    creator_login_id    character varying(60) NOT NULL,
    creator_name    character varying(50) NOT NULL,
    modify_datetime    timestamp with time zone NOT NULL,
    modifier_id    character varying(36) NOT NULL,
    modifier_login_id    character varying(60) NOT NULL,
    modifier_name    character varying(50) NOT NULL
);

create table role_permissions(
    role_id varchar(36) NOT NULL,
    permission_id varchar(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id),
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id)
);

CREATE TABLE user_roles (
    user_id varchar(36) NOT NULL,
    role_id varchar(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
);



```