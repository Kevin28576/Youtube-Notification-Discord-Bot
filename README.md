# Youtube 通知機器人

Discord 機器人，可通知成員有新的 YouTube 影片發佈。<br>
使用 [JDA library](https://github.com/discord-jda/JDA) 以 Java 編寫。

# 改寫說明

這是我一個用做 Java 的回溫練習項目，根據原作者 myne145 的 [Youtube-Notification-Discord-Bot](https://github.com/myne145/Youtube-Notification-Discord-Bot) 加以進行修改和優化。

## 功能

- 支援多個 YouTube 頻道。
- 自定義通知訊息。
- 自定義 Discord 狀態。
- 自定義檢查間隔。

## 待辦事項:

- 支援多個 Twitch 頻道。
- 支援多個 Discord 伺服器。
- 自動更新器。
- 更多指令以更改配置值。

## 設定:

### Discord

1. 前往 [Discord Developer Portal](https://discord.com/developers/applications)
2. 創建一個新的應用程式並設置一個機器人。
3. 在 **機器人** 部分啟用所有 3 個 Gateway intents (**Presence intent, Server members intent, Message content intent**)
4. 在 OAuth2 -> URL 創建一個帶有所需權限的邀請連結。
5. 獲取機器人 Token 並將其放入配置文件中。

### 本地

1. 安裝 Java 17 或更新版本。
2. 從 [最新版本](https://github.com/Kevin28576/Youtube-Notification-Discord-Bot/releases/latest) 下載 jar 文件。
3. 創建一個名為 **config.json** 的配置文件。(也可選擇初次啟動時自動創建預設文件)
4. 填寫配置文件內容如下：


```
{
    "owner": "DISCORD_OWNER_USER_ID",
    "notifications_channel_id": "CHANNEL_FOR_NOTIFICATIONS_ID",
    "messages": {
        "new_video": "$CHANNEL (用戶頻道) 發佈了新影片 - $VIDEO_LINK (影片連結)",
        "livestream": "$CHANNEL (用戶頻道) 開始了新直播 - $VIDEO_LINK (直播連結)"
    },
    "youtube_api_key": "YOUR_YOUTUBE_API_KEY",
    "youtube_channels": [
        {
            "name": "CHANNEL1_NAME",
            "id": "CHANNEL1_ID (https://www.youtube.com/channel/THE_ID_PART)"
        },
        {
            "name": "CHANNEL2_NAME",
            "id": "CHANNEL2_ID (https://www.youtube.com/channel/THE_ID_PART)"
        }
    ],
    "check_interval_minutes": 10,
    "enable_commands": true,
    "status_type": "PLAYING / WATCHING / LISTENING",
    "status_message": "YOUR_STATUS_MESSAGE",
    "token": "YOUR_DISCORD_TOKEN"
}
```

5. 使用以下命令運行 jar 文件：

```
java -jar <file_name>.jar &
```

<sup>在後面加上 & 可以使 JVM 與終端窗口分離，這樣你就可以直接關閉它並運行。</sup>