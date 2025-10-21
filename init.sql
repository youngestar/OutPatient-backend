CREATE DATABASE IF NOT EXISTS hospital_management_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_management_system;

create table ai_consult_record
(
    record_id      bigint auto_increment comment '问诊记录ID'
        primary key,
    appointment_id bigint                             not null comment '关联的预约ID',
    patient_id     bigint                             not null comment '患者ID',
    conversation   text                               not null comment 'AI 对话内容(可存储 JSON)',
    status         tinyint  default 0                 not null comment '问诊状态(0-进行中,1-已结束)',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    version        int      default 0                 null comment '版本号，用于乐观锁',
    constraint uniq_ai_consult_appointment_id
        unique (appointment_id)
)
    comment 'AI 问诊记录表';

create index idx_ai_consult_patient_id
    on ai_consult_record (patient_id);

create index idx_ai_consult_status
    on ai_consult_record (status);

create table appointment
(
    appointment_id   bigint auto_increment comment '预约ID'
        primary key,
    patient_id       bigint                             not null comment '患者ID',
    doctor_id        bigint                             not null comment '医生ID',
    schedule_id      bigint                             not null comment '排班ID',
    appointment_date date                               not null comment '预约日期',
    time_slot        varchar(20)                        not null comment '预约时间段',
    is_revisit       tinyint  default 0                 not null comment '是否为复诊(0-初诊,1-复诊)',
    status           tinyint  default 0                 not null comment '预约状态(0-待就诊,1-已就诊,2-已取消等)',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '预约挂号表';

create index idx_appointment_date
    on appointment (appointment_date);

create index idx_appointment_doctor_id
    on appointment (doctor_id);

create index idx_appointment_is_revisit
    on appointment (is_revisit);

create index idx_appointment_patient_id
    on appointment (patient_id);

create index idx_appointment_schedule_id
    on appointment (schedule_id);

create index idx_appointment_status
    on appointment (status);

create table clinic
(
    clinic_id   bigint auto_increment comment '门诊ID'
        primary key,
    dept_id     bigint                             not null comment '所属科室ID',
    clinic_name varchar(50)                        not null comment '门诊名称',
    is_active   tinyint  default 1                 not null comment '是否有效(0-无效,1-有效)',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uniq_clinic_name_dept_id
        unique (clinic_name, dept_id)
)
    comment '门诊表';

create index idx_clinic_dept_id
    on clinic (dept_id);

create index idx_clinic_is_active
    on clinic (is_active);

create table department
(
    dept_id     bigint auto_increment comment '科室ID'
        primary key,
    dept_name   varchar(50)                        not null comment '科室名称',
    is_active   tinyint  default 1                 not null comment '是否有效(0-无效,1-有效)',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uniq_department_dept_name
        unique (dept_name)
)
    comment '医院科室表';

create index idx_department_is_active
    on department (is_active);

create table diagnosis
(
    diag_id          bigint auto_increment comment '诊断记录ID'
        primary key,
    appointment_id   bigint                             not null comment '预约ID',
    doctor_id        bigint                             not null comment '医生ID',
    patient_id       bigint                             not null comment '患者ID',
    diagnosis_result text                               null comment '诊断结果',
    examination      text                               null comment '检查记录',
    prescription     text                               null comment '处方信息(药品、数量、用法等)',
    advice           text                               null comment '医嘱',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uniq_diagnosis_appointment_id
        unique (appointment_id)
)
    comment '医生诊断记录表';

create index idx_diagnosis_doctor_id
    on diagnosis (doctor_id);

create index idx_diagnosis_patient_id
    on diagnosis (patient_id);

create table doctor
(
    doctor_id    bigint auto_increment comment '医生ID'
        primary key,
    user_id      bigint                             not null comment '关联用户ID',
    name         varchar(50)                        not null comment '医生姓名',
    clinic_id    bigint                             not null comment '所属门诊ID',
    title        varchar(50)                        null comment '职称(主任医师,副主任医师等)',
    introduction text                               null comment '医生简介',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '医生信息表';

create index idx_doctor_clinic_id
    on doctor (clinic_id);

create index idx_doctor_name
    on doctor (name);

create index idx_doctor_user_id
    on doctor (user_id);

create table feedback_message
(
    message_id  bigint auto_increment comment '消息ID'
        primary key,
    diag_id     bigint                             not null comment '关联的诊断记录ID',
    sender_type int                                null comment '发送者类型（0-患者，1-医生）',
    sender_id   bigint                             not null comment '发送者ID',
    content     text                               not null comment '消息内容',
    read_status tinyint  default 0                 not null comment '阅读状态(0-未读,1-已读)',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '诊后反馈消息表';

create index idx_feedback_message_diag_id
    on feedback_message (diag_id);

create index idx_feedback_message_read_status
    on feedback_message (read_status);

create index idx_feedback_message_sender_id
    on feedback_message (sender_id);

create table patient
(
    patient_id  bigint auto_increment comment '患者ID'
        primary key,
    user_id     bigint                             not null comment '关联用户ID',
    name        varchar(50)                        not null comment '患者姓名',
    id_card     varchar(18)                        null comment '身份证号',
    gender      tinyint  default 0                 not null comment '性别(0-未知,1-男,2-女)',
    age         int                                null comment '年龄',
    region      varchar(50)                        null comment '地区(省市区)',
    address     varchar(100)                       null comment '详细住址',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '患者信息表';

create index idx_patient_id_card
    on patient (id_card);

create index idx_patient_name
    on patient (name);

create index idx_patient_region
    on patient (region);

create index idx_patient_user_id
    on patient (user_id);

create table schedule
(
    schedule_id      bigint auto_increment comment '排班ID'
        primary key,
    doctor_id        bigint                             not null comment '医生ID',
    clinic_id        bigint                             not null comment '门诊ID',
    schedule_date    date                               not null comment '排班日期',
    time_slot        varchar(20)                        not null comment '时间段(如 08:00-12:00)',
    max_patients     int      default 10                not null comment '该时段可挂号最大人数',
    current_patients int      default 0                 not null comment '当前已预约人数',
    status           tinyint  default 1                 not null comment '排班状态(0-无效,1-有效)',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uniq_schedule_doctor_date_time
        unique (doctor_id, schedule_date, time_slot)
)
    comment '医生排班表';

create index idx_schedule_clinic_id
    on schedule (clinic_id);

create index idx_schedule_date
    on schedule (schedule_date);

create index idx_schedule_doctor_id
    on schedule (doctor_id);

create index idx_schedule_status
    on schedule (status);

create table user
(
    id          bigint auto_increment comment '用户ID'
        primary key,
    username    varchar(50)                        null comment '用户名',
    password    varchar(100)                       null comment '密码(加密后)',
    phone       varchar(20)                        null comment '手机号',
    email       varchar(50)                        null comment '邮箱',
    role        tinyint  default 0                 not null comment '用户角色(0-患者,1-医生,2-管理员)',
    avatar      varchar(255)                       null comment '用户头像URL',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '用户基本信息表';

create index idx_user_email
    on user (email);

create index idx_user_phone
    on user (phone);

create index idx_user_username
    on user (username);

