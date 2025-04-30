-- 创建库
create database yu_picture;

-- 切换库
use yu_picture;

-- 用户表
drop table if exists user;
create table user
(
    id            bigint auto_increment comment '主键'
        primary key,
    user_account  varchar(256)                           not null comment '账号',
    user_password varchar(512)                           not null comment '密码',
    user_name     varchar(256)                           null comment '用户昵称',
    user_avatar   varchar(1024)                          null comment '用户头像',
    user_profile  varchar(512)                           null comment '用户简介',
    user_role     varchar(256) default 'user'            not null comment '用户角色：user / admin',
    edit_time     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0                 not null comment '是否删除',
    constraint uk_userAccount
        unique (user_account)
)
    comment '用户表' collate = utf8mb4_unicode_ci;

create index idx_userName
    on user (user_name)
    comment '索引_用户昵称';
