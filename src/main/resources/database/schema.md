## TABLE DDL

``` sql

 ## boards
 
CREATE TABLE boards (
    board_id        character varying(36) NOT null PRIMARY KEY,
    board_type      VARCHAR(10) NOT NULL, -- 공지사항, 커뮤니티, 1:1문의, 자주하는 질문
    title           VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    views           INT DEFAULT 0 NOT NULL,
    likes           INT DEFAULT 0 NOT NULL,
    unlikes         INT DEFAULT 0 NOT NULL,
    writer_id       VARCHAR(36) NOT NULL, -- UUID
    writer_name     VARCHAR(30) NOT NULL,
    file_path     character varying(200),
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

## comments

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

CREATE TABLE addresses (
    address_id           CHARACTER VARYING(36) PRIMARY KEY, -- 배송지 ID (PK)
    user_id              CHARACTER VARYING(36) NOT NULL,    -- 사용자 ID (FK)
    receiver             CHARACTER VARYING(50) NOT NULL,    -- 받는 사람 이름
    phone                CHARACTER VARYING(20) NOT NULL,    -- 받는 사람 연락처
    postal_code          CHARACTER VARYING(10) NOT NULL,    -- 우편번호
    address              TEXT NOT NULL,                     -- 기본 주소 (도로명 주소)                             
    is_default           character varying(1) DEFAULT 'Y',  -- 기본 배송지 여부
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

## users

CREATE TABLE users
(
    user_id    character varying(36) NOT null PRIMARY KEY,
    user_name    character varying(50) NOT NULL,
    nick_name   character varying(50) NOT NULL,
    login_id    character varying(60) NOT NULL,
    login_password    character varying(100) NOT NULL,
    is_active    character varying(1) DEFAULT 'Y',
    failed_login_attempts   numeric(3),
    email    character varying(50),
    phone_number    character varying(20),
    create_datetime    timestamp with time zone ,
    creator_id    character varying(36) ,
    creator_login_id    character varying(60) ,
    creator_name    character varying(50) ,
    modify_datetime    timestamp with time zone ,
    modifier_id    character varying(36) ,
    modifier_login_id    character varying(60) ,
    modifier_name    character varying(50) ,
    CONSTRAINT fk_board_creator FOREIGN KEY (creator_id) REFERENCES users(user_id),
    CONSTRAINT fk_board_modifier FOREIGN KEY (modifier_id) REFERENCES users(user_id)
);

## roles

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

## permissions

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

## user_roles

CREATE TABLE user_roles (
    user_id varchar(36) NOT NULL,
    role_id varchar(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

## role_permissions

create table role_permissions(
    role_id varchar(36) NOT NULL,
    permission_id varchar(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id),
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id)
);

## products

CREATE TABLE products (
    product_id character varying(36) NOT null PRIMARY KEY,  -- 제품 ID
    product_name VARCHAR(255) NOT NULL,  -- 제품명
    product_description TEXT,  -- 제품 설명
    price DECIMAL(10, 2) NOT NULL,  -- 가격
    stock_quantity INT NOT NULL DEFAULT 0,  -- 재고 수량
    category VARCHAR(100),  -- 카테고리
    file_path VARCHAR(200),  -- 제품 이미지 URL
    discount_rate SMALLINT NOT NULL DEFAULT 0, -- 제품 할인율
    discount_amount NUMERIC(10,2) GENERATED ALWAYS AS (ROUND(price * discount_rate / 100, 2)) STORED, -- 제품 할인 금액
    discount_price NUMERIC(10,2) GENERATED ALWAYS AS (ROUND(price - (price * discount_rate / 100), 2)) STORED, -- 제품 금액 (총 금액 - 할인 금액)
    create_datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    creator_id VARCHAR(36),
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modify_datetime TIMESTAMP WITH TIME ZONE,
    modifier_id VARCHAR(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50)
);

## bookmarks

CREATE TABLE bookmarks (
    bookmark_id character varying(36) NOT null PRIMARY KEY,  -- 즐겨찾기 ID
    user_id VARCHAR(36) NOT NULL,  -- 사용자 ID
    product_id VARCHAR(36) NOT NULL,  -- 즐겨찾기한 제품 ID
    create_datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    creator_id VARCHAR(36),
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modify_datetime TIMESTAMP WITH TIME ZONE,
    modifier_id VARCHAR(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50),
    CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;
);

## carts

CREATE TABLE carts (
    cart_id character varying(36) NOT null PRIMARY KEY,  -- 장바구니 ID
    user_id VARCHAR(36) NOT NULL,  -- 사용자 ID
    product_id VARCHAR(36) NOT NULL,  -- 장바구니에 담은 제품 ID
    quantity INT NOT NULL CHECK (quantity > 0),  -- 수량
    create_datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    creator_id VARCHAR(36),
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modify_datetime TIMESTAMP WITH TIME ZONE,
    modifier_id VARCHAR(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;
);

## likes
   
CREATE TABLE likes (
    like_id CHARACTER VARYING(36) PRIMARY KEY,
    user_id CHARACTER VARYING(36) NOT NULL,
    board_id CHARACTER VARYING(36) NOT NULL,
    status VARCHAR(10) CHECK (status IN ('LIKE', 'UNLIKE')),
    create_datetime      TIMESTAMP WITH TIME ZONE,
    creator_id           CHARACTER VARYING(36),
    creator_login_id     CHARACTER VARYING(60),
    creator_name         CHARACTER VARYING(50),
    modify_datetime      TIMESTAMP WITH TIME ZONE,
    modifier_id          CHARACTER VARYING(36),
    modifier_login_id    CHARACTER VARYING(60),
    modifier_name        CHARACTER VARYING(50),
    UNIQUE (user_id, board_id) 
);

## orders

CREATE TABLE orders ( --order_items를 합쳐서 최종 결과
    order_id character varying(36) PRIMARY KEY,  -- 주문 ID (고유 식별자)
    user_id character varying(36) NOT NULL, -- 주문한 사용자 ID (FK)
    address_id character varying(36) NOT NULL, -- 배송지 정보 (FK)
    total_price DECIMAL(10,2) NOT NULL, -- 총 주문 금액
    discount_amount DECIMAL(10,2) DEFAULT 0, -- 할인 금액
    final_price DECIMAL(10,2) NOT NULL, -- 최종 결제 금액 (total_price - discount_amount)
    order_status VARCHAR(20) DEFAULT 'PENDING', -- 주문 상태 (PENDING, PAID, CANCELLED, SHIPPED)
    create_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(), -- 생성일
    modify_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(), -- 수정일
    creator_id character varying(36) NOT NULL,
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modifier_id character varying(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id), -- 사용자 테이블 참조
    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses(address_id) -- 배송지 참조
);

## order_items

CREATE TABLE order_items ( -- 제품 개별 할인율과 금액 
    order_item_id character varying(36) PRIMARY KEY, -- 주문 상세 ID
    order_id character varying(36) NOT NULL, -- 주문 ID (FK)
    product_id character varying(36) NOT NULL, -- 상품 ID (FK)
    quantity INT NOT NULL, -- 구매 수량
    unit_price DECIMAL(10,2) NOT NULL, -- 개별 상품 가격
    discount_amount DECIMAL(10,2) DEFAULT 0, -- 할인 금액
    total_price DECIMAL(10,2) NOT NULL, -- 총 가격 (수량 * 개별 가격)
    create_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(),
    modify_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(product_id)
);

## payments

CREATE TABLE payments ( -- 주문에 대한 결제 정보 
    payment_id character varying(36) PRIMARY KEY, -- 결제 ID
    order_id character varying(36) NOT NULL, -- 결제된 주문 ID (FK)
    payment_key VARCHAR(50) NOT NULL UNIQUE, -- 토스페이먼츠 결제 키
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED, REFUNDED
    payment_method VARCHAR(20) NOT NULL, -- 카드, 계좌이체 등
    paid_amount DECIMAL(10,2) NOT NULL, -- 실제 결제 금액
    create_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(),
    modify_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

## files

CREATE TABLE files (
    file_id character varying(36) PRIMARY KEY,
    target_id character varying(36) NOT NULL,
	target_type VARCHAR(50) NOT NULL, -- 타겟 타입
    file_path VARCHAR(255) NOT NULL, -- 파일 경로 
    file_size BIGINT NOT NULL DEFAULT 0, -- 파일 사이즈 
    file_extension VARCHAR(20), -- 파일 확장자
    file_type VARCHAR(100), -- 파일 타입
    original_file_name VARCHAR(255),
    create_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(),
    modify_datetime TIMESTAMP WITH TIME ZONE DEFAULT now(),
    creator_id character varying(36) NOT NULL,
    creator_login_id VARCHAR(60),
    creator_name VARCHAR(50),
    modifier_id character varying(36),
    modifier_login_id VARCHAR(60),
    modifier_name VARCHAR(50),
    CONSTRAINT fk_creator FOREIGN KEY (creator_id) REFERENCES users(user_id),
    CONSTRAINT fk_modifier FOREIGN KEY (modifier_id) REFERENCES users(user_id)
);

   
```