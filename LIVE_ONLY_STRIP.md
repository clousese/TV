# FongMi 只保留直播 · 剥离原型说明

来源：公共仓库 FongMi/TV，`fongmi` 分支，深度 1 克隆。
注意：源码仓库里只有一个 `fongmi` 分支，没有单独的 `okjack` 源码分支。
OK 版的安装包在 FongMi/Release 的 `okjack` 发布分支下，它的改动主要体现在
发布包层面，不能从公共源码仓库单独拉取一份“OK 源码”。
本原型因此基于 FongMi 主线直播模块，与 OK 版同源，不是 OK 团队的私有改动。

## 结论先说

能剥离，但不是“删几个文件就完事”。

直播本身在代码里相对独立，点播的入口却长在首页和配置系统里。
机械删除点播文件后，仍然剩下约 190 处点播引用，分布在约 50 个文件里，
必须再做第二步重构才能编译。

## 第一步已做：机械删除的点播文件

- `playback/vod/` 整个目录（点播播放控制器、数据源、预加载、跳过策略等）
- `api/config/VodConfig.java`、`bean/Vod.java`、`browse/VodBrowse.java`
- `model/VideoViewModel.java`、`setting/VideoSetting.java`
- TV 端：`VodActivity`、`VideoActivity`、`VodPresenter`、Vod 相关的 Holder/Input
- 手机端：`VideoActivity`、`VodAdapter`、`VodFragment`、Vod 相关的 Holder
- `ui/dialog/VideoSetting*`、`mobile/bean/CastVideo.java`

删除后核查：`find app/src -name "*Vod*.java"` 已无结果。

## 保留的直播核心（不要删）

- `api/LiveApi.java`、`api/config/LiveConfig.java`、`api/parser/LiveParser.java`
- `bean/Live.java`、`bean/Channel.java`、`browse/LiveBrowse.java`
- `model/LiveViewModel.java`、`impl/LiveListener.java`、`db/dao/LiveDao.java`
- `playback/live/`（LivePlaybackController、LivePlaybackHost 等）
- TV 端和手机端各自的 `LiveActivity`、`LiveAdapter`、`ChannelAdapter`、
  `LiveDialog` 及对应的直播布局、图标
- 共用播放器：`ui/activity/PlaybackActivity.java`、`player/` 下的 Exo/MPV 引擎、
  字幕、弹幕、音视频效果（直播和点播共用这套播放器，不能连同点播一起删）

## 第二步必须做的重构（本原型尚未自动完成，原因见下）

1. 启动入口：把 `AndroidManifest` 的 LAUNCHER 从 `HomeActivity`
   改到 `LiveActivity`，或把 `HomeActivity` 改成只显示直播入口。
   现在的 `HomeActivity` 是点播首页：它用 `VodConfig.get().getHome()`、
   `VodPresenter`、`home_vod` 入口，删点播后它本身就编不过。
2. 配置同步：`LiveConfig.config()` 里有一行
   `this.sync = config.getUrl().equals(VodConfig.getUrl());`
   删掉 `VodConfig` 后要改成只用直播配置自己的 URL 判断，或直接置 false。
3. 首页/搜索/收藏/历史：`HomeActivity`、`SearchActivity`、`CollectActivity`、
   `KeepActivity`、`HistoryActivity`、`SiteApi`、`SiteViewModel`、
   `BrowseSearch`、`BrowseTree` 里大量点播站点、分类、搜索逻辑，
   只保留直播要把这些入口和站点配置 UI 一并拿掉或改成直播源配置。
4. 解析与外播：`player/parse/ParseJob`、`playback/ExternalPlayback`、
   `server/process/Action`、`ParseWebView` 里有指向点播播放的分支，
   要改成只走直播播放链路。
5. 资源清理：点播专用的布局、字符串、菜单项要再扫一遍，否则残留
   的 `home_vod`、`R.string` 引用仍会让编译失败。

## 第二步已做（TV 端，2026-11-04）

- 启动入口：`leanback` 的 LAUNCHER 从 `HomeActivity` 改到 `LiveActivity`，
  并给 `LiveActivity` 加了启动主题；`HomeActivity` 已改成只做跳转的
  壳，保留 `isRunning` / `releaseResources` 供 `ExternalActivity`、
  `RestartActivity`、`NodeRuntime`、`PlaybackService` 继续调用，
  打开即跳直播、随后结束自己。
- 返回逻辑：`LiveActivity` 按返回键时不再绕回 `HomeActivity`
  （原来那样会和跳转壳形成循环），改为直接结束。
- 设置页：TV 端 `SettingActivity` 已去掉点播配置、点播首页、
  点播历史入口，安全 DNS 改用猫影视库自带的默认列表，
  不再依赖 `VodConfig`；直播配置、壁纸配置、播放器、弹幕等入口保留。
- 删掉的 TV 端点播页：`SearchActivity`、`CollectActivity`、
  `KeepActivity` 及配套的 `CollectFragment`、`FolderFragment`、
  `TypeFragment`；清单里的点播活动已从 TV 端 Manifest 清除。
- 引用计数变化：点播引用从约 190 处降到 TV 端 + 共用代码约 84 处。
  剩下的大多不是纯点播页，而是直播间接依赖的共用引擎，
  例如 `SiteApi`（直播弹幕在用它的弹幕方法）、解析、配置同步、
  历史/收藏、服务端推送、外播等，见下一步。

## 第三步已做（干净路线，TV 端，2026-11-04）

- 弹幕抽取：直播弹幕不再走点播站点引擎，校验和爬虫加载逻辑
  已移到 `LiveDanmaku` 内部，爬虫改由 `BaseLoader` 按直播源
  解析（直播源本身带爬虫定义）。注意：原来指向点播站点键的
  弹幕源，必须先移到直播配置里做成直播源，否则弹幕会查不到爬虫。
  `BaseLoader` 也已改成只查直播源，不再回落点播站点。
- 点播站点引擎已删：`SiteApi`、`SiteViewModel` 及点播搜索、
  点播收藏、点播数据类已删；浏览树、搜索、播放记录这三个
  共用模块没有整文件删，而是只留直播分支、删点播分支
  （根目录只剩直播文件夹，搜索只搜直播频道，播放记录只留
  直播收藏，点播断点续播已去掉）。
- 外播重写：外部分享 / 打开文件时，只有直播源（m3u、txt 等）
  会走直播加载；普通本地视频、推送链接不再进点播播放页，
  改为报错结束。直播频道里嵌的 `push://` 链接也不再转点播页，
  改为播放报错（因为那本来就是点播推送，不是直播流）。
- 配置与设置：规则、壁纸、节点运行时、弹幕 API、字幕 Token、
  解析网页爬虫、应用图标等原来读点播配置的地方，已改读
  直播配置或库默认列表；点播解析线路已去掉，`Result.isUseParse`
  恒为假，直连直播不受影响，需要解析线路的频道将无法播放。
- 服务端同步：点播历史同步已关闭，收藏同步改成只同步直播收藏，
  收藏同步的配置加载改走直播配置。
- 点播布局与页面：点播活动、点播详情、点播列表、点播设置、
  点播收藏/历史展示、点播站点选择等布局和页面已删。
- 核查结果：TV 端 + 共用代码里，对已删点播类
  （点播配置、点播浏览、点播播放页、点播站点引擎等）的引用
  已清零。仍保留的“Vod”字样只是历史/收藏数据表里的字段名
  （如频道收藏里的点播名称字段），不是点播功能入口。
- 仍未验证：本机仍无 Android SDK / Gradle，无法在此编译出 APK，
  上面是源码级清零核查，不是编译通过证明。
- 手机端（mobile）尚未做第三步，仍有约 34 处点播配置引用，
  共用文件改动后，手机端现在需要同步做同样的剥离才能编译，
  不能直接用当前共用代码去编手机版。

## 本机验证情况（实话）

- 已验证：源码已拉取、直播文件位置已定位、机械删除已执行、
  剩余引用已用 grep 统计（约 190 处、约 50 个文件）。
- 未验证：没有 Android SDK / Gradle 环境，本机无法编译出 APK，
  所以这份是“源码级剥离原型”，不是可直接安装的成品 APK。
  要出成品，需要在装有 Android SDK 的机器上完成上面第二步重构后再编译。

## 许可证

原项目是 GPL-3.0。本剥离版同样是衍生作品，继续分发时必须保留
GPL-3.0 许可证和原作者声明，不能改成闭源发布。

## 第 4 步：直播不需要弹幕、打开直接进播放页、不要搜索、GitHub 出包

### 1. 弹幕已去掉（直播源本来就不带弹幕）
- 删了 `LiveDanmaku.java`（上一步从点播引擎里抽出来的直播弹幕爬虫，现在不需要了）。
- `PlaybackActivity` 里弹幕开关强制关掉：`danmakuController.setEnabled(false)`，弹幕源永远设为 null，弹幕配置、弹幕发送的回调都改成空操作；`PlayerManager.getSelectedDanmakuUri()` 永远返回 null。
- TV 设置页的弹幕入口删了，`SettingDanmakuActivity` 也删了（TV 清单里已清掉）。
- 注意：`PlayerManager` / `PlaySpec` 里还留着 Media3 弹幕的数据类和回调接口，那是播放核心的共用代码，全删会动到播放器本身；现在它们拿不到任何弹幕源、开关也是关的，直播画面不会出现弹幕。

### 2. 打开软件直接进直播播放页
- 本来就已经是这样：TV 清单里 LAUNCHER 就是 `LiveActivity`，不是点播首页；点播首页只剩一个跳转壳。
- 这一步补了一个缺口：以前打开后自动选了首个频道（无收藏时是第 2 个分组第 1 个频道，即第一个真实分组的首个频道；有收藏时是上次看的频道），但频道列表还盖在画面上。现在 `setPosition(int[])` 自动选台后调用 `hideUI()`，打开就是全屏播放；想换台按切换键调出列表即可。

### 3. 搜索已去掉
- 删了 `BrowseSearch.java` 和 `LiveBrowse.search()`。
- `BrowseTree.search()` / `getSearchResult()` 改成永远返回空列表，`clearSearch()` 改成空操作（调用它的地方不用改）。
- `BrowseLibrary`（Android Auto / 媒体会话那边的搜索）也改成直接返回空，不再建搜索任务。
- 直播页本身没有搜索框，换台只靠分组 / 频道列表。

### 4. 关联 GitHub 是不是就能出 APK？——不是，还差一个编译流程
- 原仓库 `.github/workflows/` 里只有一个 `pages.yml`（编译网站），没有安卓编译流程。光把代码推到 GitHub，什么包也不会出。
- 已经在 `fongmi-live-only/.github/workflows/android.yml` 里加好了流程，推到 GitHub 后在 Actions 里点 Run workflow（或推 main/master/fongmi 分支触发）就能出包，产物在 Actions 的 Artifacts 里下载。
- 流程只编 TV 端：`./gradlew :app:assembleLeanbackRelease`，手机端现在编不过（手机源集还没剥离点播），不要用 `assembleRelease`。
- 签名是关键一环：这个项目的 `app/build.gradle` 读 `local.properties` 里的 keystore，而且 debug 和 release 都用同一把 key；没有 `local.properties` 编译会直接失败。流程里做了两档：
  - 优先用仓库 Secrets：`ANDROID_KEYSTORE_BASE64`（keystore 文件的 base64）、`ANDROID_KEY_ALIAS`、`ANDROID_KEYSTORE_PASSWORD`；
  - 没设 Secrets 时自动生成一把临时 keystore 并把 `keystore.jks` 也当产物上传。注意：临时 key 签的包，下一次换 key 后就没法直接覆盖升级，所以第一次跑完要把那把 keystore 存成 Secret 再重编一次，之后更新才正常。
- 环境：JDK 21、Android SDK（setup-android 自动装）、Gradle 9.7.1（仓库自带 wrapper），产物是 arm64 + arm32 两个分包（`splits.abi` 的设置没变）。
- 仍然没法在这台机器上验证编译：本机没有 Android SDK / JDK，上面是源码级检查（删掉的类已无引用），真机编译结果要以 GitHub Actions 第一次跑的结果为准。
