Minecraft 1.21.4 Carpet / GCA 假人修复

安装：
1. 从 mods 文件夹移除原版 fabric-carpet-1.21.4-1.4.161+v241203.jar。
2. 放入 fabric-carpet-1.21.4-1.4.161+v241203-fall-damage-xp-fix.jar。
3. 从 mods 文件夹移除原版 gugle-carpet-addition-mc1.21.4-v2.10.0+build.12.jar。
4. 放入 gugle-carpet-addition-mc1.21.4-v2.10.0+build.12-resident-singleplayer-fix.jar。
5. 同一个 mod 不要同时保留原版 JAR 和修复版 JAR。

假人驻留：
- 安装修复版后默认自动启用，不需要执行命令或修改 carpet.conf。
- 默认同时恢复攻击、使用、移动、潜行等假人动作。
- 即使旧世界的 carpet.conf 保存了 false，启动世界后也会自动改为运行时启用。
- 命令可以临时关闭规则，但下次启动世界时会再次自动启用。

修复内容：
- 修复 1.21.4 中假人移动被错误视为客户端控制、落地处理被跳过的问题。
- 替换 Carpet 已过时的递归摔落处理。
- 通过原版方块的落地逻辑结算伤害，保留干草块、黏液块等行为。
- 修复假人死亡后经验未清零，重新召唤并再次死亡时重复掉落经验的问题。
- 修复 GCA 停服保存驻留假人时，被真实玩家或“不驻留”假人提前中断整个保存流程的问题。
- 将 fakePlayerResident 和 fakePlayerReloadAction 的默认值改为 true，并在世界加载完成后主动启用，避免旧 carpet.conf 覆盖。
- 修复单人世界退出时主玩家先断开、GCA 最终保存阶段玩家列表已空，导致 fake_player.gca.json 被覆盖为 {} 的问题。

实测：
- Fabric Loader 0.16.9
- Carpet 1.4.161+v241203（修复版）
- GCA 2.10.0+build.12（驻留修复版）
- Java 21
- 生存假人从 Y=15 落到 Y=5，生命值从 20 降至 13，无崩溃。
- 10 级假人第一次死亡正常掉落经验；再次召唤时等级、总经验和经验进度均为 0，第二次死亡不再掉落经验。
- 在预先保存两项 false 的 carpet.conf 测试世界中，启动后两项规则均自动变为 true。
- 假人停服后重新启动会自动上线；下界位置、朝向和每 17 tick 攻击动作均保持不变。
- 已根据单人世界录像和 latest.log 验证失败原因，并在主玩家断开时提前保存假人状态。
