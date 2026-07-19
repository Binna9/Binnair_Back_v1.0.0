## WEB TABLE DDL

스키마: `web`  
공통 감사 컬럼 규칙:
- `create_datetime` / `creator_*` : 생성 시각·생성자
- `modify_datetime` / `modifier_*` : 수정 시각·수정자 (nullable)

---

### users

시스템 사용자 마스터.

```sql
CREATE TABLE web.users (
    user_id               varchar(36)  NOT NULL,
    user_name             varchar(50)  NOT NULL,
    login_id              varchar(60)  NOT NULL,
    login_password        varchar(100) NOT NULL,
    provider_id           varchar(36)  NULL,
    provider              varchar(30)  NULL,
    failed_login_attempts numeric(3)   NULL,
    email                 varchar(50)  NULL,
    phone_number          varchar(20)  NULL,
    nick_name             varchar(50)  NULL,
    is_active             varchar(1)   NOT NULL,
    create_datetime       timestamptz  DEFAULT now() NOT NULL,
    creator_id            varchar(36)  NOT NULL,
    creator_login_id      varchar(60)  NOT NULL,
    creator_name          varchar(50)  NOT NULL,
    modify_datetime       timestamptz  DEFAULT now() NULL,
    modifier_id           varchar(36)  NULL,
    modifier_login_id     varchar(60)  NULL,
    modifier_name         varchar(50)  NULL,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT chk_users_is_active CHECK (is_active IN ('Y', 'N', 'L'))
);

CREATE UNIQUE INDEX idx_users_login_id ON web.users (login_id);

COMMENT ON TABLE web.users IS '시스템 사용자 마스터. 로컬/OAuth 로그인 계정과 권한 부여의 기준 테이블';
COMMENT ON COLUMN web.users.user_id IS '사용자 PK (UUID)';
COMMENT ON COLUMN web.users.user_name IS '사용자 실명';
COMMENT ON COLUMN web.users.login_id IS '로그인 아이디 (UNIQUE)';
COMMENT ON COLUMN web.users.login_password IS '암호화된 로그인 비밀번호. OAuth 전용 계정은 더미/미사용 가능';
COMMENT ON COLUMN web.users.provider_id IS 'OAuth 제공자 측 사용자 ID';
COMMENT ON COLUMN web.users.provider IS 'OAuth 제공자 코드 (google, kakao, naver, SYSTEM 등)';
COMMENT ON COLUMN web.users.failed_login_attempts IS '연속 로그인 실패 횟수. 잠금 정책에 사용';
COMMENT ON COLUMN web.users.email IS '이메일';
COMMENT ON COLUMN web.users.phone_number IS '전화번호';
COMMENT ON COLUMN web.users.nick_name IS '닉네임';
COMMENT ON COLUMN web.users.is_active IS '계정 상태: Y=활성, N=비활성, L=잠금';
COMMENT ON COLUMN web.users.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.users.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.users.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.users.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.users.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.users.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.users.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.users.modifier_name IS '수정자 표시명';

-- 시스템 계정 시드
INSERT INTO web.users (
    user_id, user_name, login_id, login_password,
    failed_login_attempts, email, phone_number,
    create_datetime, creator_id, creator_login_id, creator_name,
    modify_datetime, modifier_id, modifier_login_id, modifier_name,
    nick_name, is_active, provider_id, provider
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    'system', 'system', 'system',
    0, 'system@system.com', '000-0000-0000',
    now(), '00000000-0000-0000-0000-000000000000', 'system', 'system',
    now(), '00000000-0000-0000-0000-000000000000', 'system', 'system',
    'system', 'Y', 'SYSTEM', 'SYSTEM'
);
```

---

### boards

게시판 게시글.

```sql
CREATE TABLE web.boards (
    board_id          varchar(36)  NOT NULL,
    board_type        varchar(10)  NOT NULL,
    title             varchar(255) NOT NULL,
    content           text         NOT NULL,
    views             int4         DEFAULT 0 NOT NULL,
    likes             int4         DEFAULT 0 NOT NULL,
    unlikes           int4         DEFAULT 0 NOT NULL,
    writer_id         varchar(36)  NOT NULL,
    writer_name       varchar(30)  NOT NULL,
    create_datetime   timestamptz  DEFAULT now() NOT NULL,
    creator_id        varchar(36)  NOT NULL,
    creator_login_id  varchar(60)  NOT NULL,
    creator_name      varchar(50)  NOT NULL,
    modify_datetime   timestamptz  DEFAULT now() NULL,
    modifier_id       varchar(36)  NULL,
    modifier_login_id varchar(60)  NULL,
    modifier_name     varchar(50)  NULL,
    CONSTRAINT boards_pkey PRIMARY KEY (board_id)
);

ALTER TABLE web.boards ADD CONSTRAINT fk_board_creator FOREIGN KEY (creator_id) REFERENCES web.users(user_id);
ALTER TABLE web.boards ADD CONSTRAINT fk_board_modifier FOREIGN KEY (modifier_id) REFERENCES web.users(user_id);
ALTER TABLE web.boards ADD CONSTRAINT fk_board_writer FOREIGN KEY (writer_id) REFERENCES web.users(user_id);

COMMENT ON TABLE web.boards IS '게시판 게시글. board_type으로 게시판 종류를 구분';
COMMENT ON COLUMN web.boards.board_id IS '게시글 PK (UUID)';
COMMENT ON COLUMN web.boards.board_type IS '게시판 유형 코드';
COMMENT ON COLUMN web.boards.title IS '제목';
COMMENT ON COLUMN web.boards.content IS '본문';
COMMENT ON COLUMN web.boards.views IS '조회수';
COMMENT ON COLUMN web.boards.likes IS '좋아요 수 (집계 캐시)';
COMMENT ON COLUMN web.boards.unlikes IS '싫어요 수 (집계 캐시)';
COMMENT ON COLUMN web.boards.writer_id IS '작성자 user_id';
COMMENT ON COLUMN web.boards.writer_name IS '작성 시점 작성자 표시명 스냅샷';
COMMENT ON COLUMN web.boards.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.boards.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.boards.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.boards.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.boards.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.boards.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.boards.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.boards.modifier_name IS '수정자 표시명';
```

---

### comments

게시글 댓글/대댓글.

```sql
CREATE TABLE web.comments (
    comment_id        varchar(36) NOT NULL,
    board_id          varchar(36) NOT NULL,
    parent_id         varchar(36) NULL,
    writer_id         varchar(36) NOT NULL,
    writer_name       varchar(30) NOT NULL,
    content           text        NOT NULL,
    create_datetime   timestamptz DEFAULT now() NOT NULL,
    creator_id        varchar(36) NOT NULL,
    creator_login_id  varchar(60) NOT NULL,
    creator_name      varchar(50) NOT NULL,
    modify_datetime   timestamptz DEFAULT now() NULL,
    modifier_id       varchar(36) NULL,
    modifier_login_id varchar(60) NULL,
    modifier_name     varchar(50) NULL,
    CONSTRAINT comments_pkey PRIMARY KEY (comment_id)
);

ALTER TABLE web.comments ADD CONSTRAINT fk_comment_board FOREIGN KEY (board_id) REFERENCES web.boards(board_id) ON DELETE CASCADE;
ALTER TABLE web.comments ADD CONSTRAINT fk_comment_creator FOREIGN KEY (creator_id) REFERENCES web.users(user_id);
ALTER TABLE web.comments ADD CONSTRAINT fk_comment_modifier FOREIGN KEY (modifier_id) REFERENCES web.users(user_id);
ALTER TABLE web.comments ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES web.comments(comment_id) ON DELETE CASCADE;
ALTER TABLE web.comments ADD CONSTRAINT fk_comment_writer FOREIGN KEY (writer_id) REFERENCES web.users(user_id);

COMMENT ON TABLE web.comments IS '게시글 댓글. parent_id가 있으면 대댓글';
COMMENT ON COLUMN web.comments.comment_id IS '댓글 PK (UUID)';
COMMENT ON COLUMN web.comments.board_id IS '소속 게시글 ID';
COMMENT ON COLUMN web.comments.parent_id IS '부모 댓글 ID. NULL이면 최상위 댓글';
COMMENT ON COLUMN web.comments.writer_id IS '작성자 user_id';
COMMENT ON COLUMN web.comments.writer_name IS '작성 시점 작성자 표시명 스냅샷';
COMMENT ON COLUMN web.comments.content IS '댓글 본문';
COMMENT ON COLUMN web.comments.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.comments.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.comments.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.comments.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.comments.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.comments.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.comments.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.comments.modifier_name IS '수정자 표시명';
```

---

### addresses

사용자 배송지.

```sql
CREATE TABLE web.addresses (
    address_id        varchar(36) NOT NULL,
    user_id           varchar(36) NOT NULL,
    receiver          varchar(50) NOT NULL,
    phone_number      varchar(20) NOT NULL,
    postal_code       varchar(10) NOT NULL,
    address           text        NOT NULL,
    is_default        varchar(5)  NOT NULL,
    create_datetime   timestamptz DEFAULT now() NOT NULL,
    creator_id        varchar(36) NOT NULL,
    creator_login_id  varchar(60) NOT NULL,
    creator_name      varchar(50) NOT NULL,
    modify_datetime   timestamptz DEFAULT now() NULL,
    modifier_id       varchar(36) NULL,
    modifier_login_id varchar(60) NULL,
    modifier_name     varchar(50) NULL,
    CONSTRAINT addresses_pkey PRIMARY KEY (address_id)
);

ALTER TABLE web.addresses ADD CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES web.users(user_id) ON DELETE CASCADE;

COMMENT ON TABLE web.addresses IS '사용자 배송지 목록';
COMMENT ON COLUMN web.addresses.address_id IS '배송지 PK (UUID)';
COMMENT ON COLUMN web.addresses.user_id IS '소유 사용자 ID';
COMMENT ON COLUMN web.addresses.receiver IS '수령인 이름';
COMMENT ON COLUMN web.addresses.phone_number IS '수령인 연락처';
COMMENT ON COLUMN web.addresses.postal_code IS '우편번호';
COMMENT ON COLUMN web.addresses.address IS '상세 주소';
COMMENT ON COLUMN web.addresses.is_default IS '기본 배송지 여부';
COMMENT ON COLUMN web.addresses.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.addresses.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.addresses.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.addresses.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.addresses.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.addresses.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.addresses.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.addresses.modifier_name IS '수정자 표시명';
```

---

### roles

역할(권한 그룹) 마스터.

```sql
CREATE TABLE web.roles (
    role_id           varchar(36)   NOT NULL,
    role_name         varchar(50)   NOT NULL,
    role_description  varchar(1000) NULL,
    create_datetime   timestamptz   DEFAULT now() NOT NULL,
    creator_id        varchar(36)   NOT NULL,
    creator_login_id  varchar(60)   NOT NULL,
    creator_name      varchar(50)   NOT NULL,
    modify_datetime   timestamptz   DEFAULT now() NULL,
    modifier_id       varchar(36)   NULL,
    modifier_login_id varchar(60)   NULL,
    modifier_name     varchar(50)   NULL,
    CONSTRAINT roles_pkey PRIMARY KEY (role_id)
);

COMMENT ON TABLE web.roles IS '역할(권한 그룹) 마스터. 사용자·권한과 N:N로 연결';
COMMENT ON COLUMN web.roles.role_id IS '역할 PK (UUID)';
COMMENT ON COLUMN web.roles.role_name IS '역할 이름';
COMMENT ON COLUMN web.roles.role_description IS '역할 설명';
COMMENT ON COLUMN web.roles.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.roles.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.roles.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.roles.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.roles.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.roles.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.roles.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.roles.modifier_name IS '수정자 표시명';
```

---

### permissions

개별 권한 마스터.

```sql
CREATE TABLE web.permissions (
    permission_id          varchar(36)   NOT NULL,
    permission_name        varchar(50)   NOT NULL,
    permission_description varchar(1000) NULL,
    create_datetime        timestamptz   DEFAULT now() NOT NULL,
    creator_id             varchar(36)   NOT NULL,
    creator_login_id       varchar(60)   NOT NULL,
    creator_name           varchar(50)   NOT NULL,
    modify_datetime        timestamptz   DEFAULT now() NULL,
    modifier_id            varchar(36)   NULL,
    modifier_login_id      varchar(60)   NULL,
    modifier_name          varchar(50)   NULL,
    CONSTRAINT permissions_pkey PRIMARY KEY (permission_id)
);

COMMENT ON TABLE web.permissions IS '개별 권한 마스터. 역할에 묶여 사용자에게 부여';
COMMENT ON COLUMN web.permissions.permission_id IS '권한 PK (UUID)';
COMMENT ON COLUMN web.permissions.permission_name IS '권한 이름/코드';
COMMENT ON COLUMN web.permissions.permission_description IS '권한 설명';
COMMENT ON COLUMN web.permissions.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.permissions.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.permissions.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.permissions.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.permissions.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.permissions.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.permissions.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.permissions.modifier_name IS '수정자 표시명';
```

---

### user_roles

사용자-역할 매핑.

```sql
CREATE TABLE web.user_roles (
    user_id varchar(36) NOT NULL,
    role_id varchar(36) NOT NULL,
    CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id)
);

ALTER TABLE web.user_roles ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES web.roles(role_id);
ALTER TABLE web.user_roles ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES web.users(user_id);

COMMENT ON TABLE web.user_roles IS '사용자와 역할의 N:N 매핑';
COMMENT ON COLUMN web.user_roles.user_id IS '사용자 ID';
COMMENT ON COLUMN web.user_roles.role_id IS '역할 ID';
```

---

### role_permissions

역할-권한 매핑.

```sql
CREATE TABLE web.role_permissions (
    role_id       varchar(36) NOT NULL,
    permission_id varchar(36) NOT NULL,
    CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id)
);

ALTER TABLE web.role_permissions ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES web.permissions(permission_id);
ALTER TABLE web.role_permissions ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES web.roles(role_id);

COMMENT ON TABLE web.role_permissions IS '역할과 권한의 N:N 매핑';
COMMENT ON COLUMN web.role_permissions.role_id IS '역할 ID';
COMMENT ON COLUMN web.role_permissions.permission_id IS '권한 ID';
```

---

### products

상품 마스터.

```sql
CREATE TABLE web.products (
    product_id          varchar(36)    NOT NULL,
    product_name        varchar(255)   NOT NULL,
    product_description text           NULL,
    price               numeric(10, 2) NOT NULL,
    stock_quantity      int4           DEFAULT 0 NOT NULL,
    category            varchar(100)   NULL,
    discount_rate       int2           DEFAULT 0 NOT NULL,
    discount_amount     numeric(10, 2) GENERATED ALWAYS AS (round(price * discount_rate::numeric / 100::numeric, 2)) STORED NULL,
    discount_price      numeric(10, 2) GENERATED ALWAYS AS (round(price - price * discount_rate::numeric / 100::numeric, 2)) STORED NULL,
    create_datetime     timestamptz    DEFAULT now() NOT NULL,
    creator_id          varchar(36)    NOT NULL,
    creator_login_id    varchar(60)    NOT NULL,
    creator_name        varchar(50)    NOT NULL,
    modify_datetime     timestamptz    DEFAULT now() NULL,
    modifier_id         varchar(36)    NULL,
    modifier_login_id   varchar(60)    NULL,
    modifier_name       varchar(50)    NULL,
    CONSTRAINT products_pkey PRIMARY KEY (product_id)
);

COMMENT ON TABLE web.products IS '상품 마스터. 할인액/할인가격은 discount_rate 기반 생성 컬럼';
COMMENT ON COLUMN web.products.product_id IS '상품 PK (UUID)';
COMMENT ON COLUMN web.products.product_name IS '상품명';
COMMENT ON COLUMN web.products.product_description IS '상품 설명';
COMMENT ON COLUMN web.products.price IS '정가';
COMMENT ON COLUMN web.products.stock_quantity IS '재고 수량';
COMMENT ON COLUMN web.products.category IS '카테고리';
COMMENT ON COLUMN web.products.discount_rate IS '할인율 (%). 0~100 권장';
COMMENT ON COLUMN web.products.discount_amount IS '할인 금액 (생성 컬럼: price * discount_rate / 100)';
COMMENT ON COLUMN web.products.discount_price IS '할인 적용가 (생성 컬럼: price - discount_amount)';
COMMENT ON COLUMN web.products.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.products.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.products.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.products.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.products.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.products.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.products.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.products.modifier_name IS '수정자 표시명';
```

---

### bookmarks

사용자 북마크(즐겨찾기).

```sql
CREATE TABLE web.bookmarks (
    bookmark_id       varchar(36) NOT NULL,
    user_id           varchar(36) NOT NULL,
    target_id         varchar(36) NOT NULL,
    create_datetime   timestamptz DEFAULT now() NOT NULL,
    creator_id        varchar(36) NOT NULL,
    creator_login_id  varchar(60) NOT NULL,
    creator_name      varchar(50) NOT NULL,
    modify_datetime   timestamptz DEFAULT now() NULL,
    modifier_id       varchar(36) NULL,
    modifier_login_id varchar(60) NULL,
    modifier_name     varchar(50) NULL,
    CONSTRAINT bookmarks_pkey PRIMARY KEY (bookmark_id)
);

ALTER TABLE web.bookmarks ADD CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES web.users(user_id) ON DELETE CASCADE;

COMMENT ON TABLE web.bookmarks IS '사용자 북마크. target_id는 대상 리소스 ID(게시글 등)';
COMMENT ON COLUMN web.bookmarks.bookmark_id IS '북마크 PK (UUID)';
COMMENT ON COLUMN web.bookmarks.user_id IS '소유 사용자 ID';
COMMENT ON COLUMN web.bookmarks.target_id IS '북마크 대상 ID';
COMMENT ON COLUMN web.bookmarks.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.bookmarks.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.bookmarks.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.bookmarks.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.bookmarks.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.bookmarks.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.bookmarks.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.bookmarks.modifier_name IS '수정자 표시명';
```

---

### carts

장바구니.

```sql
CREATE TABLE web.carts (
    cart_id           varchar(36) NOT NULL,
    user_id           varchar(36) NOT NULL,
    product_id        varchar(36) NOT NULL,
    quantity          int4        NOT NULL,
    create_datetime   timestamptz DEFAULT now() NOT NULL,
    creator_id        varchar(36) NOT NULL,
    creator_login_id  varchar(60) NOT NULL,
    creator_name      varchar(50) NOT NULL,
    modify_datetime   timestamptz DEFAULT now() NULL,
    modifier_id       varchar(36) NULL,
    modifier_login_id varchar(60) NULL,
    modifier_name     varchar(50) NULL,
    CONSTRAINT carts_pkey PRIMARY KEY (cart_id),
    CONSTRAINT carts_quantity_check CHECK (quantity > 0)
);

ALTER TABLE web.carts ADD CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES web.products(product_id) ON DELETE CASCADE;
ALTER TABLE web.carts ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES web.users(user_id) ON DELETE CASCADE;

COMMENT ON TABLE web.carts IS '사용자 장바구니 항목';
COMMENT ON COLUMN web.carts.cart_id IS '장바구니 항목 PK (UUID)';
COMMENT ON COLUMN web.carts.user_id IS '소유 사용자 ID';
COMMENT ON COLUMN web.carts.product_id IS '상품 ID';
COMMENT ON COLUMN web.carts.quantity IS '수량 (1 이상)';
COMMENT ON COLUMN web.carts.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.carts.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.carts.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.carts.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.carts.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.carts.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.carts.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.carts.modifier_name IS '수정자 표시명';
```

---

### likes

게시글 좋아요/싫어요.

```sql
CREATE TABLE web.likes (
    like_id           varchar(36) NOT NULL,
    user_id           varchar(36) NOT NULL,
    board_id          varchar(36) NOT NULL,
    status            varchar(10) NULL,
    create_datetime   timestamptz DEFAULT now() NOT NULL,
    creator_id        varchar(36) NOT NULL,
    creator_login_id  varchar(60) NOT NULL,
    creator_name      varchar(50) NOT NULL,
    modify_datetime   timestamptz DEFAULT now() NULL,
    modifier_id       varchar(36) NULL,
    modifier_login_id varchar(60) NULL,
    modifier_name     varchar(50) NULL,
    CONSTRAINT likes_pkey PRIMARY KEY (like_id),
    CONSTRAINT likes_status_check CHECK (status::text = ANY (ARRAY['LIKE'::varchar, 'UNLIKE'::varchar]::text[])),
    CONSTRAINT likes_user_id_board_id_key UNIQUE (user_id, board_id)
);

COMMENT ON TABLE web.likes IS '게시글에 대한 사용자별 좋아요/싫어요. 사용자당 게시글 1건';
COMMENT ON COLUMN web.likes.like_id IS '좋아요 레코드 PK (UUID)';
COMMENT ON COLUMN web.likes.user_id IS '사용자 ID';
COMMENT ON COLUMN web.likes.board_id IS '게시글 ID';
COMMENT ON COLUMN web.likes.status IS '반응 상태: LIKE | UNLIKE';
COMMENT ON COLUMN web.likes.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.likes.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.likes.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.likes.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.likes.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.likes.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.likes.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.likes.modifier_name IS '수정자 표시명';
```

---

### files

업로드 파일 메타.

```sql
CREATE TABLE web.files (
    file_id            varchar(36)  NOT NULL,
    target_id          varchar(36)  NOT NULL,
    target_type        varchar(50)  NOT NULL,
    file_path          varchar(255) NOT NULL,
    file_size          int8         DEFAULT 0 NOT NULL,
    file_extension     varchar(20)  NULL,
    file_type          varchar(100) NULL,
    original_file_name varchar(255) NULL,
    create_datetime    timestamptz  DEFAULT now() NOT NULL,
    creator_id         varchar(36)  NOT NULL,
    creator_login_id   varchar(60)  NOT NULL,
    creator_name       varchar(50)  NOT NULL,
    modify_datetime    timestamptz  DEFAULT now() NULL,
    modifier_id        varchar(36)  NULL,
    modifier_login_id  varchar(60)  NULL,
    modifier_name      varchar(50)  NULL,
    CONSTRAINT files_pkey PRIMARY KEY (file_id)
);

ALTER TABLE web.files ADD CONSTRAINT fk_creator FOREIGN KEY (creator_id) REFERENCES web.users(user_id);
ALTER TABLE web.files ADD CONSTRAINT fk_modifier FOREIGN KEY (modifier_id) REFERENCES web.users(user_id);

COMMENT ON TABLE web.files IS '업로드 파일 메타. 실제 바이너리는 스토리지 경로(file_path)에 저장';
COMMENT ON COLUMN web.files.file_id IS '파일 PK (UUID)';
COMMENT ON COLUMN web.files.target_id IS '파일이 연결된 대상 ID (user/product/board 등)';
COMMENT ON COLUMN web.files.target_type IS '대상 유형 코드 (USER, PRODUCT, BOARD 등)';
COMMENT ON COLUMN web.files.file_path IS '스토리지 상대/절대 경로';
COMMENT ON COLUMN web.files.file_size IS '파일 크기 (bytes)';
COMMENT ON COLUMN web.files.file_extension IS '확장자';
COMMENT ON COLUMN web.files.file_type IS 'MIME 타입 또는 파일 분류';
COMMENT ON COLUMN web.files.original_file_name IS '원본 파일명';
COMMENT ON COLUMN web.files.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.files.creator_id IS '생성자 user_id';
COMMENT ON COLUMN web.files.creator_login_id IS '생성자 login_id';
COMMENT ON COLUMN web.files.creator_name IS '생성자 표시명';
COMMENT ON COLUMN web.files.modify_datetime IS '최종 수정 시각';
COMMENT ON COLUMN web.files.modifier_id IS '수정자 user_id';
COMMENT ON COLUMN web.files.modifier_login_id IS '수정자 login_id';
COMMENT ON COLUMN web.files.modifier_name IS '수정자 표시명';
```

---

### chats

웹소켓 채팅 메시지.

```sql
CREATE TABLE web.chats (
    chat_id   varchar(36)  NOT NULL,
    user_id   varchar(36)  NOT NULL,
    sender    varchar(255) NOT NULL,
    content   text         NOT NULL,
    timestamp timestamp    NOT NULL,
    CONSTRAINT chats_pkey PRIMARY KEY (chat_id)
);

ALTER TABLE web.chats ADD CONSTRAINT fk_chats_user
    FOREIGN KEY (user_id) REFERENCES web.users(user_id) ON DELETE CASCADE;

COMMENT ON TABLE web.chats IS '채팅 메시지 이력. WebSocket 핸들러가 적재';
COMMENT ON COLUMN web.chats.chat_id IS '채팅 메시지 PK (UUID)';
COMMENT ON COLUMN web.chats.user_id IS '메시지 소유/발신 사용자 ID';
COMMENT ON COLUMN web.chats.sender IS '발신자 표시명';
COMMENT ON COLUMN web.chats.content IS '메시지 본문';
COMMENT ON COLUMN web.chats.timestamp IS '메시지 시각';
```

---

### market_symbols

anomaly 실시간 Writer / 필터용 거래소 심볼 마스터.  
기존 `core.venues` / `core.instruments` / `core.venue_symbols`를 대체한다.

API 매핑:
- `venueId` = `venue_id`
- `instrumentId` = `symbol_id`

```sql
CREATE TABLE web.market_symbols (
    symbol_id       BIGSERIAL    PRIMARY KEY,
    venue_id        BIGINT       NOT NULL,
    venue_code      TEXT         NOT NULL,
    venue_symbol    TEXT         NOT NULL,
    display_symbol  TEXT         NOT NULL,
    venue_type      TEXT         NOT NULL DEFAULT 'exchange',
    asset_class     TEXT         NOT NULL DEFAULT 'crypto',
    base_asset      TEXT         NULL,
    quote_asset     TEXT         NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    create_datetime TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modify_datetime TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_market_symbols_venue_symbol UNIQUE (venue_code, venue_symbol)
);

CREATE INDEX idx_market_symbols_active
    ON web.market_symbols (is_active, venue_code);

CREATE INDEX idx_market_symbols_venue_id
    ON web.market_symbols (venue_id, symbol_id);

COMMENT ON TABLE web.market_symbols IS '거래소 심볼 마스터. anomaly realtime Writer 수집 대상과 필터 API(/anomaly/filter)의 단일 소스';
COMMENT ON COLUMN web.market_symbols.symbol_id IS 'PK. REST path의 instrumentId로 사용';
COMMENT ON COLUMN web.market_symbols.venue_id IS '논리 거래소 ID. REST path의 venueId로 사용 (예: binance=1)';
COMMENT ON COLUMN web.market_symbols.venue_code IS '거래소 코드 (binance, upbit 등). MarketDataClient 라우팅 키';
COMMENT ON COLUMN web.market_symbols.venue_symbol IS '거래소 API 원본 심볼 (예: BTCUSDT)';
COMMENT ON COLUMN web.market_symbols.display_symbol IS '화면/응답용 심볼 (예: BTC/USDT)';
COMMENT ON COLUMN web.market_symbols.venue_type IS '거래소 유형 (exchange / broker / data_vendor)';
COMMENT ON COLUMN web.market_symbols.asset_class IS '자산군 (crypto / equity / fx / index / commodity 등)';
COMMENT ON COLUMN web.market_symbols.base_asset IS '기초자산 코드 (예: BTC)';
COMMENT ON COLUMN web.market_symbols.quote_asset IS '상대자산 코드 (예: USDT)';
COMMENT ON COLUMN web.market_symbols.is_active IS '활성 여부. true만 Writer 수집·필터 목록에 포함';
COMMENT ON COLUMN web.market_symbols.create_datetime IS '생성 시각';
COMMENT ON COLUMN web.market_symbols.modify_datetime IS '최종 수정 시각';

-- 샘플 (binance)
INSERT INTO web.market_symbols (
    venue_id, venue_code, venue_symbol, display_symbol,
    venue_type, asset_class, base_asset, quote_asset, is_active
) VALUES
    (1, 'binance', 'BTCUSDT', 'BTC/USDT', 'exchange', 'crypto', 'BTC', 'USDT', TRUE),
    (1, 'binance', 'ETHUSDT', 'ETH/USDT', 'exchange', 'crypto', 'ETH', 'USDT', TRUE),
    (1, 'binance', 'SOLUSDT', 'SOL/USDT', 'exchange', 'crypto', 'SOL', 'USDT', TRUE)
ON CONFLICT (venue_code, venue_symbol) DO NOTHING;
```
