# 架构与数据约定

## 数据

`Item` 表示循环课程或指定日期的安排。`start/end` 是课表时区中当天零点起的分钟，区间为 `[start,end)`；因此两门首尾相接的课不会被判定冲突。`day` 使用 ISO 星期 1–7；`weeks` 保存 1–30 周内的范围、离散周次或单双周。`date` 非空时按明确日期安排，周次不参与发生时间计算。

学期起点必须是周一。周次不截断为 1，开学前不会误显示第一周。循环课程的 `excludedDates` 保存取消或调课的原始日期；仅修改本次时，给循环课程增加例外，再生成新的指定日期安排。

SQLite 存储 `id + JSON payload`，数据库版本与备份格式均为 1。每次写入先验证整套数据，再检查 revision 并在一个数据库事务内替换；课程编辑、导入、AI 和恢复备份使用同一入口。所有视图、提醒和组件读取同一数据源。设置保存在 SharedPreferences；首周日期等设置变更后重建提醒与组件。备份先完整解析验证，再由用户确认覆盖；异常文件不写入数据库。

## PDF

SAF 只读取用户选择的文件，不申请整个存储空间权限。20MB、40页、30万文字位置上限，解析在后台线程执行。PDFBox 提供文字方向校正后的 `getXDirAdj/getYDirAdj`，纯 Java 解析器按行识别真实星期表头并分列。续页复用列边界，而不是把第二页的晚课归到第一个星期。Unicode NFKC 归一化处理部首式汉字与全角符号。所有条目先预览、去重、确认，再保存；原始元数据保存在课程备注。

未识别的表头、扫描件与空导入不落库。没有固定节次的网课不编造时间；导入页提醒用户核对并补充。应用内 PdfRenderer 预览尊重原页面旋转，并限制位图大小；临时预览副本关闭后删除。

## 提醒与组件

AlarmManager 维护一个最近触发点：最近的课程提醒或下一次课表时区午夜。午夜刷新可滚动安排后续提醒并更新桌面组件。课程变化、启动/回到应用、系统重启、应用更新、时间/时区变更、精确提醒授权变化均重建计划。Receiver 用异步 PendingResult 完成数据库读取和重排。

未获得精确提醒权限时使用 `setAndAllowWhileIdle`，不冒充准时提醒；通知授权独立处理。送达时重新验证安排仍存在、时间未改变、未完成，避免修改后发旧通知；相同课程/日期/起点不会重复发。超过一小时的陈旧提醒不会补发。桌面 RemoteViews 展示接下来七天最近五项安排，并随数据变化、系统周期和午夜刷新；不是常驻服务。

## AI

默认模板：`https://api.deepseek.com`，模型 ID `deepseek-chat`。地址和模型可修改；只允许 HTTPS、不跟随重定向、使用平台默认 TLS 信任、不记录 Key 或完整响应。网络调用有连接/读取超时和响应大小上限。

API Key 通过 Android Keystore AES-GCM 加密后保存在应用私有 SharedPreferences；不会写入课程备份。应用禁用自动云备份和系统设备转移，以免导出不可迁移的 Keystore 密文。助手入口明确说明请求会发送当前课表；不会后台自动上传 PDF。

JSON mode 结果解析为 `Plan`，网络客户端没有数据库写入能力。最多50项操作；本地核对 ID、字段、日期与周次；产生候选整表并计算冲突。确认时再核对数据库 revision、首周日期、时区。单次修改和整个循环明确区分。相同助手面板内保留最近四轮对话用于追问；关闭后不持久化聊天。

## 界面与构建

AppCompat + `Theme.Material3.DayNight`，MaterialToolbar、分段按钮、BottomNavigationView、FloatingActionButton、TextInputLayout、MaterialCardView、MaterialCheckBox、BottomSheetDialog 和 MaterialAlertDialogBuilder。周表采用原生可点击卡片，支持无障碍描述与重叠并列展示，日/列表视图提供完整课程文本。所有 UI 都是 Android View，不嵌套网页或 Flutter 引擎。

Java 17 + AGP 8.11.1 + Gradle 8.14.3，JDK 21 构建；Java 核心与 Android I/O 分离。启用核心库脱糖支持 Android 8.0 起的 Java 时间/集合 API。CI 使用标准 SDK 工具；本次云环境中的 ARM 主机资源编译工具适配没有写入工程。
