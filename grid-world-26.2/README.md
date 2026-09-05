# Shenmi Grid World

面向 **Minecraft Java Edition 26.2 + Fabric** 的三维网格世界 Mod。

目标是复现 Grid World Generator / 参考视频中的玩法：保留原版世界的生物群系与世界生成逻辑，但将自然地形大面积挖空，只留下稀疏的三维网格骨架；村庄、神殿、矿井等后续结构仍按原版流程生成。

## 当前实现

- Minecraft Java Edition 26.2。
- Fabric Loader 0.19.3。
- Fabric API 0.156.0+26.2。
- Fabric Loom 1.17-SNAPSHOT（当前解析为 1.17.20）。
- Java 25。
- Gradle 9.5.1。
- 主世界、下界、末地等使用 `NoiseBasedChunkGenerator` 的维度都会进行网格化。
- 网格处理发生在 NOISE 地形生成完成后、SURFACE / CARVERS / FEATURES 之前。
- 因此自然地形先被网格化，之后表面、洞穴、树木、矿物、村庄、神殿等继续执行原版生成流程。
- 不再使用区块加载后的二次挖掘，因此不会在每次加载世界时重复修改已经生成的区块。

## 默认网格规则

默认配置：

```properties
gridSpacing=5
gridThickness=1
```

一个自然地形方块只有在 X/Y/Z 三个坐标中 **至少两个坐标命中网格位置** 时才保留。

例如默认间距为 5 时，可近似理解为保留：

```text
x mod 5 == 0 且 y mod 5 == 0
x mod 5 == 0 且 z mod 5 == 0
y mod 5 == 0 且 z mod 5 == 0
```

这样留下的是三维“线框/格架”，而不是大片网格平面。默认参数理论保留比例约为 10.4%，与参考模组约 10.33% 的目标非常接近。

## 配置

第一次启动 Minecraft / Fabric Server 后会生成：

```text
config/shenmi-grid-world.properties
```

可修改：

```properties
gridSpacing=5
gridThickness=1
```

有效范围：

- `gridSpacing`: 2 ~ 64
- `gridThickness`: 1 ~ `gridSpacing - 1`

修改后需要重启游戏或服务器。

## 构建环境

本机已经安装：

```text
Zulu OpenJDK 25.0.4.1
C:\Program Files\Zulu\zulu-25
```

当前项目在 `gradle.properties` 中固定使用这套 JDK 25：

```properties
org.gradle.java.home=C:/Program Files/Zulu/zulu-25
```

同时已经安装用户级 Gradle 9.5.1：

```text
%LOCALAPPDATA%\Gradle\gradle-9.5.1
```

构建命令：

```powershell
& "$env:LOCALAPPDATA\Gradle\gradle-9.5.1\bin\gradle.bat" clean build
```

## 构建结果

已经在本机完成 `clean build`，构建成功。

可安装 Mod：

```text
build/libs/shenmi-grid-world-0.1.0.jar
```

源码包：

```text
build/libs/shenmi-grid-world-0.1.0-sources.jar
```

## 安装与测试建议

1. 使用 Minecraft 26.2。
2. 安装 Fabric Loader 0.19.3 或兼容的更新版本。
3. 安装适用于 Minecraft 26.2 的 Fabric API。
4. 把 `shenmi-grid-world-0.1.0.jar` 放入实例的 `mods` 文件夹。
5. 建议创建一个全新的测试世界；旧区块不会自动重新网格化。
6. 分别检查主世界、下界和末地。
7. 重点检查村庄、神殿、矿井、要塞等结构是否保持完整，以及水/岩浆在网格边缘的更新行为。

## 当前状态

源码编译和 Mixin 注入签名已经通过 Minecraft 26.2 + Fabric Loom 的构建检查。

下一步最重要的是实际启动 Minecraft 26.2 新世界进行视觉与玩法测试，重点验证：

- 5 格网格是否与参考视频视觉密度一致；
- 原版结构是否完整；
- 水和岩浆是否需要额外禁止初始流动；
- 出生点是否需要做安全位置修正；
- 性能是否需要进一步优化。
