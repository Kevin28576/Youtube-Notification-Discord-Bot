package com.kevin.ava.ytbot.config;

import com.kevin.ava.ytbot.utils.ConsoleColors;
import com.kevin.ava.ytbot.youtube.YoutubeChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class BotConfig {
    private static String apiKey;
    private static final ArrayList<YoutubeChannel> youtubeChannels = new ArrayList<>();
    private final static File CONFIG_FILE = new File("config.json");
    private static String notificationsChannelID;
    private static String ownerUserId;
    private static String newVideoMessage;
    private static String livestreamMessage;
    private static String token;
    private static String activityText;
    private static String activityType;
    private static int checkIntervalMinutes;
    private static boolean enableCommands;
    private static boolean enableAPI;
    private static int API_Port;

    /**
     * 讀取指定文件的內容。
     * @param fileToRead 要讀取的文件
     * @return 文件內容的字符串
     * @throws IOException
     */
    public static String readFileString(File fileToRead) throws IOException {
        StringBuilder fileToReadReader = new StringBuilder();
        for(String fileLine : Files.readAllLines(fileToRead.toPath())) {
            fileToReadReader.append(fileLine);
        }
        return fileToReadReader.toString();
    }

    /**
     * 初始化 config.json 文件。
     * @throws IOException
     */
    public static void createConfig() throws IOException {
        if (!CONFIG_FILE.exists()) {
            if (!CONFIG_FILE.createNewFile()) {
                System.out.println(ConsoleColors.RED + "[錯誤] 無法創建 config.json 文件，請檢查系統是否有權限執行此檔案。" + ConsoleColors.RESET);
                System.exit(1);
            } else {
                // 如果文件不存在，填入預設內容
                System.out.println(ConsoleColors.RED_BACKGROUND + "========== [配置文件缺失] ==========" + ConsoleColors.RESET);
                System.out.println(ConsoleColors.YELLOW + "[錯誤] 未發現配置文件，自動創建中。" + ConsoleColors.RESET);
                JSONObject defaultConfig = new JSONObject();
                defaultConfig.put("owner", "DISCORD_OWNER_USER_ID");
                defaultConfig.put("notifications_channel_id", "CHANNEL_FOR_NOTIFICATIONS_ID");

                JSONObject messages = new JSONObject();
                messages.put("new_video", "$CHANNEL published a new video - $VIDEO_LINK");
                messages.put("livestream", "$CHANNEL started a new livestream - $VIDEO_LINK");

                defaultConfig.put("messages", messages);
                defaultConfig.put("youtube_api_key", "YOUR_YOUTUBE_API_KEY");

                JSONArray youtubeChannels = new JSONArray();
                JSONObject channel1 = new JSONObject();
                channel1.put("name", "CHANNEL1_NAME");
                channel1.put("id", "CHANNEL1_ID (https://www.youtube.com/channel/Here_is_the_ID)");
                youtubeChannels.put(channel1);

                JSONObject channel2 = new JSONObject();
                channel2.put("name", "CHANNEL2_NAME");
                channel2.put("id", "CHANNEL2_ID (https://www.youtube.com/channel/Here_is_the_ID)");
                youtubeChannels.put(channel2);

                defaultConfig.put("youtube_channels", youtubeChannels);

                defaultConfig.put("check_interval_minutes", 10);

                JSONObject service = new JSONObject();
                service.put("API", true);
                service.put("API_Port", 3000);
                service.put("Slashcommand", true);

                defaultConfig.put("service", service);

                defaultConfig.put("status_type", "PLAYING / WATCHING / LISTENING");
                defaultConfig.put("status_message", "YOUR_STATUS_MESSAGE");
                defaultConfig.put("token", "YOUR_DISCORD_TOKEN");

                try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                    writer.write(defaultConfig.toString(4));
                    System.out.println(ConsoleColors.YELLOW + "[錯誤] 已自動創建配置文件，請前往填寫。" + ConsoleColors.RESET);
                }
            }
        }
        File lastYoutubeVideoDirectory = new File("last_youtube_videos");
        if(!lastYoutubeVideoDirectory.exists()) {
            if(!lastYoutubeVideoDirectory.mkdir()) {
                System.out.println(ConsoleColors.RED + "[錯誤] 無法創建 \"last_youtube_videos\" 目錄。" + ConsoleColors.RESET);
                System.exit(1);
            }
        }


        JSONObject config = new JSONObject(readFileString(CONFIG_FILE));
        token = config.getString("token");
        ownerUserId = config.getString("owner");
        apiKey = config.getString("youtube_api_key");
        notificationsChannelID = config.getString("notifications_channel_id");
        activityText = config.getString("status_message");
        activityType = config.getString("status_type");
        checkIntervalMinutes = config.getInt("check_interval_minutes");


        JSONArray channels = new JSONArray(config.getJSONArray("youtube_channels"));
        for (int i = 0; i < channels.length(); i++) {
            BotConfig.youtubeChannels.add(new YoutubeChannel(channels.getJSONObject(i).getString("id"), channels.getJSONObject(i).getString("name")));
        }

        JSONObject messages = config.getJSONObject("messages");
        newVideoMessage = messages.getString("new_video");
        livestreamMessage = messages.getString("livestream");

        JSONObject service = config.getJSONObject("service");
        enableAPI = service.getBoolean("API");
        API_Port = service.getInt("API_Port");
        enableCommands = service.getBoolean("Slashcommand");
    }

    /**
     * 更新 Discord 通知頻道並將其寫入配置文件。
     * @param channel 新的通知 Discord 頻道
     * @throws IOException
     */
    public static void updateNotificationChannel(TextChannel channel) throws IOException {
        JSONObject object = new JSONObject(readFileString(CONFIG_FILE));
        object.put("notifications_channel_id", channel.getId());
        notificationsChannelID = channel.getId();

        try(FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(object.toString(4));
        }
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static ArrayList<YoutubeChannel> getChannels() {
        return youtubeChannels;
    }

    public static String getNotificationsChannelID() {
        return notificationsChannelID;
    }

    public static String getOwnerUserId() {
        return ownerUserId;
    }

    public static String getNewVideoMessage(YoutubeChannel youtubeChannel, String youtubeVideoID) {
        return newVideoMessage.replace("$CHANNEL", youtubeChannel.name()).replace("$VIDEO_LINK", "https://www.youtube.com/watch?v=" + youtubeVideoID);
    }

    public static String getLivestreamMessage(YoutubeChannel youtubeChannel, String youtubeVideoID) {
        return livestreamMessage.replace("$CHANNEL", youtubeChannel.name()).replace("$VIDEO_LINK", "https://www.youtube.com/watch?v=" + youtubeVideoID);
    }

    public static String getToken() {
        return token;
    }

    public static String getActivityText() {
        return activityText;
    }

    public static String getActivityType() {
        return activityType;
    }
    public static int getCheckIntervalMinutesv() {
        return checkIntervalMinutes;
    }
    public static boolean getEnableCommands() {
        return enableCommands;
    }
    public static boolean getEnableAPI() {
        return enableAPI;
    }
    public static int getAPIPort() {
        return API_Port;
    }
}
