# 资源与依赖

- 品牌图标来自用户指定的 [ddyec/YIClass](https://github.com/ddyec/YIClass) 中 `assets/icon/app_icon.png`，按用户要求复用；工具栏缩略图由同一图标缩放生成。
- 原生应用、课程核心、存储、导入、AI 与提醒逻辑为本仓库新实现。未移植旧仓库 Dart/Flutter 代码。
- AndroidX、Material Components、Gson、Gradle：Apache License 2.0。
- PDFBox-Android / Apache PDFBox / FontBox：Apache License 2.0；其依赖的 Bouncy Castle 使用自身许可证。具体第三方许可证随依赖提供。
- Robolectric、JUnit：仅用于测试，不打包进 APK；测试中使用的 Robolectric 4.18-beta-1 提供本次 Linux ARM64 环境所需的原生图形/SQLite 运行支持。
- 书页（AI 助手）矢量图标是本工程的简洁几何图形；其余界面图标取自 Google [Material Symbols](https://github.com/google/material-design-icons)（Rounded），Apache License 2.0。课表截图只作为布局参考。
- androidx.graphics:graphics-shapes（Expressive 形状）：Apache License 2.0。

用户原始 PDF 和课表截图没有作为应用内置内容或公共测试附件提交。
