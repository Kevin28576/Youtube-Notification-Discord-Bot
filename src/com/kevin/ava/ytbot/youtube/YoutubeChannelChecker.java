package com.kevin.ava.ytbot.youtube;

import com.kevin.ava.ytbot.Bot;
import com.kevin.ava.ytbot.config.BotConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class YoutubeChannelChecker {
    private JSONObject lastVideoFromPreviousCheck;
    private final YoutubeChannel channelToCheck;

    public YoutubeChannelChecker(YoutubeChannel channelToCheck) {
        this.channelToCheck = channelToCheck;
    }

    /**
     * 判斷新影片是否為直播。
     * @return 該影片是否為直播。
     */
    public boolean isLiveStream() {
        if (lastVideoFromPreviousCheck.getJSONObject("snippet").has("liveBroadcastContent")) {
//  ㄈ          System.out.println(lastVideoFromPreviousCheck.toString());
            String type = lastVideoFromPreviousCheck.getJSONObject("snippet").getString("liveBroadcastContent");
            return type.equals("live");
        }
        return false;
    }

    /**
     * 檢查 YouTube 頻道是否有新影片。
     * @return 頻道是否有新影片。
     * @throws URISyntaxException
     * @throws IOException
     */
    public boolean hasNewVideos() throws URISyntaxException, IOException {
        URL requestURL =
                new URI("https://www.googleapis.com/youtube/v3/search?key=" +
                        BotConfig.getApiKey() + "&channelId=" + channelToCheck.id() + "&part=snippet,id&order=date&maxResults=20").toURL();
        HttpURLConnection urlConnection = (HttpURLConnection) requestURL.openConnection();
        urlConnection.setRequestMethod("GET");

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(urlConnection.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line).append("\n");
            }
        }
//        String result = BotConfig.readFileString(new File(channelToCheck.name() + ".json"));


        JSONObject response = new JSONObject(result.toString());
        JSONArray videos = new JSONArray(response.getJSONArray("items"));

        if (videos.length() <= 0) {
            return false; //channel has no videos uploaded
        }
        JSONObject lastVideoFromCurrentCheck = videos.getJSONObject(0);

        File lastYoutubeVideoFile = new File("last_youtube_videos/" + getYoutubeChannel().name() + ".json");
//        File lastYoutubeVideoFile = new File("last_youtube_videos/last_youtube_video_" + getYoutubeChannel().name() + ".json");
        if (!lastYoutubeVideoFile.exists())
            lastYoutubeVideoFile.createNewFile();

        boolean isLastVideoJSONFileValid = true;
        try {
            new JSONObject(BotConfig.readFileString(lastYoutubeVideoFile));
        } catch (Exception e) {
            isLastVideoJSONFileValid = false;
        }

        if (lastVideoFromPreviousCheck == null || !isLastVideoJSONFileValid) {
            lastVideoFromPreviousCheck = lastVideoFromCurrentCheck;
        }
        if (isLastVideoJSONFileValid) {
            lastVideoFromPreviousCheck = new JSONObject(BotConfig.readFileString(lastYoutubeVideoFile));
        }
        boolean areVideosTheSame = lastVideoFromCurrentCheck.getJSONObject("id").getString("videoId")
                .equals(lastVideoFromPreviousCheck.getJSONObject("id").getString("videoId"));

        lastVideoFromPreviousCheck = lastVideoFromCurrentCheck;
        try (FileWriter writer = new FileWriter(lastYoutubeVideoFile)) {
            writer.write(lastVideoFromCurrentCheck.toString(4));
        }
        return !areVideosTheSame;
    }


    /**
     * 每 X 分鐘循環調用 {@link #hasNewVideos()}。
     */
    public static int cont = 0;
    public static int checkIntervalMinutes = BotConfig.getCheckIntervalMinutesv();
    public void checkForNewVideosInLoop() {
        while (true) {
            try {
                cont++;
//                System.out.println("檢查第 " + cont + " 次 | " + checkIntervalMinutes + " 分鐘檢查一次。");
                boolean tempNewVideos = hasNewVideos();
                if (tempNewVideos) {
                    Bot.broadcastNewVideoMessage(isLiveStream(), this);
                }
                TimeUnit.MINUTES.sleep(checkIntervalMinutes); // 抓取配置的 check_interval_minutes 時間
//                Thread.sleep(1000);
            } catch (InterruptedException | URISyntaxException | IOException e) {
                System.out.println("出現錯誤:");
                throw new RuntimeException(e);
            }
        }
    }


    public YoutubeChannel getYoutubeChannel() {
        return channelToCheck;
    }

    /**
     * 如果有新影片上傳，獲取其 YouTube ID。
     * @return 最新影片的 YouTube ID
     */
    public String getLatestUploadedVideoId() {
        return lastVideoFromPreviousCheck.getJSONObject("id").getString("videoId");
    }
}
