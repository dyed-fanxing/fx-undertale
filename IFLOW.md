# Undertale Mod 项目文档

## 项目概述

这是一个基于 Minecraft NeoForge 1.21.1 的模组项目，旨在将《Undertale》游戏中的角色和机制引入 Minecraft。项目主要实现了 Sans 角色、Gaster Blaster 攻击、骨头攻击等游戏特性。

### 核心技术栈
- **Minecraft 版本**: 1.21.1
- **NeoForge 版本**: 21.1.186
- **Java 版本**: 21
- **构建工具**: Gradle (使用 net.neoforged.moddev 插件)

### 主要依赖库
- **GeckoLib** (4.7.6): 用于 3D 动画和模型渲染
- **JEI** (19.21.2.313): Just Enough Items 物品显示支持
- **Curios API** (9.2.2): 饰品和装备系统支持
- **Player Animator** (2.0.1+1.21.1): 玩家动画支持

## 项目结构

```
D:\Project\undertale\
├── src/
│   └── main/
│       ├── java/com/sakpeipei/undertale/
│       │   ├── Undertale.java              # 主模组类
│       │   ├── Config.java                 # 配置文件
│       │   ├── commands/                   # 命令系统
│       │   │   └── SansCommand.java        # Sans 技能命令
│       │   ├── common/                     # 通用功能
│       │   │   ├── mechanism/              # 游戏机制
│       │   │   └── anim/                   # 动画系统
│       │   ├── entity/                     # 实体系统
│       │   │   ├── boss/                   # Boss 实体
│       │   │   │   └── Sans.java           # Sans Boss
│       │   │   ├── projectile/             # 投射物
│       │   │   ├── summon/                 # 召唤物
│       │   │   └── IAnimatable.java        # 动画接口
│       │   ├── event/                      # 事件处理
│       │   ├── item/                       # 物品
│       │   │   ├── GasterBlasterItem.java
│       │   │   └── GasterBlasterProItem.java
│       │   ├── mobEffect/                  # 状态效果
│       │   │   └── KarmaMobEffect.java     # 业力效果
│       │   ├── network/                    # 网络数据包
│       │   ├── particle/                   # 粒子系统
│       │   ├── registry/                   # 注册系统
│       │   ├── utils/                      # 工具类
│       │   └── mixin/                      # Mixin 注入
│       └── resources/
│           ├── assets/undertale/
│           │   ├── animations/             # 动画定义
│           │   ├── geo/                    # GeckoLib 模型
│           │   ├── lang/                   # 语言文件 (en_us, zh_cn)
│           │   ├── models/                 # 模型文件
│           │   ├── particles/              # 粒子定义
│           │   ├── sounds/                 # 音效文件
│           │   └── textures/               # 纹理资源
│           └── data/                       # 数据生成
├── ogg/                                    # 音频资源
├── build.gradle                            # Gradle 构建配置
├── gradle.properties                       # Gradle 属性配置
├── settings.gradle                         # Gradle 设置
└── 实体中文文档.md                          # 实体中文文档
```

## 构建和运行

### 环境要求
- JDK 21
- Gradle 8.x (通过 Gradle Wrapper 自动管理)

### 构建命令

```powershell
# 清理并构建项目
./gradlew clean build

# 生成资源数据
./gradlew runData

# 运行客户端（开发模式）
./gradlew runClient

# 运行服务端（开发模式）
./gradlew runServer

# 运行游戏测试服务器
./gradlew runGameTestServer
```

### 重要说明
- 首次运行会自动下载 NeoForge 和所有依赖项
- 开发环境日志级别设置为 DEBUG
- 生成的资源文件会输出到 `src/generated/resources/`

## 开发规范

### 代码组织
- **包结构**: 按功能模块划分（entity, item, event, network 等）
- **注册系统**: 所有游戏元素通过 Registry 类统一注册
- **事件驱动**: 使用 NeoForge 事件总线处理游戏事件

### 命名约定
- **类名**: 使用 PascalCase（如 `SansCommand.java`）
- **方法名**: 使用 camelCase（如 `shootBoneRingVolley`）
- **常量**: 使用 UPPER_SNAKE_CASE
- **Mod ID**: `undertale`（小写）

### 动画系统
- 使用 GeckoLib 实现 3D 动画
- 实现 `IAnimatable` 接口的实体支持自定义动画
- 动画文件位于 `assets/undertale/animations/`

### 网络通信
- 使用 NeoForge 网络系统进行客户端-服务端通信
- 数据包位于 `network/` 包下
- 通过 `PacketDistributor` 分发数据包

### Mixin 使用
- Mixin 配置文件: `undertale.mixins.json`
- 用于修改 Minecraft 原有行为
- Mixin 类位于 `mixin/` 包下

## 核心功能

### Sans Boss
- 位于 `entity/boss/Sans.java`
- 实现多种攻击方式：
  1. 骨头环射击
  2. 弧形扫射
  3. 瞄准弹幕
  4. 前方弹幕
  5. 地面骨刺
  6. 环形骨刺波
  7. 单向骨刺波

### 技能命令
- 命令: `/sans <function>`
- 需要 OP 权限（等级 3）
- 功能参数: 1-7（对应不同攻击方式）

### Gaster Blaster
- 物品: `GasterBlasterItem` 和 `GasterBlasterProItem`
- 实现激光束攻击
- 支持网络同步

### 状态效果
- **Karma (业力)**: `KarmaMobEffect`
- 自定义伤害类型

## 配置文件

### gradle.properties 关键配置
```properties
minecraft_version=1.21.1
neo_version=21.1.186
geckolib_version=4.7.6
jei_version=19.21.2.313
player_animator_version=2.0.1+1.21.1
curios_version=9.2.2
```

### Mod 元数据
- **Mod ID**: `undertale`
- **版本**: `1.0-SNAPSHOT`
- **作者**: (在 gradle.properties 中配置)
- **许可证**: All Rights Reserved

## 资源管理

### 音效文件
- 位于 `ogg/` 目录
- 支持 MP3 和 OGG 格式
- 包括战斗音效、技能音效等

### 本地化
- 支持英文 (`en_us.json`) 和中文 (`zh_cn.json`)
- 位于 `assets/undertale/lang/`

## 开发注意事项

1. **Java 版本**: 必须使用 JDK 21
2. **数据生成**: 修改资源后运行 `runData` 任务
3. **热重载**: IDE 支持热重载，但某些修改需要重启
4. **Mixin 调试**: 使用 `-Dmixin.debug=true` 启用 Mixin 调试
5. **网络同步**: 确保客户端和服务端状态正确同步
6. **性能优化**: 注意动画和粒子效果的性能影响

## 常见问题

### 构建失败
- 检查 JDK 版本是否为 21
- 确保 Gradle 依赖正确下载
- 清理构建缓存: `./gradlew clean`

### 运行时崩溃
- 查看日志文件: `run/logs/`
- 检查依赖版本兼容性
- 验证资源文件完整性

### 动画不显示
- 确认 GeckoLib 依赖正确加载
- 检查动画文件路径和格式
- 验证模型文件是否正确

## 扩展开发

### 添加新实体
1. 在 `entity/` 包创建实体类
2. 在 `EntityTypeRegistry.java` 注册
3. 创建对应的模型和动画文件
4. 添加语言条目和纹理

### 添加新物品
1. 在 `item/` 包创建物品类
2. 在 `ItemRegistry.java` 注册
3. 添加模型和纹理资源
4. 配置创造模式标签页

### 添加新攻击方式
1. 在 Sans 类实现新攻击方法
2. 在 `SansCommand.java` 添加命令映射
3. 如需要，创建新的投射物实体

## 相关资源

- **NeoForge 文档**: https://docs.neoforge.net/
- **GeckoLib 文档**: https://github.com/bernie-g/geckolib
- **Minecraft Wiki**: https://minecraft.fandom.com/
- **Undertale Wiki**: https://undertale.fandom.com/

## 版本历史

- **1.0-SNAPSHOT**: 初始开发版本
  - 实现 Sans Boss
  - 实现 Gaster Blaster 攻击
  - 实现基础骨头攻击
  - 添加业力效果