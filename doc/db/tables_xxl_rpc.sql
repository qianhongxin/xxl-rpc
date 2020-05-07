#
# XXL-RPC 1.6.1-SNAPSHOT
# Copyright (c) 2015-present, xuxueli.

CREATE database if NOT EXISTS `xxl_rpc` default character set utf8mb4 collate utf8mb4_unicode_ci;
use `xxl_rpc`;

SET NAMES utf8mb4;

CREATE TABLE `xxl_rpc_registry` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `biz` varchar(50) NOT NULL COMMENT '业务标识',
  `env` varchar(50) NOT NULL COMMENT '环境标识',
  `key` varchar(255) NOT NULL COMMENT '注册Key',
  `data` text NOT NULL COMMENT '注册Value有效数据',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0-正常、1-锁定',
  PRIMARY KEY (`id`),
  UNIQUE KEY `I_b_e_k` (`biz`,`env`,`key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `xxl_rpc_registry_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `biz` varchar(50) NOT NULL COMMENT '业务标识',
  `env` varchar(50) NOT NULL COMMENT '环境标识',
  `key` varchar(255) NOT NULL COMMENT '注册Key',
  `value` varchar(255) NOT NULL COMMENT '注册Value',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `I_b_e_k_v` (`biz`,`env`,`key`,`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `xxl_rpc_registry_message` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '消息类型：0-注册更新',
  `data` text NOT NULL COMMENT '消息内容',
  `addTime` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

commit;
