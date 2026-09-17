# 黑马点评（HM-DianPing）

> 一个基于 Spring Boot + Redis 的高并发点评类 Web 项目，仿大众点评实现商铺浏览、探店笔记、优惠券秒杀等核心功能，深度融合 Redis 高级特性解决高并发场景下的缓存问题。

---

## 项目简介

黑马点评是一个面向杭州地区的本地生活服务平台，用户可以浏览周边美食、KTV 等商铺信息，发布探店笔记，点赞评论互动，参与限时秒杀抢购优惠券等。项目重点聚焦于高并发场景下 Redis 缓存技术的深度应用，是学习和掌握 Redis 高级特性的优秀实践项目。

### 核心功能模块

| 模块 | 说明 |
|------|------|
| 用户模块 | 手机号 + 验证码登录，基于 Redis + Token 的会话管理 |
| 商铺模块 | 商铺 CRUD、按类型/名称分页查询、地理位置（GEO）排序 |
| 探店笔记 | 发布/查看探店笔记，点赞（基于 Redis ZSet），评论互动 |
| 关注模块 | 关注/取关用户，共同关注查询，关注 Feed 流推送 |
| 优惠券秒杀 | 秒杀抢券（全局唯一 ID、Lua 脚本 + Redis Stream 异步下单） |

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 1.8 | 开发语言 |
| Spring Boot | 2.3.12.RELEASE | 基础框架 |
| MyBatis Plus | 3.4.3 | ORM 持久层框架 |
| MySQL | 5.x | 关系型数据库 |
| Redis | 6.x | 缓存、分布式锁、消息队列、GEO |
| Redisson | 3.16.0 | Redis 分布式框架（分布式锁） |
| Lettuce | 6.1.6 | Redis 客户端连接 |
| Hutool | 5.7.17 | Java 工具库 |
| AspectJ | - | AOP 切面编程 |
| Lombok | - | 代码简化 |

---

## 项目结构

```
hm-dianping
├── src/main/java/com/hmdp
│   ├── config/           # 配置类（MVC拦截器、MyBatis、Redisson、全局异常）
│   ├── controller/       # 控制器层
│   │   ├── BlogController.java           # 探店笔记
│   │   ├── BlogCommentsController.java   # 笔记评论
│   │   ├── FollowController.java         # 关注
│   │   ├── ShopController.java           # 商铺
│   │   ├── ShopTypeController.java       # 商铺类型
│   │   ├── UploadController.java         # 文件上传
│   │   ├── UserController.java           # 用户
│   │   ├── VoucherController.java        # 优惠券
│   │   └── VoucherOrderController.java   # 秒杀下单
│   ├── dto/              # 数据传输对象（Result、UserDTO、LoginFormDTO 等）
│   ├── entity/           # 实体类（Shop、Blog、User、Voucher 等）
│   ├── mapper/           # MyBatis Mapper 接口
│   ├── service/          # 业务逻辑层接口与实现
│   └── utils/            # 工具类
│       ├── CacheClient.java          # 缓存工具（穿透/击穿/逻辑过期）
│       ├── SimpleRedisLock.java      # 基于 Redis SETNX 的分布式锁
│       ├── ILock.java                # 分布式锁接口
│       ├── RedisIdWorker.java        # 全局唯一 ID 生成器
│       ├── RedisConstants.java       # Redis 常量配置
│       ├── RedisData.java            # 缓存数据包装（逻辑过期）
│       ├── LoginIntercepter.java     # 登录拦截器
│       ├── RefreshTokenIntercepter.java  # Token 刷新拦截器
│       ├── PasswordEncoder.java      # 密码加密
│       ├── RegexUtils.java           # 正则校验工具
│       ├── SystemConstants.java      # 系统常量
│       └── UserHolder.java           # 当前用户持有者（ThreadLocal）
├── src/main/resources
│   ├── db/hmdp.sql          # 数据库初始化脚本
│   ├── mapper/              # MyBatis XML 映射文件
│   ├── application.yaml     # 应用配置
│   ├── seckill.lua          # 秒杀 Lua 脚本
│   └── unlock.lua           # 分布式锁释放 Lua 脚本
└── pom.xml
```

---

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.x
- MySQL 5.x / 8.x
- Redis 6.x+

### 数据库初始化

1. 在 MySQL 中创建数据库 `hmdp`
2. 执行 `src/main/resources/db/hmdp.sql` 初始化表结构和测试数据

```bash
mysql -u root -p hmdp < src/main/resources/db/hmdp.sql
```

### 配置修改

编辑 `src/main/resources/application.yaml`，修改为你本地的 MySQL 和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://你的MySQL地址:3306/hmdp?useSSL=false&serverTimezone=UTC
    username: root
    password: 你的密码
  redis:
    host: 你的Redis地址
    port: 6379
    password: 你的密码
```

### 启动项目

```bash
cd hm-dianping
mvn clean package -DskipTests
java -jar target/hm-dianping-0.0.1-SNAPSHOT.jar
```

启动后服务运行在 `http://localhost:8081`

---

## Redis 高级特性深度应用

本项目重点展示了 Redis 在解决高并发场景下各类缓存问题的技术方案：

### 1. 缓存穿透解决方案

**问题**：恶意请求查询不存在的数据，导致请求直接打到数据库。

**方案**：在 `CacheClient.queryWithPassThrough()` 中实现「缓存空对象」策略 —— 当数据库查询结果为空时，向 Redis 写入一个带短 TTL 的空值，避免同一不存在的数据反复查询数据库。

### 2. 缓存击穿解决方案

**问题**：热点 Key 过期瞬间，大量并发请求同时打到数据库。

**方案一（互斥锁）**：使用 Redis SETNX 实现分布式互斥锁，只有一个线程可以重建缓存，其他线程休眠重试。

**方案二（逻辑过期）**：在 `CacheClient.queryWithLogicalExpire()` 中实现 —— 缓存数据永不过期，而是设置一个逻辑过期时间。发现过期后获取互斥锁，开启独立线程异步重建缓存，主线程直接返回旧数据，实现高可用。

### 3. 分布式锁

**SimpleRedisLock**：基于 Redis SETNX + Lua 脚本解锁的轻量分布式锁，保证拿锁-比锁-删锁的原子性。

**Redisson**：集成 Redisson 框架的 `RLock`，支持自动续期、可重入、红锁等高级特性。

### 4. 全局唯一 ID 生成

**RedisIdWorker**：利用 Redis 自增生成分布式全局唯一 ID，格式为 `(时间戳 << 32) | 序列号`，支持单机 QPS 千万级，全局递增。

### 5. 秒杀优化

**流程**：Lua 脚本原子校验库存 + 用户下单状态 → Redis Stream 消息队列异步创建订单 → Redisson 锁防止用户重复下单。

核心 `seckill.lua` 脚本在 Redis 服务端完成：
- 库存校验（GET + 边界检查）
- 重复下单校验（SISMEMBER）
- 库存扣减（INCRBY）
- 下单标记（SADD）
- 消息推送（XADD）

### 6. Feed 流推送

基于 Redis ZSet 实现 **滚动分页**（Scroll Pagination），解决传统 offset 分页在高频插入场景下的数据重复问题，支持关注 Feed 流的实时推送。

### 7. GEO 地理位置查询

利用 Redis GEO 数据结构，实现「按距离排序查询附近商铺」功能，替代数据库空间索引，大幅提升地理位置查询效率。

---

## API 接口概览

### 用户模块
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/code` | 发送短信验证码 |
| POST | `/user/login` | 登录验证 |
| GET | `/user/me` | 获取当前用户信息 |

### 商铺模块
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shop/{id}` | 查询商铺详情（缓存优化） |
| POST | `/shop` | 新增商铺 |
| PUT | `/shop` | 更新商铺（缓存 + 数据库双写） |
| GET | `/shop/of/type` | 按类型分页查商铺（支持 GEO 排序） |
| GET | `/shop/of/name` | 按名称搜索商铺 |

### 探店笔记
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/blog` | 发布探店笔记 |
| GET | `/blog/hot` | 热门笔记（按点赞排序） |
| GET | `/blog/{id}` | 笔记详情 |
| PUT | `/blog/like/{id}` | 点赞/取消点赞（Redis ZSet） |
| GET | `/blog/likes/{id}` | 查看点赞用户列表（TOP5） |
| GET | `/blog/of/follow` | 关注 Feed 流（滚动分页） |

### 优惠券秒杀
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/voucher-order/seckill/{id}` | 秒杀优惠券（Lua + Stream） |

### 关注模块
| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/follow/{id}/{isFollow}` | 关注/取关 |
| GET | `/follow/or/not/{id}` | 是否已关注 |
| GET | `/follow/common/{id}` | 共同关注（Redis Set 交集） |

---

## 数据库 E-R 概览

| 表名 | 说明 | 核心字段 |
|------|------|------|
| `tb_user` | 用户表 | id, phone, password, nick_name, icon |
| `tb_shop` | 商铺表 | id, name, type_id, area, x, y, avg_price, score |
| `tb_shop_type` | 商铺类型 | id, name, icon, sort |
| `tb_blog` | 探店笔记 | id, shop_id, user_id, title, liked, comments |
| `tb_blog_comments` | 笔记评论 | id, user_id, blog_id, parent_id, content |
| `tb_follow` | 关注表 | id, user_id, follow_user_id |
| `tb_voucher` | 优惠券 | id, title, type, value, stock |
| `tb_seckill_voucher` | 秒杀券 | voucher_id, stock, begin_time, end_time |
| `tb_voucher_order` | 秒杀订单 | id, user_id, voucher_id, status |
| `tb_sign` | 签到表 | id, user_id, year, month, date |

---

## 作者

**橘子熊**

---

## 许可证

本项目仅用于学习交流目的，请勿用于商业用途。