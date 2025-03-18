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
    file_path VARCHAR(200),
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
    file_path    character varying(200),
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

CREATE TABLE products (
    product_id character varying(36) NOT null PRIMARY KEY,  -- 제품 ID
    product_name VARCHAR(255) NOT NULL,  -- 제품명
    product_description TEXT,  -- 제품 설명
    price DECIMAL(10, 2) NOT NULL,  -- 가격
    stock_quantity INT NOT NULL DEFAULT 0,  -- 재고 수량
    category VARCHAR(100),  -- 카테고리
    file_path VARCHAR(200),  -- 제품 이미지 URL
    create_datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    creator_id VARCHAR(36),
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modify_datetime TIMESTAMP WITH TIME ZONE,
    modifier_id VARCHAR(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50)
);

ALTER TABLE products ADD COLUMN discount_rate SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE products
ADD COLUMN discount_amount NUMERIC(10,2) GENERATED ALWAYS AS (ROUND(price * discount_rate / 100, 2)) STORED,
ADD COLUMN discount_price NUMERIC(10,2) GENERATED ALWAYS AS (ROUND(price - (price * discount_rate / 100), 2)) STORED;


CREATE TABLE bookmarks (
    bookmark_id character varying(36) NOT null PRIMARY KEY,  -- 즐겨찾기 ID
    user_id VARCHAR(36) NOT NULL,  -- 사용자 ID
    product_id VARCHAR(36) NOT NULL,  -- 즐겨찾기한 제품 ID

    CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,

    -- BaseEntity 필드
    create_datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    creator_id VARCHAR(36),
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modify_datetime TIMESTAMP WITH TIME ZONE,
    modifier_id VARCHAR(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50)
);

CREATE TABLE carts (
    cart_id character varying(36) NOT null PRIMARY KEY,  -- 장바구니 ID
    user_id VARCHAR(36) NOT NULL,  -- 사용자 ID
    product_id VARCHAR(36) NOT NULL,  -- 장바구니에 담은 제품 ID
    quantity INT NOT NULL CHECK (quantity > 0),  -- 수량

    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,

    -- BaseEntity 필드
    create_datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    creator_id VARCHAR(36),
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modify_datetime TIMESTAMP WITH TIME ZONE,
    modifier_id VARCHAR(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50)
);

CREATE TABLE addresses (
    address_id           CHARACTER VARYING(36) PRIMARY KEY, -- 배송지 ID (PK)
    user_id              CHARACTER VARYING(36) NOT NULL,   -- 사용자 ID (FK)
    receiver             CHARACTER VARYING(50) NOT NULL,   -- 받는 사람 이름
    phone                CHARACTER VARYING(20) NOT NULL,   -- 받는 사람 연락처
    postal_code1          CHARACTER VARYING(10) NOT NULL,   -- 우편번호1
    postal_code2          CHARACTER VARYING(10) ,   -- 우편번호2
    postal_code3          CHARACTER VARYING(10) ,   -- 우편번호3
    address1             TEXT NOT NULL,                   -- 기본 주소 (도로명 주소)
    address2             TEXT,                            -- 상세 주소 (아파트, 동, 호수 등)
    address3             TEXT,                            -- 상세 주소 (아파트, 동, 호수 등)
    default_address_index INT CHECK (default_address_index IN (1, 2, 3)),
    create_datetime      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    creator_id           CHARACTER VARYING(36),
    creator_login_id     CHARACTER VARYING(60),
    creator_name         CHARACTER VARYING(50),
    modify_datetime      TIMESTAMP WITH TIME ZONE,
    modifier_id          CHARACTER VARYING(36),
    modifier_login_id    CHARACTER VARYING(60),
    modifier_name        CHARACTER VARYING(50),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE cascade 
    );
   

CREATE OR REPLACE FUNCTION set_first_default_address()
RETURNS TRIGGER AS $$
BEGIN
    -- 사용자의 기존 주소 개수 확인
    IF (SELECT COUNT(*) FROM addresses WHERE user_id = NEW.user_id) = 0 THEN
        NEW.default_address_index := 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_set_default_address
BEFORE INSERT ON addresses
FOR EACH ROW
EXECUTE FUNCTION set_first_default_address();


```