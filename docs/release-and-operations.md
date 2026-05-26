# 发布与运维文档

## 分支

当前主分支：

```text
main
```

代码变更应在本地验证后提交到 `main`。APK 发布通过 Git tag 触发。

## 版本规则

使用语义化 tag：

```text
vMAJOR.MINOR.PATCH
```

示例：

- `v1.0.2`
- `v1.0.3`

CI 转换规则：

```text
v1.0.2 -> versionName 1.0.2
v1.0.2 -> versionCode 1000002
```

## 发布流程

1. 本地构建。

```bash
cd ZYPlayer
./gradlew assembleDebug --stacktrace
```

2. 提交并推送代码。

```bash
git add <files>
git commit -m "<message>"
git push origin main
```

3. 创建并推送 tag。

```bash
git tag v1.0.3
git push origin v1.0.3
```

4. 观察 GitHub Actions。

```bash
gh run list --repo lonnnnnng/VideoPlayer --workflow "Android APK" --limit 10
gh run watch <run-id> --repo lonnnnnng/VideoPlayer --exit-status
```

5. 确认 Release 产物。

```bash
gh release view v1.0.3 --repo lonnnnnng/VideoPlayer
```

## APK 签名

当前工作流构建 debug APK。为了让 debug APK 能覆盖安装已有 debug 包，CI 和本地必须使用同一个 debug 签名。

当前 GitHub Actions Secret：

```text
ANDROID_DEBUG_KEYSTORE_BASE64
```

工作流会将它还原到：

```text
$HOME/.android/debug.keystore
```

并设置：

```text
ANDROID_DEBUG_KEYSTORE_PATH=$HOME/.android/debug.keystore
```

Gradle 在该变量存在时使用该 keystore 签名 debug APK。

`v1.0.2` 已验证签名 SHA-256：

```text
6c8823ff4295d7b29e8e2c58b13c864f795b082255b67c80aa8cb783155e3899
```

## Release APK 验证

下载 Release APK：

```bash
rm -rf /tmp/zyplayer-release-v1.0.3
mkdir -p /tmp/zyplayer-release-v1.0.3
gh release download v1.0.3 --repo lonnnnnng/VideoPlayer --pattern "*.apk" --dir /tmp/zyplayer-release-v1.0.3
```

验证签名：

```bash
apksigner verify --print-certs /tmp/zyplayer-release-v1.0.3/ZYPlayer-v1.0.3.apk
```

覆盖安装：

```bash
adb install -r /tmp/zyplayer-release-v1.0.3/ZYPlayer-v1.0.3.apk
adb shell dumpsys package com.zy.player | rg "versionCode|versionName"
```

预期：

- `adb install -r` 返回 `Success`。
- `versionName` 与 tag 一致。
- `versionCode` 与 CI 派生值一致。

## 应用内更新运维

当前更新检查地址：

```text
https://api.github.com/repos/lonnnnnng/VideoPlayer/releases/latest
```

如果仓库是 private，普通用户设备无法匿名读取该接口，会收到 `404`。

推荐生产方案：

- 使用公开 Release 仓库。
- 将更新 JSON 和 APK 发布到公开 CDN 或对象存储。
- 建立后端代理，由服务端持有 GitHub 凭据，App 访问自有接口。

禁止事项：

- 不要把 GitHub Personal Access Token 写入 APK。
- 不要依赖 private GitHub Release 作为面向普通用户的更新源。

## 回滚策略

如果某个版本存在严重问题：

1. 将有问题的 GitHub Release 删除、下架或改为 prerelease/draft。
2. 发布更高版本号的修复 tag。
3. 确认应用更新检查只会拿到目标稳定版本。

注意：Android 正常安装流程不允许用低 `versionCode` 覆盖高 `versionCode`。

## 运维检查项

- GitHub Actions 是否成功。
- Release 是否存在 APK asset。
- APK 大小是否正常。
- APK 签名 SHA 是否符合预期。
- 应用更新接口是否可访问。
- 默认视频源、直播源是否可访问。
- 目标模拟器或真机是否可播放剧集、直播和在线链接。
