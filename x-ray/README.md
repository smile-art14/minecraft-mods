# X-Ray

一个适用于 Minecraft 1.20.1 Fabric 的纯客户端可调夜视模组。

## 操作

- `X`：开关夜视
- `[`：降低夜视强度（每次 10%）
- `]`：提高夜视强度（每次 10%）

启用状态与强度会保存到 `config/x-ray.properties`。

本模组只依赖 Fabric Loader，不需要额外安装 Fabric API，也不需要在服务端安装。
1.0.5 起夜视状态完全由每个玩家自己的客户端渲染控制：不会再向 `LocalPlayer` 的真实药水效果列表写入夜视，因此远程服务器的效果同步不会覆盖 X-Ray。对于会查询玩家夜视状态的客户端渲染/光影代码，模组会通过客户端 Mixin 提供一个虚拟夜视查询结果，同时仍保持服务器端玩家状态不变。

## 构建

```powershell
.\gradlew.bat build
```

构建完成后的模组位于
`build/libs/x-ray-fabric-1.20.1-client-1.0.5.jar`。

请只把这个文件放入 `mods` 文件夹，不要安装名称中包含
`sources` 的源码包。
