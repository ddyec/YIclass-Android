# YI课表 · Android 原生版

Java + Material 3 Expressive，从空仓库重新实现课程核心、存储和构建。只复用 YIClass 品牌图标，不依赖 Flutter、Dart 或旧应用逻辑。

## 已实现

- 周视图、日视图、全部安排列表；切换周次、回到本周、展开晚间课程。
- 手工新增、编辑、删除课程；连续周次、离散周次、单双周；单次课程和循环课程分开编辑，可只调整/删除某一次。
- 日程与待办：指定日期、钟点、地点、备注、完成状态；与课程共用冲突检测和提醒。
- 本地 PDF 选择、文字坐标解析、逐项勾选预览、去重和确认导入；应用内查看原 PDF 页面；不上传原文件。
- SQLite 持久化、事务与版本检查；JSON 导出、恢复，包含学期、时区、作息与调课例外。
- 提前通知、通知渠道、Android 13+ 通知授权入口、精确提醒授权入口；设备重启/时间变化后恢复，未授权精确提醒时使用系统允许的近似提醒。
- 可调整大小的桌面课表组件，显示今日及接下来七天内最近的安排，点击进入应用。
- 右下角书页入口的 AI 助手，默认 DeepSeek 兼容接口，可配置 HTTPS 地址、模型和 Key；支持咨询、调课、增删课程、排日程；同一次会话支持补充信息。
- AI 仅生成方案：本地验证、变更清单、冲突提示、用户确认、版本检查后一次性入库。单次调课保留其他周次。
- Material 3 Expressive 界面：连接式按钮组、形状变换按钮、分段列表、Expressive 形状、LoadingIndicator、弹性导航栏与 FAB；浅色、深色、跟随系统，Android 12+ 可选动态配色；Android Keystore 加密保存 API Key；JSON 备份不包含 Key。

## 运行与构建

Android 8.0（API 26）起；JDK 21、Android SDK 35。源码使用 Java 17，Gradle Wrapper 已包含。

```sh
# local.properties 中设置 sdk.dir=/your/Android/Sdk
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

调试安装包：`app/build/outputs/apk/debug/app-debug.apk`。

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`assembleRelease` 生成未签名的正式构建；发布前需配置自己的签名，不复用旧项目密钥。GitHub Actions 自动测试并上传调试 APK 和测试报告。

## PDF 导入与验证

导入器根据真实的“星期一…星期日”表头定位列，用文字方向归一化坐标解析课程，不按照原始 PDF 文本输出顺序猜星期。支持附件所用的旋转页面、两页续表、特殊汉字编码、节次、单双周与临时周次。

本次提供的真实 PDF 有 **21 条定时安排**。按原始 PDF 渲染逐列核对：篮球课在星期三，第 5–6 节，4–15 周；第二页含周三第 9–10 节、5–11 单周的课程。参考截图中调课后的布局不会反向覆盖 PDF 数据。

为保护学生姓名和学号，原始 PDF 没有提交到仓库。通过参数在本地复现真实文件测试：

```sh
./gradlew :core:test :app:testDebugUnitTest -Dfixture.pdf=/absolute/path/timetable.pdf
```

这会同时验证桌面 PDFBox 与 Android 实际导入器，并对页面 `/Rotate` 的 0°、90°、180°、270° 验证相同的课程结果。没有私有附件时，这两项测试会明确跳过，其余合成表格与业务测试照常执行。

PDF 本身不提供钟点或开学首周日期。首次导入前，请核对设置中的学校作息和导入预览中的“第1周周一”。未排固定节次的“其他课程/网课”会提示手动补充，不会自动编造星期和时间。扫描件、图片 PDF、非此类表格、镜像或其他无法可靠识别的版式会报错或提示核对；不声称兼容所有学校模板。

## 结构

```text
core/      纯 Java：课程、周次、冲突、时间计算、PDF 表格与 AI 方案验证
app/       Android：Material 3 Expressive 界面、SQLite、SAF 文件访问、PDFBox、提醒、组件、AI
docs/      架构、验证记录与界面截图
```

详情见 [架构](docs/ARCHITECTURE.md)、[验证记录](docs/VALIDATION.md)、[第三方说明](NOTICE.md)。

## 使用边界

提醒准时性受设备权限、省电策略和厂商后台限制影响；设置页会显示授权状态。测试包含本地规则、Android 框架模拟环境下的数据与界面验证，不等同于已在实体手机上验证所有后台状态。真实 DeepSeek 网络调用需要用户配置有效 Key，交付时不内置密钥。

本仓库是可构建的首版原生实现，发布前应在目标手机上完成通知、重启恢复和桌面组件验收。
