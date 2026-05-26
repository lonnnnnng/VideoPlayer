# VideoPlayer / ZYPlayer

VideoPlayer 是一个 Android 视频播放器项目，当前 Android 应用模块为 `ZYPlayer`，包名为 `com.zy.player`。

项目围绕三类播放场景建设：

- 在线影视列表浏览、搜索、详情和剧集播放。
- IPTV 直播源解析和直播播放。
- 手动输入 M3U8 或 M3U 链接后解析播放。

当前界面采用深色影院风格，底部主导航为 `首页`、`直播`、`在线`、`设置`。

## 当前功能

- 首页：展示影视内容，支持搜索入口、下拉刷新、上滑加载更多。
- 搜索：支持关键词搜索，并跳转到搜索结果列表。
- 详情：展示海报、基础信息、简介、播放源、播放线路和紧凑剧集列表。
- 剧集播放：支持返回、标题和当前集数展示、源地址展示和复制、快退、播放/暂停、快进、全屏、倍速最高 4 倍、分辨率/码率/实时网速展示。
- 直播：支持 M3U 直播源解析、频道搜索、分组筛选、直播源切换。
- 直播播放：布局和剧集播放页保持一致，支持频道标题、源地址复制、投屏、播放/暂停、全屏、分辨率/码率/网速展示。
- 在线播放：支持输入 M3U8 或 M3U 链接，解析后进入对应播放流程。
- 设置：支持播放历史、视频源管理、直播源管理、重置应用、免责声明、检测更新。
- CI：GitHub Actions 可构建 APK，并在推送 `v*` 标签时创建 GitHub Release。

## 技术栈

- 开发语言：Kotlin
- UI：Jetpack Compose、Material 3
- 导航：Navigation Compose
- 依赖注入：Hilt
- 本地存储：Room
- 网络：Retrofit、OkHttp
- 播放：AndroidX Media3 / ExoPlayer，支持 HLS
- 图片加载：Coil
- 异步：Kotlin Coroutines、Flow
- 构建：Gradle Kotlin DSL
- CI/CD：GitHub Actions

## 项目结构

```text
.
├── .github/workflows/android-apk.yml
├── prototypes/
│   └── zyplayer-ui-concept.html
└── ZYPlayer/
    ├── app/build.gradle.kts
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── java/com/zy/player/
        │   ├── data/
        │   ├── di/
        │   ├── domain/
        │   ├── player/
        │   └── ui/
        └── res/
```

核心目录说明：

- `data/local`：Room 数据库、DAO、默认数据源配置。
- `data/remote`：影视 API 数据模型和 Retrofit Service。
- `data/repository`：影视、直播、历史、源管理、应用更新等仓库。
- `domain`：播放模型和解析辅助逻辑。
- `player`：共享 Media3/ExoPlayer 播放器管理。
- `ui/navigation`：路由和导航图。
- `ui/screens`：首页、详情、播放、直播、在线、设置、管理页等页面。
- `ui/components`：通用 UI 组件。

## 开发环境

推荐开发环境：

- macOS 或 Linux。
- Android Studio。
- JDK 17。
- Android SDK Platform 35。
- Android Build Tools 35.0.0 或兼容版本。
- 使用仓库内置 Gradle Wrapper。

本地构建：

```bash
cd ZYPlayer
chmod +x ./gradlew
./gradlew assembleDebug --stacktrace
```

Debug APK 输出位置：

```text
ZYPlayer/app/build/outputs/apk/debug/app-debug.apk
```

手动安装到模拟器：

```bash
adb install -r ZYPlayer/app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.zy.player -c android.intent.category.LAUNCHER 1
```

## 测试环境

当前主要手动验证环境：

- 模拟器：`Pixel9-2`
- 应用包名：`com.zy.player`
- 基线版本：`1.0.0`
- 已验证发布版本：`1.0.2`
- 已验证结果：GitHub Release APK 在签名一致时可以覆盖安装旧版本。

常用检查命令：

```bash
adb devices
adb shell dumpsys package com.zy.player | rg "versionCode|versionName"
```

重点测试范围：

- 应用启动和底部导航。
- 首页下拉刷新和上滑加载更多。
- 搜索、详情、剧集播放流程。
- 直播源解析、频道筛选、直播播放流程。
- 在线 M3U8/M3U 链接解析和播放流程。
- 离开播放页后播放器是否释放，是否还存在后台声音或旧画面。
- 当前源地址复制功能。
- 分辨率、码率、实时网速展示。
- 设置页检测更新流程。

## CI 和发布

GitHub Actions 工作流：

```text
.github/workflows/android-apk.yml
```

工作流能力：

- `main` 分支推送、PR、手动触发、`v*` 标签均可构建 APK。
- 每次构建上传 debug APK artifact。
- 推送 `v*` 标签时创建 GitHub Release，并上传 `ZYPlayer-<tag>.apk`。
- 根据 tag 派生版本号。例如 `v1.0.2` 会生成 `versionName=1.0.2`。
- 根据 tag 派生版本码。例如 `v1.0.2` 会生成 `versionCode=1000002`。
- 支持通过 `ANDROID_DEBUG_KEYSTORE_BASE64` Secret 固定 debug 签名，保证 Release APK 能覆盖安装本机 debug 包。

发布新版本：

```bash
git tag v1.0.3
git push origin v1.0.3
```

## 应用内更新说明

当前应用检测更新接口：

```text
https://api.github.com/repos/lonnnnnng/VideoPlayer/releases/latest
```

如果 GitHub 仓库是 private，Android App 匿名请求该接口会返回 `404`，即使 Release 实际存在。当前表现是设置页提示：

```text
检测更新失败：暂未找到 GitHub Release
```

要让普通用户可以正常应用内更新，需要满足以下任一条件：

- 将仓库或 Release 下载入口调整为公开可访问。
- 将更新元数据和 APK 放到公开 CDN 或对象存储。
- 增加服务端代理，由服务端持有 GitHub 凭据，App 只访问自有公开接口。

不要把 GitHub Token 写入 APK。

## 文档

- [需求文档](docs/requirements.md)
- [业务文档](docs/business.md)
- [技术架构文档](docs/technical-architecture.md)
- [测试与验收文档](docs/testing-and-acceptance.md)
- [发布与运维文档](docs/release-and-operations.md)
- [开发指南](docs/development-guide.md)

## 注意事项

本项目提供的是播放客户端、源管理和播放体验实现。影视和直播内容来自配置的数据源，数据源可用性、合法性、版权合规性需要由项目运营方在分发前自行确认。
