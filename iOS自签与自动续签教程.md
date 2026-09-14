# SideStore 使用教程：iOS 自签与自动续签

用免费 Apple ID 在非越狱 iPhone / iPad 上安装 IPA，到期前自动刷新，避免 7 天后打不开。

---

## 你需要准备

| 项目 | 说明 |
|------|------|
| 手机 | iPhone 或 iPad，系统 **iOS 14 及以上**，已设置锁屏密码 |
| 电脑 | Windows / Mac / Linux，**只在第一次安装时用** |
| 账号 | 一个 Apple ID（免费即可） |
| 网络 | Wi-Fi 正常 |

手机上先从 App Store 安装 **StosVPN**（安装和刷新应用时都要开着）。

---

## 第一步：安装电脑工具 iloader

### Windows

1. 安装 iTunes（必须从 [Apple 官网](https://apple.co/ms)下载，不要用微软商店版）
2. 下载并安装 [iloader](https://github.com/nab138/iloader/releases)

### Mac

1. 下载 [iloader](https://github.com/nab138/iloader/releases)（选对芯片：M 系列或 Intel）
2. 打开终端，执行：

```bash
sudo xattr -c /Applications/iloader.app
```

### Linux

1. 安装 `usbmuxd`
2. 从 [发布页](https://github.com/nab138/iloader/releases) 下载对应发行版的安装包

---

## 第二步：把 SideStore 装到手机上

### 电脑上操作

1. 用数据线连接手机，解锁，信任此电脑  
2. 打开 **iloader**  
3. 登录你的 Apple ID（注意大小写）  
4. 选择设备 → 点 **Install SideStore (Stable)**  

### 手机上操作

**iPhone 系统是 iOS 15：**

1. 设置 → 通用 → **VPN 与设备管理**  
2. 点你的 Apple 邮箱 → **验证 / 信任**  
3. 打开 **StosVPN** → Connect  
4. 打开 **SideStore** → 登录同一 Apple ID  
5. 进入 **My Apps** → 点 SideStore 旁边的 **「7 DAYS」** → **Refresh Now**

**iPhone 系统是 iOS 16 或更高：**

在信任证书之后，还要多做一步：

1. 设置 → 隐私与安全性 → 打开 **开发者模式**（手机会重启）  
2. 重启后：开 StosVPN → 打开 SideStore → 登录 → My Apps → 点天数 → **Refresh Now**

刷新成功后 SideStore 会退回桌面，几秒后就能再打开。

> 装好后**先刷新 SideStore 自己**，再装别的 App。

---

## 第三步：安装你的 IPA

1. 确认 StosVPN 已连接  
2. 打开 SideStore  
3. 选择你要安装的 `.ipa` 文件  
4. 等待签名安装完成  
5. 如果提示不受信任：设置 → 通用 → VPN 与设备管理 → 信任  

也可以添加社区源，从里面直接装应用：

```
https://community-apps.sidestore.io/sidecommunity.json
```

---

## 自动续签（最重要）

免费 Apple ID 签名的 App **只能用 7 天**。到期前要在 SideStore 里「刷新」，否则打不开。

### 怎么刷新

1. 打开 StosVPN → Connect  
2. 打开 SideStore → **My Apps**  
3. 点某个 App 旁的 **「X DAYS」**（X 是剩余天数）  
4. 等一会儿就好了  

SideStore 也会尝试在后台自动刷新，但不一定每次都能成功。  
**建议：剩余 1～2 天时自己点一次 Refresh，别等过期。**

---

## 免费账号能装多少个

| 限制 | 数量 |
|------|------|
| 同时可用的 App | **3 个**（SideStore 自己算 1 个） |
| 一周内注册的 App ID | **10 个** |
| 签名有效期 | **7 天** |

想多装、装得久：

- 花 $99 / 年开通 Apple 开发者账号（约 365 天，不限 3 个）
- 或在 SideStore 里「停用」不用的 App，腾出名额

---

## 常见问题

**刷新时失败 / 提示 AFC 连接失败**  
检查 StosVPN 是否已连接、Wi-Fi 是否正常。还不行就重启 SideStore 和 VPN，或用 iloader 重新导入配对文件。

**登录失败**  
多半是验证服务器临时挂了。在 SideStore 设置里换一个 Anisette 服务器地址。

**系统升级后 SideStore 用不了**  
配对文件可能失效，用 iloader 重新安装 SideStore，并按官方指南重新导入 pairing file。

**从 AltStore 换过来**  
先别删手机上的旧 App。直接在 SideStore 里装同一个（或更新的）IPA，数据一般会保留。

**手机设置里出现奇怪的 Mac**  
正常现象，是登录时自动生成的，不是真的电脑。

**iOS 16 以上打不开侧载 App**  
去 设置 → 隐私与安全性，打开 **开发者模式**。

---

## 日常使用清单

```
手机已设锁屏密码
App Store 已装 StosVPN
电脑已装 iloader，SideStore 已装到手机
已信任开发者证书
iOS 16+ 已打开开发者模式
SideStore 登录成功，自身已 Refresh
已安装需要的 IPA
每 5～6 天：开 VPN → SideStore → My Apps → Refresh
```

---

## 官方链接

- 教程文档：https://docs.sidestore.io/zh/  
- 项目主页：https://github.com/SideStore/SideStore  
- 问题求助：https://dis.sidestore.io  
