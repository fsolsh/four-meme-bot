/*
 Navicat Premium Dump SQL

 Source Server         : LocalHost-mysql
 Source Server Type    : MySQL
 Source Server Version : 80016 (8.0.16)
 Source Host           : localhost:3306
 Source Schema         : four_meme

 Target Server Type    : MySQL
 Target Server Version : 80016 (8.0.16)
 File Encoding         : 65001

 Date: 23/09/2026 17:44:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bot_task
-- ----------------------------
DROP TABLE IF EXISTS `bot_task`;
CREATE TABLE `bot_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `token_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代币合约地址',
  `token_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代币简称',
  `market_pair` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易市场',
  `min_buy_amount` decimal(36, 18) NOT NULL DEFAULT 0.001000000000000000 COMMENT '最小买入额',
  `max_buy_amount` decimal(36, 18) NOT NULL DEFAULT 0.010000000000000000 COMMENT '最大买入额',
  `min_interval_sec` int(11) NULL DEFAULT 10 COMMENT '最小间隔-秒',
  `max_interval_sec` int(11) NULL DEFAULT 30 COMMENT '最大间隔-秒',
  `buy_weight` int(11) NULL DEFAULT 60 COMMENT '买入权重百分比(0-100)',
  `max_trades_per_round` int(11) NULL DEFAULT 1 COMMENT '每轮最多交易笔数',
  `is_running` tinyint(4) NULL DEFAULT 0 COMMENT '运行状态',
  `graduated` tinyint(4) NULL DEFAULT 0 COMMENT '已毕业联合曲线\r\n已经毕业(0-联合曲线1-PancakeSwap)',
  `graduation_threshold` decimal(36, 18) NULL DEFAULT 18.000000000000000000 COMMENT '募集数量',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_token`(`token_address` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交易任务' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for token_meta
-- ----------------------------
DROP TABLE IF EXISTS `token_meta`;
CREATE TABLE `token_meta`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `token_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代币合约地址',
  `token_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代币简称',
  `decimals` int(11) NOT NULL COMMENT '精度',
  `market_pair` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '交易市场：BNB/USDT',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'CHAIN' COMMENT '数据来源：CHAIN/MANUAL',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_token_address`(`token_address` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代币元数据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trade_record
-- ----------------------------
DROP TABLE IF EXISTS `trade_record`;
CREATE TABLE `trade_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `wallet_id` bigint(20) NOT NULL COMMENT '交易钱包ID',
  `token_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代币合约地址',
  `token_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代币简称',
  `quote_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报价币简称',
  `trade_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BUY/SELL',
  `quote_amount` decimal(36, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '金额',
  `token_amount` decimal(36, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '数量',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '交易Hash',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '0=待确认 1=成功 2=失败',
  `error_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `gas_price` bigint(20) NULL DEFAULT 0 COMMENT 'gas价格',
  `stage` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'BONDING_CURVE' COMMENT '生命周期：BONDING_CURVE/PANCAKE',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_token`(`token_address` ASC) USING BTREE,
  INDEX `idx_wallet`(`wallet_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 571 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交易记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt哈希',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'USER' COMMENT 'ADMIN/USER',
  `totp_secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Google验证器密钥',
  `totp_bound` tinyint(4) NULL DEFAULT 0 COMMENT '是否已绑定TOTP',
  `password_changed` tinyint(4) NULL DEFAULT 0 COMMENT '是否已修改初始密码',
  `token_version` int(11) NULL DEFAULT 0 COMMENT 'JWT版本号，递增后旧token失效',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最近登录时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wallet
-- ----------------------------
DROP TABLE IF EXISTS `wallet`;
CREATE TABLE `wallet`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '钱包地址',
  `private_key_encrypted` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AES加密后的私钥',
  `mnemonic_encrypted` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AES加密后的助记词',
  `bnb_balance` decimal(36, 18) NULL DEFAULT 0.000000000000000000 COMMENT 'BNB余额',
  `usdt_balance` decimal(36, 18) NULL DEFAULT 0.000000000000000000 COMMENT 'USDT余额',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `last_trade_at` bigint(20) NULL DEFAULT 0 COMMENT '上次交易时间戳(毫秒)',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `address`(`address` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '钱包信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for wallet_token_balance
-- ----------------------------
DROP TABLE IF EXISTS `wallet_token_balance`;
CREATE TABLE `wallet_token_balance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `wallet_id` bigint(20) NOT NULL COMMENT '钱包ID',
  `token_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代币合约地址(小写)',
  `token_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '代币简称',
  `balance` decimal(36, 18) NULL DEFAULT 0.000000000000000000 COMMENT '持仓数量',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wallet_token`(`wallet_id` ASC, `token_address` ASC) USING BTREE,
  INDEX `idx_wallet`(`wallet_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代币资产' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
