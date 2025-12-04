## TABLE DDL
``` sql
## users

CREATE TABLE public.users (
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

--- 최초 system 계정 ---
INSERT INTO public.users
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


## boards
 
CREATE TABLE public.boards (
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

-- public.boards foreign keys

ALTER TABLE public.boards ADD CONSTRAINT fk_board_creator FOREIGN KEY (creator_id) REFERENCES public.users(user_id);
ALTER TABLE public.boards ADD CONSTRAINT fk_board_modifier FOREIGN KEY (modifier_id) REFERENCES public.users(user_id);
ALTER TABLE public.boards ADD CONSTRAINT fk_board_writer FOREIGN KEY (writer_id) REFERENCES public.users(user_id);

## comments

CREATE TABLE public."comments" (
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

-- public."comments" foreign keys

ALTER TABLE public."comments" ADD CONSTRAINT fk_comment_board FOREIGN KEY (board_id) REFERENCES public.boards(board_id) ON DELETE CASCADE;
ALTER TABLE public."comments" ADD CONSTRAINT fk_comment_creator FOREIGN KEY (creator_id) REFERENCES public.users(user_id);
ALTER TABLE public."comments" ADD CONSTRAINT fk_comment_modifier FOREIGN KEY (modifier_id) REFERENCES public.users(user_id);
ALTER TABLE public."comments" ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES public."comments"(comment_id) ON DELETE CASCADE;
ALTER TABLE public."comments" ADD CONSTRAINT fk_comment_writer FOREIGN KEY (writer_id) REFERENCES public.users(user_id);

## Addresses 

CREATE TABLE public.addresses (
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

-- public.addresses foreign keys

ALTER TABLE public.addresses ADD CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

## roles

CREATE TABLE public.roles (
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

## permissions

CREATE TABLE public.permissions (
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

## user_roles

CREATE TABLE public.user_roles (
	user_id varchar(36) NOT NULL,
	role_id varchar(36) NOT NULL,
	CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id)
);

-- public.user_roles foreign keys

ALTER TABLE public.user_roles ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(role_id);
ALTER TABLE public.user_roles ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

## role_permissions

CREATE TABLE public.role_permissions (
	role_id varchar(36) NOT NULL,
	permission_id varchar(36) NOT NULL,
	CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id)
);

-- public.role_permissions foreign keys

ALTER TABLE public.role_permissions ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(permission_id);
ALTER TABLE public.role_permissions ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(role_id);

## products

CREATE TABLE public.products (
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

## bookmarks

CREATE TABLE public.bookmarks (
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

-- public.bookmarks foreign keys

ALTER TABLE public.bookmarks ADD CONSTRAINT fk_bookmark_product FOREIGN KEY (product_id) REFERENCES public.products(product_id) ON DELETE CASCADE;
ALTER TABLE public.bookmarks ADD CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

## carts

CREATE TABLE public.carts (
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

-- public.carts foreign keys

ALTER TABLE public.carts ADD CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES public.products(product_id) ON DELETE CASCADE;
ALTER TABLE public.carts ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

## likes
   
CREATE TABLE public.likes (
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

## files

CREATE TABLE public.files (
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

-- public.files foreign keys

ALTER TABLE public.files ADD CONSTRAINT fk_creator FOREIGN KEY (creator_id) REFERENCES public.users(user_id);
ALTER TABLE public.files ADD CONSTRAINT fk_modifier FOREIGN KEY (modifier_id) REFERENCES public.users(user_id);

## chats

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