# APP 接口文档

- 接口前缀：`/app`
- 认证：白名单免鉴权（初始化与上报接口，详见 `JwtAuthFilter` 放行 `/app/**`）
- 请求头：`Content-Type: application/json`
- 返回封装：统一使用 `Result<T>`，字段：`code`、`message`、`success`、`data`

## 1）APP 初始化
- 路径：`POST /app/init`
- 入参：`AppInitRequest`
```json
{
  "packageName": "com.example.demo",
  "channelName": "oppo",
  "version": "1.2.3"
}
```
- 行为与约束：
  - 按 `packageName + channelName` 精确匹配 `products`；仅考虑 `is_deleted=0` 且 `placement_status='enabled'` 的记录
  - 版本匹配：仅考虑“请求版本 ≤ 数据库版本”的候选，从中选择更贴近请求版本的数据库版本
  - 屏蔽策略：通过 `BlockStrategyUtil.check(channelName, adcode)`；不通过则关闭广告
- 返回：`AppInitResponse`
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": {
    "adEnabled": 1,
    "adStrategy": "strict",
    "pangleAppId": "5722897",
    "adSplashId": "88776655",
    "adInterstitialId": "11223344",
    "adBannerId": "99887766",
    "adRewardedId": "55443322",
    "downloadUrl": "https://example.com/app.apk",
    "umengKey": "UMENG_KEY_xxx",
    "apihzId": "10007306",
    "apihzKey": "jiangxileihekj"
  }
}
```
- 字段说明：
  - `adEnabled`：0 关闭，1 开启
  - `adStrategy`：广告策略（`adEnabled=1` 时返回，否则为空）
  - 其他广告位与三方信息：同上，仅当开启时返回；否则为空字符串

## 2）广告策略校验
- 路径：`POST /app/ad-strategy/check`
- 入参：`AppUserInitRequest`
```json
{
  "packageName": "com.example.demo",
  "channelName": "vivo",
  "version": "1.2.3",
  "oaid": "A1B2C3D4E5",
  "deviceInfo": "brand=model; os=Android 14; ..."
}
```
- 逻辑：
  1. 记录用户（近 24h 内存在则不新增）
  2. 查询开关 `CHANNEL_UPPER + "_AD_EXACT_MATCH_SWITCH"`（如 `VIVO_AD_EXACT_MATCH_SWITCH`）
     - 为 `0`：直接返回空字符串
  3. 开关为 `1`：查询渠道点击表最近 7×24 小时是否存在该 `oaid`
     - 存在：返回空字符串
     - 不存在：读取 `CHANNEL_UPPER + "_PRECISE_AD_STRATEGY"` 的配置值作为策略返回
- 返回示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": "strict"
}
```
- 入参校验：`packageName/channelName/version/oaid` 必填；`channelName` 统一转小写；`deviceInfo` 去换行并截断 ≤ 1024 字符

## 3）用户初始化
- 路径：`POST /app/user-init`
- 入参：`AppUserInitRequest`（同上）
- 逻辑：近 24 小时按 `packageName+channelName+version+oaid` 查询 `customers`，存在则返回既有记录ID，否则新增并返回新记录ID
- 返回示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": 1008611
}
```

## 4）记录（广告交互上报）
- 路径：`POST /app/record`
- 入参：`AppRecordRequest`
```json
{
  "oaid": "A1B2C3D4E5",
  "ecpm": 123.45,
  "adType": "splash",
  "interactionType": "view",
  "deviceInfo": "brand=model; os=Android 14; ..."
}
```
- 逻辑：
  - 仅按 `oaid` 在近 24 小时内查找 `customers` 最近一条记录；未找到则返回成功但不写库
  - 更新规则：
    - `ecpm` 取较大值（保留两位）
    - `ltv = ltv + ecpm/1000`（保留两位）
    - 交互计数：`view` → `ad_view_count+1`；`click` → `ad_click_count+1`
  - 写入 `customer_ad_details` 明细：`customerId/adType/ecpm/interactionType/createdAt`
- 返回：`Result<Long>`（客户ID，未匹配到时返回 `data=null`）
- 入参校验：
  - `oaid`、`ecpm`、`adType`、`interactionType` 必填
  - `ecpm` 非负；`adType` 枚举：`splash/interstitial/banner/rewarded`；`interactionType` 枚举：`view/click`

## 5）客户反馈
- 路径：`POST /app/feedback`
- 入参：`AppFeedbackRequest`（兼容下划线/驼峰）
```json
{
  "package_name": "com.example.demo",
  "channel_name": "oppo",
  "version": "1.2.3",
  "subject": "功能建议",
  "content": "这里是详细反馈内容...",
  "contact": "user@example.com"
}
```
- 逻辑：必填字段校验通过后，写入 `customer_feedback` 表；`contact` 为空或空白则存 `NULL`
- 返回：`Result<String>`，成功值为 `"完成"`
- 入参校验：`package_name/channel_name/version/subject/content` 必填

## 字段与清洗规则
- `channelName`：统一转小写
- `deviceInfo`：去除换行，最长 1024 字符
- 时间与时区：服务端统一 `Asia/Shanghai`
- 返回封装：所有接口返回均为 `Result<T>`，异常时返回 `code=500` 与错误消息

## 示例请求（curl）
```bash
curl -X POST 'http://localhost:9091/app/init' \
  -H 'Content-Type: application/json' \
  -d '{"packageName":"com.example.demo","channelName":"oppo","version":"1.2.3"}'
```