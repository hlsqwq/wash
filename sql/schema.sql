-- 蜂窝盘模型库初始化脚本（幂等）。
-- 只创建缺失的表，不删除、不清空任何已有数据。
-- 用法： mysql -h 10.32.22.200 -P 3306 -uroot -p < sql/schema.sql

CREATE DATABASE IF NOT EXISTS wash
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE wash;

CREATE TABLE IF NOT EXISTS model (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    code                 VARCHAR(64)  NOT NULL COMMENT '稳定模型编码，例如 e107',
    name                 VARCHAR(128) NOT NULL COMMENT '模型显示名称',
    image_id             VARCHAR(64)  NULL COMMENT '关联 media.id（MD5）',
    dx                   INT          NOT NULL COMMENT '横向一个管间距的脉冲数',
    dy                   INT          NOT NULL COMMENT '纵向一个行间距的脉冲数',
    total_row            INT          NOT NULL COMMENT '总行数，由行数组计算',
    row_len              LONGTEXT     NOT NULL COMMENT '每行管数 JSON 数组',
    row_offset           LONGTEXT     NOT NULL COMMENT '每行首点偏移 JSON 数组',
    hide_holes           LONGTEXT     NULL COMMENT '隐藏孔位 JSON 数组',
    block_holes          LONGTEXT     NULL COMMENT '堵塞孔位 JSON 数组',
    is_vertical_partition TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '竖向隔断，与 wide_center_gap 语义不同',
    is_horizon_partition TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否存在横向隔断',
    horizon_partition    LONGTEXT     NULL COMMENT '横向隔断 JSON 数组',
    wide_center_gap      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '盘面中间加宽',
    description          VARCHAR(255) NULL,
    version              INT          NOT NULL DEFAULT 1,
    enabled              TINYINT(1)   NOT NULL DEFAULT 1,
    create_time          DATETIME     NOT NULL,
    update_time          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_code (code),
    KEY idx_model_enabled (enabled),
    KEY idx_model_image (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜂窝盘模型';

CREATE TABLE IF NOT EXISTS media (
    id           VARCHAR(64)   NOT NULL COMMENT '文件内容 MD5',
    name         VARCHAR(255)  NULL COMMENT '原始文件名',
    type         VARCHAR(16)   NOT NULL DEFAULT 'pic' COMMENT 'pic/video',
    file_size    BIGINT        NULL,
    url          VARCHAR(1024) NULL COMMENT '后端访问路径',
    bucket       VARCHAR(128)  NULL COMMENT 'RustFS 桶名',
    path         VARCHAR(1024) NULL COMMENT '桶内对象路径',
    content_type VARCHAR(128)  NULL COMMENT 'MIME 类型',
    create_time  DATETIME      NOT NULL,
    update_time  DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_media_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒体文件';
