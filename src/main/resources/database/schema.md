## TABLE DDL

``` sql
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
    modifier_name    character varying(50) NOT NULL
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