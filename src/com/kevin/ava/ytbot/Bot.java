package com.kevin.ava.ytbot;


import com.kevin.ava.ytbot.config.BotConfig;
import com.kevin.ava.ytbot.youtube.YoutubeChannel;
import com.kevin.ava.ytbot.youtube.YoutubeChannelChecker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;


public class Bot extends ListenerAdapter {
    private static TextChannel selectedDiscordTextChannel;
    private static final ArrayList<Thread> ytChannelsThreads = new ArrayList<>();
    private static final ArrayList<YoutubeChannelChecker> youtubeChannelsCheckers = new ArrayList<>();
    private static JDA jda;


    /**
     * 確定 Discord 用戶是否為機器人的擁有者。
     * @param userId Discord 用戶的 ID
     * @return 用戶是否為擁有者
     */
    private static boolean isOwner(String userId) {
        return userId.equals(BotConfig.getOwnerUserId());
    }

    public static void broadcastNewVideoMessage(boolean isLiveStream, YoutubeChannelChecker youtubeChannelChecker) {
        selectedDiscordTextChannel = jda.getTextChannelById(BotConfig.getNotificationsChannelID());
        if(selectedDiscordTextChannel == null)
            return;

        if(!isLiveStream) {
            selectedDiscordTextChannel.sendMessage(BotConfig.getNewVideoMessage(youtubeChannelChecker.getYoutubeChannel(),
                    youtubeChannelChecker.getLatestUploadedVideoId())).queue();
        } else {
            selectedDiscordTextChannel.sendMessage(BotConfig.getLivestreamMessage(youtubeChannelChecker.getYoutubeChannel(),
                    youtubeChannelChecker.getLatestUploadedVideoId())).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "輸出調試資料" -> {
                event.deferReply().queue();
                selectedDiscordTextChannel = jda.getTextChannelById(BotConfig.getNotificationsChannelID());
                StringBuilder threadsStates = new StringBuilder();
                for (Thread thread : ytChannelsThreads) {
                    threadsStates.append(thread.getName()).append("\t").append(thread.getState()).append("\n");
                }
                event.getHook().sendMessage("```系統資訊:\n" +
                        "作業系統:\t" + System.getProperty("os.name") + "\n" +
                        "總記憶體:\t" + Runtime.getRuntime().totalMemory() +
                        "b\n空閒記憶體:\t" + Runtime.getRuntime().freeMemory() +
                        "b\n大概的JVM記憶體使用量:\t" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) +"b\n\n" +
                        "機器人資訊:\n" +
                        "last_youtube_videos 檔案清單:\t" + Arrays.asList(new File("last_youtube_videos").list()) +
                        "\n頻道掃描執行緒數量:\t" + ytChannelsThreads.size() + "\n" +
                        threadsStates + "\nDiscord 通知頻道:\t" + selectedDiscordTextChannel.getId() +
                        ", " + selectedDiscordTextChannel.getName() + ", " + selectedDiscordTextChannel.getGuild() + "\n追蹤的 YouTube 頻道:\t" + BotConfig.getChannels() + ", "
                        + BotConfig.getChannels().size() + "\n" +
                        "JVM 正常運行時間:\t" + (ManagementFactory.getRuntimeMXBean().getUptime() / 1000L) + "秒\n" + "```").queue();
            }
            case "設置通知頻道" -> {
                Channel channel = event.getOption("channel").getAsChannel();

                if(!isOwner(event.getUser().getId())) {
                    event.reply("抱歉，這個指令只有授權人員才能使用！").setEphemeral(true).queue();
                    return;
                }
                if(event.getOption("channel") == null) {
                    event.reply("請先指定文字頻道。").queue(); // 無意義的 null 檢查
                    return;
                }
                if(channel.getType() == ChannelType.VOICE) {
                    event.reply("頻道類型錯誤！請選擇正確的文字頻道。").setEphemeral(true).queue();
                    return;
                }

                try {
                    BotConfig.updateNotificationChannel((TextChannel) channel);
                } catch (IOException e) {
                    event.reply("無法更改通知頻道：\n" + e.getMessage()).queue();
                }
                event.reply("已成功更改通知頻道！").queue();
            }
            case "立即檢查新片" -> {
                if(!isOwner(event.getUser().getId())) {
                    event.reply("抱歉，這個指令只有授權人員才能使用！").setEphemeral(true).queue();
                    return;
                }
                boolean wereAnyVideosFound = false;
                for(YoutubeChannelChecker checker : youtubeChannelsCheckers) {
                    boolean hasNewVideos;
                    try {
                        hasNewVideos = checker.hasNewVideos();
                    } catch (URISyntaxException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    if(hasNewVideos) {
                        wereAnyVideosFound = true;
                        broadcastNewVideoMessage(checker.isLiveStream(), checker);
                    }
                }
                int checkIntervalMinutes = BotConfig.getCheckIntervalMinutesv();
                if(!wereAnyVideosFound)
                    event.reply(MarkdownUtil.quoteBlock("未找到新影片。\n註：連續使用此指令將耗盡 Kevin 的 YouTube Data API v3 使用額度，配置預設的自動檢查為每 " + checkIntervalMinutes + " 分鐘進行一次。")).setEphemeral(true).queue();
                else
                    event.reply(MarkdownUtil.quoteBlock("找到新影片!\n註：連續使用此指令將耗盡 Kevin 的 YouTube Data API v3 使用額度，配置預設的自動檢查為每 " + checkIntervalMinutes + " 分鐘進行一次。")).setEphemeral(true).queue();

            }
            case "help" -> event.reply(MarkdownUtil.bold(event.getJDA().getSelfUser().getName()) + " 指令列表：\n\n" +
                    MarkdownUtil.bold("關於：\n") +
                    "`/help` - 顯示此幫助訊息。\n"+
                    "`/輸出調試資料` - 輸出機器人的 debug 資料。\n\n" +
                    MarkdownUtil.bold("授權人員：\n") +
                    "`/設置通知頻道` - 設置所有已綁定 YT 頻道的默認通知頻道。\n" +
                    "`/立即檢查新片` - 強制檢查所有頻道的新影片資料。").queue();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        BotConfig.createConfig();
        jda = JDABuilder.createDefault(BotConfig.getToken())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new Bot())
                .build();
        if(BotConfig.getEnableCommands()) {
            System.out.println("加載斜線指令中...");
            jda.getPresence().setPresence(Activity.playing("加載指令中..."), true);
            jda.updateCommands().addCommands(
                    Commands.slash("輸出調試資料", "輸出機器人 debug 資料。"),
                    Commands.slash("設置通知頻道", "設置所有 YT 頻道的默認通知頻道。")
                            .addOption(OptionType.CHANNEL, "channel", "你希望將通知放置在的頻道。", true),
                    Commands.slash("立即檢查新片", "強制檢查所有頻道的新影片資料。"),
                    Commands.slash("help", "列出所有指令及其使用說明。")
            ).queue();
            System.out.println("加載斜線指令完成!");
        } else {
            System.out.println("以不加載斜線指令的方式啟動中...");
            jda.updateCommands().queue();
        }

        System.out.println("等待 5 秒以完成加載程序...");
        System.out.println("配置的自動檢查間隔: " + BotConfig.getCheckIntervalMinutesv() + " 分鐘");
        Thread.sleep(5000);
        String activityType = BotConfig.getActivityType();
        switch (activityType) {
            case "WATCHING" -> jda.getPresence().setPresence(Activity.watching(BotConfig.getActivityText()), true);
            case "LISTENING" -> jda.getPresence().setPresence(Activity.listening(BotConfig.getActivityText()), true);
            default -> jda.getPresence().setPresence(Activity.playing(BotConfig.getActivityText()), true);
        }

        for(YoutubeChannel youtubeChannel : BotConfig.getChannels()) {
            YoutubeChannelChecker youtubeChannelChecker = new YoutubeChannelChecker(youtubeChannel);
            ytChannelsThreads.add(new Thread(youtubeChannelChecker::checkForNewVideosInLoop));
            youtubeChannelsCheckers.add(youtubeChannelChecker);
        }
        for(Thread thread : ytChannelsThreads)
            thread.start();
    }
}
