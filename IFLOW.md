# Undertale Mod 项目文档

## 项目概述

这是一个基于 Minecraft NeoForge 1.21.1 的模组项目，旨在将《Undertale》游戏中的角色和机制引入 Minecraft。项目主要实现了 Sans Boss、Gaster Blaster 攻击、骨头攻击、业力效果等游戏特性。

### 核心技术栈
- **Minecraft 版本**: 1.21.1
- **NeoForge 版本**: 21.1.186
- **Java 版本**: 21
- **构建工具**: Gradle (使用 net.neoforged.moddev 插件 2.0.95)

### 主要依赖库
- **GeckoLib** (4.7.6): 用于 3D 动画和模型渲染
- **JEI** (19.21.2.313): Just Enough Items 物品显示支持
- **Curios API** (9.2.2): 饰品和装备系统支持
- **Player Animator** (2.0.1+1.21.1): 玩家动画支持

## 项目结构

```
E:\MinecraftMod\Java\undertale\
├── src/
│   └── main/
│       ├── java/com/sakpeipei/undertale/
│       │   ├── Undertale.java              # 主模组类
│       │   ├── Config.java                 # 配置文件
│       │   ├── commands/                   # 命令系统
│       │   │   └── SansCommand.java        # Sans 技能命令
│       │   ├── common/                     # 通用功能
│       │   │   ├── anim/                   # 动画系统
│       │   │   │   ├── ActionData.java
│       │   │   │   ├── AnimStepT.java
│       │   │   │   ├── SequenceAnim.java
│       │   │   │   ├── SequenceAnimT.java
│       │   │   │   ├── SingleAnim.java
│       │   │   │   └── SingleAnimT.java
│       │   │   ├── mechanism/              # 游戏机制
│       │   │   │   ├── AquaAttack.java
│       │   │   │   ├── ColorAttack.java
│       │   │   │   ├── GreenAttack.java
│       │   │   │   └── OrangeAttack.java
│       │   │   ├── function/               # 函数接口
│       │   │   │   └── FloatSupplier.java
│       │   │   ├── tags/                   # 标签系统
│       │   │   │   └── entitytype/
│       │   │   │       └── EntityTypeTags.java
│       │   │   ├── CommonHooks.java
│       │   │   ├── Config.java
│       │   │   ├── DamageTypes.java
│       │   │   ├── EnumParameters.java
│       │   │   ├── LocalDirection.java
│       │   │   ├── LocalVec3.java
│       │   │   └── RenderTypes.java
│       │   ├── client/                     # 客户端专用代码
│       │   │   ├── Setup.java
│       │   │   ├── event/                  # 客户端事件
│       │   │   │   └── handler/
│       │   │   ├── gui/                    # GUI 系统
│       │   │   │   └── KaramHeartType.java
│       │   │   ├── model/                  # 模型
│       │   │   │   ├── entity/
│       │   │   │   │   ├── FlyingBoneModel.java
│       │   │   │   │   ├── GasterBlasterModel.java
│       │   │   │   │   ├── GasterBlasterProModel.java
│       │   │   │   │   ├── GroundBoneModel.java
│       │   │   │   │   ├── GroundBoneProjectileModel.java
│       │   │   │   │   └── SansModel.java
│       │   │   │   └── item/
│       │   │   ├── particle/               # 粒子渲染
│       │   │   │   ├── BallGrowParticle.java
│       │   │   │   ├── LightStreakParticle.java
│       │   │   │   └── NoRandomDirParticle.java
│       │   │   └── render/                 # 渲染器
│       │   │       ├── effect/
│       │   │       │   ├── Effect.java
│       │   │       │   ├── WarningTip.java
│       │   │       │   └── WarningTipAABB.java
│       │   │       ├── entity/
│       │   │       │   ├── ColorAttackRenderer.java
│       │   │       │   ├── FlyingBoneRender.java
│       │   │       │   ├── GasterBlasterBeamRenderer.java
│       │   │       │   ├── GasterBlasterProRender.java
│       │   │       │   ├── GasterBlasterRender.java
│       │   │       │   ├── GroundBoneProjectileRender.java
│       │   │       │   ├── GroundBoneRender.java
│       │   │       │   └── SansRender.java
│       │   │       ├── gui/
│       │   │       │   └── KarmaOverlay.java
│       │   │       ├── item/
│       │   │       │   ├── GasterBlasterItemRender.java
│       │   │       │   └── GasterBlasterProItemRender.java
│       │   │       └── layer/
│       │   │           ├── AnimatedGlowingLayer.java
│       │   │           ├── AnimatedLayer.java
│       │   │           ├── GasterBlasterEyesLayer.java
│       │   │           ├── SansEyesLayer.java
│       │   │           └── SansFatigueLayer.java
│       │   ├── data/                       # 数据生成
│       │   │   ├── DamageTypeTagsProvider.java
│       │   │   └── EntityTypeTagsProvider.java
│       │   ├── entity/                     # 实体系统
│       │   │   ├── ai/                     # AI 系统
│       │   │   │   └── goal/
│       │   │   │       ├── AnimGoal.java
│       │   │   │       ├── NeutralMobAngerTargetGoal.java
│       │   │   │       ├── SequenceAnimGoal.java
│       │   │   │       └── SingleAnimGoal.java
│       │   │   ├── attachment/             # 数据附件
│       │   │   │   ├── GravityData.java
│       │   │   │   ├── KaramAttackData.java
│       │   │   │   └── KaramMobEffectData.java
│       │   │   ├── boss/                   # Boss 实体
│       │   │   │   └── Sans.java           # Sans Boss
│       │   │   ├── common/                 # 通用实体
│       │   │   ├── projectile/             # 投射物
│       │   │   │   ├── AbstractPenetrableProjectile.java
│       │   │   │   ├── FlyingBone.java
│       │   │   │   ├── GroundBoneProjectile.java
│       │   │   │   └── ProjectileDeflection.java
│       │   │   ├── summon/                 # 召唤物
│       │   │   │   ├── GasterBlaster.java
│       │   │   │   ├── GasterBlasterPro.java
│       │   │   │   ├── GroundBone.java
│       │   │   │   └── IGasterBlaster.java
│       │   │   ├── AttackColored.java
│       │   │   ├── DelayAction.java
│       │   │   ├── DelayedEntity.java
│       │   │   ├── DelayEntity.java
│       │   │   └── IAnimatable.java        # 动画接口
│       │   ├── event/                      # 事件处理
│       │   │   ├── handler/
│       │   │   │   ├── DataHandler.java
│       │   │   │   ├── EntitySizeHandler.java
│       │   │   │   ├── EntityTrackerHandler.java
│       │   │   │   ├── EventHandlers.java
│       │   │   │   ├── KarmaAndFrameHandler.java
│       │   │   │   ├── LivingEntityHandler.java
│       │   │   │   ├── PlayerHandler.java
│       │   │   │   └── ServerTickHandler.java
│       │   │   └── ProjectileDodgeEvent.java
│       │   ├── item/                       # 物品
│       │   │   ├── GasterBlasterItem.java
│       │   │   └── GasterBlasterProItem.java
│       │   ├── mixin/                      # Mixin 注入
│       │   │   ├── AbstractArrowMixin.java
│       │   │   ├── BlackScreenTeleportMixin.java
│       │   │   ├── EntityGroundMixin.java
│       │   │   ├── EntityRenderDispatcherMixin.java
│       │   │   ├── KarmaHeartMixin.java
│       │   │   ├── LivingEntityGravityMixin.java
│       │   │   ├── ServerGamePacketListenerImplMixin.java
│       │   │   └── TimeJumpTeleportMixin.java
│       │   ├── mobEffect/                  # 状态效果
│       │   │   └── KarmaMobEffect.java     # 业力效果
│       │   ├── network/                    # 网络数据包
│       │   │   ├── AnimPacket.java
│       │   │   ├── GasterBlasterBeamEndPacket.java
│       │   │   ├── GasterBlasterProPacket.java
│       │   │   ├── KaramPacket.java
│       │   │   ├── TimeJumpTeleportPacket.java
│       │   │   ├── WarningTipAABBPacket.java
│       │   │   └── WarningTipPacket.java
│       │   ├── particle/                   # 粒子系统
│       │   │   └── options/
│       │   │       ├── GrowOption.java
│       │   │       ├── GrowTrackEntityOption.java
│       │   │       ├── LifeTimeOption.java
│       │   │       └── UniversalParticleOptions.java
│       │   ├── registry/                   # 注册系统
│       │   │   ├── AttachmentTypeRegistry.java
│       │   │   ├── BlockRegistry.java
│       │   │   ├── EntityTypeRegistry.java
│       │   │   ├── ItemRegistry.java
│       │   │   ├── MobEffectRegistry.java
│       │   │   ├── ParticleRegistry.java
│       │   │   └── SoundRegistry.java
│       │   └── utils/                      # 工具类
│       └── resources/
│           ├── undertale.mixins.json        # Mixin 配置
│           ├── META-INF/
│           │   └── neoforge.mods.toml      # 模组元数据
│           ├── assets/undertale/
│           │   ├── animations/             # 动画定义
│           │   │   ├── entity/
│           │   │   │   ├── bone.animation.json
│           │   │   │   ├── gaster_blaster.animation.json
│           │   │   │   └── sans.animation.json
│           │   ├── geo/                    # GeckoLib 模型
│           │   │   ├── entity/
│           │   │   │   ├── bone.bbmodel
│           │   │   │   ├── bone.geo.json
│           │   │   │   ├── gaster_blaster.geo.json
│           │   │   │   ├── GasterBlaster.bbmodel
│           │   │   │   ├── sans_1.bbmodel
│           │   │   │   ├── sans.bbmodel
│           │   │   │   └── sans.geo.json
│           │   │   └── item/
│           │   │       └── gaster_blaster.geo.json
│           │   ├── lang/                   # 语言文件
│           │   │   ├── en_us.json
│           │   │   └── zh_cn.json
│           │   ├── models/                 # 模型文件
│           │   │   ├── item/
│           │   │   │   └── gaster_blaster.json
│           │   ├── particles/              # 粒子定义
│           │   │   ├── ball_grow.json
│           │   │   ├── halo_scale.json
│           │   │   └── light_streak.json
│           │   ├── sounds/                 # 音效文件
│           │   │   ├── gaster_blaster/
│           │   │   │   ├── charge.ogg
│           │   │   │   ├── fire.ogg
│           │   │   │   └── whole.ogg
│           │   │   ├── sans/
│           │   │   │   ├── bone_spine.ogg
│           │   │   │   ├── slam.ogg
│           │   │   │   └── teleport_time_jump.ogg
│           │   │   └── enemy_encounter_attack_tip.ogg
│           │   └── textures/               # 纹理资源
│           │       ├── entity/
│           │       │   ├── beam/
│           │       │   │   ├── front.png
│           │       │   │   ├── ribbon_glow.png
│           │       │   │   ├── side.png
│           │       │   │   └── twisting_glow.png
│           │       │   ├── gaster_blaster/
│           │       │   ├── bone.png
│           │       │   ├── gaster_blaster_eyes.png
│           │       │   ├── gaster_blaster_eyes.png.mcmeta
│           │       │   ├── gaster_blaster.png
│           │       │   ├── sans_eye_t.png
│           │       │   ├── sans_eye_t.png.mcmeta
│           │       │   ├── sans_eye.png
│           │       │   ├── sans_eye.png.mcmeta
│           │       │   ├── sans_eyes.png
│           │       │   ├── sans_eyes.png.mcmeta
│           │       │   ├── sans_sweat_1.png
│           │       │   ├── sans_sweat_2.png
│           │       │   ├── sans_sweat_3.png
│           │       │   └── sans.png
│           │       ├── gui/
│           │       ├── item/
│           │       └── particle/
│           └── data/                       # 数据生成
│               ├── minecraft/
│               │   └── tags/
│               │       └── entity_type/
│               │           └── fall_damage_immune.json
│               ├── neoforge/
│               │   └── tags/
│               │       └── damage_type/
│               └── undertale/
│                   ├── damage_type/
│                   │   ├── frame.json
│                   │   ├── gaster_blaster_beam.json
│                   │   └── karma.json
│                   └── entity_type/
├── ogg/                                    # 音频资源
├── 音效包/                                 # 音效包目录
├── build.gradle                            # Gradle 构建配置
├── gradle.properties                       # Gradle 属性配置
├── settings.gradle                         # Gradle 设置
├── IFLOW.md                                # 项目文档
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
- **附件系统**: 使用 NeoForge Attachment API 存储实体附加数据

### 命名约定
- **类名**: 使用 PascalCase（如 `SansCommand.java`）
- **方法名**: 使用 camelCase（如 `shootBoneRingVolley`）
- **常量**: 使用 UPPER_SNAKE_CASE
- **Mod ID**: `undertale`（小写）

### 动画系统
- 使用 GeckoLib 实现 3D 动画
- 实现 `IAnimatable` 接口的实体支持自定义动画
- 支持 `SingleAnim` 和 `SequenceAnim` 两种动画类型
- 动画文件位于 `assets/undertale/animations/`

### 网络通信
- 使用 NeoForge 网络系统进行客户端-服务端通信
- 数据包位于 `network/` 包下
- 通过 `PacketDistributor` 分发数据包
- 支持动画同步、效果同步、警告提示等

### Mixin 使用
- Mixin 配置文件: `undertale.mixins.json`
- 用于修改 Minecraft 原有行为
- Mixin 类位于 `mixin/` 包下
- 分为服务端 Mixin 和客户端 Mixin

### 附件系统 (Attachment System)
- 使用 NeoForge Attachment API 存储实体附加数据
- 附件类型在 `AttachmentTypeRegistry` 中注册
- 支持的附件类型:
  - `KaramMobEffectData`: 业力效果数据
  - `KaramAttackData`: 业力攻击数据
  - `GravityData`: 重力数据

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
- 支持 AI 行为和动画系统
- 支持疲劳效果和眼睛动画

### 技能命令
- 命令: `/sans <function>`
- 需要 OP 权限（等级 3）
- 功能参数: 1-7（对应不同攻击方式）

### Gaster Blaster
- 物品: `GasterBlasterItem` 和 `GasterBlasterProItem`
- 实体: `GasterBlaster` 和 `GasterBlasterPro`
- 实现激光束攻击
- 支持网络同步
- 包含充能、发射、完整音效

### 骨头攻击系统
- 飞行骨头 (`FlyingBone`): 空中投射物
- 地面骨刺 (`GroundBone`): 从地面升起
- 地面骨刺投射物 (`GroundBoneProjectile`): 可投掷的骨刺
- 支持颜色攻击机制（橙色、绿色、蓝色）

### 状态效果
- **Karma (业力)**: `KarmaMobEffect`
  - 自定义伤害效果
  - 使用附件系统存储效果数据
  - 心形 GUI 变化
- **Frame (帧)**: 自定义伤害类型

### 粒子系统
- **BallGrow**: 随生命周期变大的粒子
- **HaloScale**: 光环粒子
- **LightStreak**: 光束拖尾粒子
- 自定义粒子选项支持

### 音效系统
- Sans 音效:
  - `bone_spine`: 骨刺音效
  - `slam`: 拍击音效
  - `teleport_time_jump`: 传送音效
- Gaster Blaster 音效:
  - `charge`: 充能音效
  - `fire`: 发射音效
  - `whole`: 完整音效
- 其他音效:
  - `enemy_encounter_attack_tip`: 攻击提示音效

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

### 模型和纹理
- GeckoLib 模型位于 `assets/undertale/geo/`
- BlockBench 模型文件 (`.bbmodel`)
- 纹理位于 `assets/undertale/textures/`

## 开发注意事项

1. **Java 版本**: 必须使用 JDK 21
2. **数据生成**: 修改资源后运行 `runData` 任务
3. **热重载**: IDE 支持热重载，但某些修改需要重启
4. **Mixin 调试**: 使用 `-Dmixin.debug=true` 启用 Mixin 调试
5. **网络同步**: 确保客户端和服务端状态正确同步
6. **性能优化**: 注意动画和粒子效果的性能影响
7. **附件系统**: 使用 Attachment API 存储实体附加数据，避免使用 NBT

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
4. 创建渲染器和模型类
5. 添加语言条目和纹理

### 添加新物品
1. 在 `item/` 包创建物品类
2. 在 `ItemRegistry.java` 注册
3. 添加模型和纹理资源
4. 配置创造模式标签页

### 添加新攻击方式
1. 在 Sans 类实现新攻击方法
2. 在 `SansCommand.java` 添加命令映射
3. 如需要，创建新的投射物实体
4. 添加相关音效和粒子效果

### 添加新粒子
1. 在 `particle/options/` 创建粒子选项类
2. 在 `ParticleRegistry.java` 注册粒子类型
3. 在 `client/particle/` 创建粒子渲染类
4. 添加粒子定义 JSON 文件

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
  - 添加粒子系统
  - 添加音效系统
  - 实现 Attachment API
  - 添加 AI 系统