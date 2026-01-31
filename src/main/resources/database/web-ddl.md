## : WEB TABLE DDL

### users
``` sql
CREATE TABLE web.users (
	user_id varchar(36) NOT NULL,
	user_name varchar(50) NOT NULL,
	login_id varchar(60) NOT NULL,
	login_password varchar(100) NOT NULL,
	provider_id varchar(36) NULL,
	provider varchar(30) NULL,
	failed_login_attempts numeric(3) NULL,
	email varchar(50) NULL,
	phone_number varchar(20) NULL,
	nick_name varchar(50) NULL,
	is_active varchar(1) NOT NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT users_pkey PRIMARY KEY (user_id)
);

-- 로그인 ID는 중복 방지와 검색 성능을 위해 UNIQUE 인덱스 권장
CREATE UNIQUE INDEX idx_users_login_id ON web.users(login_id);

-- 활성화 상태 값 제한
ALTER TABLE web.users ADD CONSTRAINT chk_users_is_active 
CHECK (is_active IN ('Y', 'N', 'L'));

-- 테이블 코멘트
COMMENT ON TABLE web.users IS '시스템 사용자 정보 테이블';
-- 컬럼 코멘트
COMMENT ON COLUMN web.users.user_id IS '사용자 고유 식별자 (UUID)';
COMMENT ON COLUMN web.users.user_name IS '사용자 실명';
COMMENT ON COLUMN web.users.login_id IS '로그인 아이디';
COMMENT ON COLUMN web.users.login_password IS '암호화된 로그인 비밀번호';
COMMENT ON COLUMN web.users.provider_id IS 'OAuth 제공자에서의 사용자 ID (소셜 로그인 시 사용)';
COMMENT ON COLUMN web.users.provider IS 'OAuth 제공자 (google, kakao, naver, facebook 등)';
COMMENT ON COLUMN web.users.failed_login_attempts IS '연속 로그인 실패 횟수 (보안 정책에 따른 계정 잠금용)';
COMMENT ON COLUMN web.users.email IS '사용자 이메일 주소';
COMMENT ON COLUMN web.users.phone_number IS '사용자 전화번호';
COMMENT ON COLUMN web.users.nick_name IS '사용자 닉네임';
COMMENT ON COLUMN web.users.is_active IS '계정 활성화 상태: Y(활성), N(비활성), L(잠금) 등';
COMMENT ON COLUMN web.users.create_datetime IS '계정 생성 일시';
COMMENT ON COLUMN web.users.creator_id IS '생성자 사용자 ID';
COMMENT ON COLUMN web.users.creator_login_id IS '생성자 로그인 ID';
COMMENT ON COLUMN web.users.creator_name IS '생성자 이름';
COMMENT ON COLUMN web.users.modify_datetime IS '최종 수정 일시';
COMMENT ON COLUMN web.users.modifier_id IS '수정자 사용자 ID';
COMMENT ON COLUMN web.users.modifier_login_id IS '수정자 로그인 ID';
COMMENT ON COLUMN web.users.modifier_name IS '수정자 이름';

--- 최초 user system 계정 ---
INSERT INTO web.users
(
    user_id,
    user_name,
    login_id,
    login_password,
    failed_login_attempts,
    email,
    phone_number,
    create_datetime,
    creator_id,
    creator_login_id,
    creator_name,
    modify_datetime,
    modifier_id,
    modifier_login_id,
    modifier_name,
    nick_name,
    is_active,
    provider_id,
    provider
)
VALUES (
    '00000000-0000-0000-0000-000000000000', 
    'system',
    'system',
    'system', 
    0,
    'system@system.com',
    '000-0000-0000',
    now(),
    '00000000-0000-0000-0000-000000000000',
    'system',
    'system',
    now(),
    '00000000-0000-0000-0000-000000000000',
    'system',
    'system',
    'system',
    'Y',
    'SYSTEM',
    'SYSTEM'
);
```
### boards
``` sql 
CREATE TABLE web.boards (
	board_id varchar(36) NOT NULL,
	board_type varchar(10) NOT NULL,
	title varchar(255) NOT NULL,
	content text NOT NULL,
	views int4 DEFAULT 0 NOT NULL,
	likes int4 DEFAULT 0 NOT NULL,
	unlikes int4 DEFAULT 0 NOT NULL,
	writer_id varchar(36) NOT NULL,
	writer_name varchar(30) NOT NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT boards_pkey PRIMARY KEY (board_id)
);

-- web.boards foreign keys

ALTER TABLE web.boards ADD CONSTRAINT fk_board_creator FOREIGN KEY (creator_id) REFERENCES web.users(user_id);
ALTER TABLE web.boards ADD CONSTRAINT fk_board_modifier FOREIGN KEY (modifier_id) REFERENCES web.users(user_id);
ALTER TABLE web.boards ADD CONSTRAINT fk_board_writer FOREIGN KEY (writer_id) REFERENCES web.users(user_id);
``` 
### comments
``` sql
CREATE TABLE web."comments" (
	comment_id varchar(36) NOT NULL,
	board_id varchar(36) NOT NULL,
	parent_id varchar(36) NULL,
	writer_id varchar(36) NOT NULL,
	writer_name varchar(30) NOT NULL,
	content text NOT NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT comments_pkey PRIMARY KEY (comment_id)
);

-- web."comments" foreign keys

ALTER TABLE web."comments" ADD CONSTRAINT fk_comment_board FOREIGN KEY (board_id) REFERENCES web.boards(board_id) ON DELETE CASCADE;
ALTER TABLE web."comments" ADD CONSTRAINT fk_comment_creator FOREIGN KEY (creator_id) REFERENCES web.users(user_id);
ALTER TABLE web."comments" ADD CONSTRAINT fk_comment_modifier FOREIGN KEY (modifier_id) REFERENCES web.users(user_id);
ALTER TABLE web."comments" ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES web."comments"(comment_id) ON DELETE CASCADE;
ALTER TABLE web."comments" ADD CONSTRAINT fk_comment_writer FOREIGN KEY (writer_id) REFERENCES web.users(user_id);
```
### Addresses 
``` sql
CREATE TABLE web.addresses (
	address_id varchar(36) NOT NULL,
	user_id varchar(36) NOT NULL,
	receiver varchar(50) NOT NULL,
	phone_number varchar(20) NOT NULL,
	postal_code varchar(10) NOT NULL,
	address text NOT NULL,
	is_default varchar(5) NOT NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT addresses_pkey PRIMARY KEY (address_id)
);

-- web.addresses foreign keys

ALTER TABLE web.addresses ADD CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES web.users(user_id) ON DELETE CASCADE;
```
### roles
``` sql
CREATE TABLE web.roles (
	role_id varchar(36) NOT NULL,
	role_name varchar(50) NOT NULL,
	role_description varchar(1000) NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT roles_pkey PRIMARY KEY (role_id)
);
```
### permissions
``` sql
CREATE TABLE web.permissions (
	permission_id varchar(36) NOT NULL,
	permission_name varchar(50) NOT NULL,
	permission_description varchar(1000) NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT permissions_pkey PRIMARY KEY (permission_id)
);
```
### user_roles
``` sql
CREATE TABLE web.user_roles (
	user_id varchar(36) NOT NULL,
	role_id varchar(36) NOT NULL,
	CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id)
);

-- web.user_roles foreign keys

ALTER TABLE web.user_roles ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES web.roles(role_id);
ALTER TABLE web.user_roles ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES web.users(user_id);
```
### role_permissions
``` sql
CREATE TABLE web.role_permissions (
	role_id varchar(36) NOT NULL,
	permission_id varchar(36) NOT NULL,
	CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id)
);

-- web.role_permissions foreign keys

ALTER TABLE web.role_permissions ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES web.permissions(permission_id);
ALTER TABLE web.role_permissions ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES web.roles(role_id);
```
### products
``` sql
CREATE TABLE web.products (
	product_id varchar(36) NOT NULL,
	product_name varchar(255) NOT NULL,
	product_description text NULL,
	price numeric(10, 2) NOT NULL,
	stock_quantity int4 DEFAULT 0 NOT NULL,
	category varchar(100) NULL,
	discount_rate int2 DEFAULT 0 NOT NULL,
	discount_amount numeric(10, 2) GENERATED ALWAYS AS (round(price * discount_rate::numeric / 100::numeric, 2)) STORED NULL,
	discount_price numeric(10, 2) GENERATED ALWAYS AS (round(price - price * discount_rate::numeric / 100::numeric, 2)) STORED NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT products_pkey PRIMARY KEY (product_id)
);
```
### bookmarks
``` sql
CREATE TABLE web.bookmarks (
	bookmark_id varchar(36) NOT NULL,
	user_id varchar(36) NOT NULL,
	target_id varchar(36) NOT NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT bookmarks_pkey PRIMARY KEY (bookmark_id)
);

-- web.bookmarks foreign keys

ALTER TABLE web.bookmarks ADD CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES web.users(user_id) ON DELETE CASCADE;
```
### carts
``` sql
CREATE TABLE web.carts (
	cart_id varchar(36) NOT NULL,
	user_id varchar(36) NOT NULL,
	product_id varchar(36) NOT NULL,
	quantity int4 NOT NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT carts_pkey PRIMARY KEY (cart_id),
	CONSTRAINT carts_quantity_check CHECK ((quantity > 0))
);

-- web.carts foreign keys

ALTER TABLE web.carts ADD CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES web.products(product_id) ON DELETE CASCADE;
ALTER TABLE web.carts ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES web.users(user_id) ON DELETE CASCADE;
```
### likes
``` sql   
CREATE TABLE web.likes (
	like_id varchar(36) NOT NULL,
	user_id varchar(36) NOT NULL,
	board_id varchar(36) NOT NULL,
	status varchar(10) NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT likes_pkey PRIMARY KEY (like_id),
	CONSTRAINT likes_status_check CHECK (((status)::text = ANY ((ARRAY['LIKE'::character varying, 'UNLIKE'::character varying])::text[]))),
	CONSTRAINT likes_user_id_board_id_key UNIQUE (user_id, board_id)
);
```
### files
``` sql
CREATE TABLE web.files (
	file_id varchar(36) NOT NULL,
	target_id varchar(36) NOT NULL,
	target_type varchar(50) NOT NULL,
	file_path varchar(255) NOT NULL,
	file_size int8 DEFAULT 0 NOT NULL,
	file_extension varchar(20) NULL,
	file_type varchar(100) NULL,
	original_file_name varchar(255) NULL,
	create_datetime timestamptz DEFAULT now() NOT NULL,
	creator_id varchar(36) NOT NULL,
	creator_login_id varchar(60) NOT NULL,
	creator_name varchar(50) NOT NULL,
	modify_datetime timestamptz DEFAULT now() NULL,
	modifier_id varchar(36) NULL,
	modifier_login_id varchar(60) NULL,
	modifier_name varchar(50) NULL,
	CONSTRAINT files_pkey PRIMARY KEY (file_id)
);

-- web.files foreign keys

ALTER TABLE web.files ADD CONSTRAINT fk_creator FOREIGN KEY (creator_id) REFERENCES web.users(user_id);
ALTER TABLE web.files ADD CONSTRAINT fk_modifier FOREIGN KEY (modifier_id) REFERENCES web.users(user_id);
```
### chats
``` sql
CREATE TABLE chats (
    chat_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    sender VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,   
    CONSTRAINT fk_chats_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);
```