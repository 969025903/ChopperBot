package org.example.core.factory;

import org.example.core.parser.PlatformFlvUrlParser;
import org.example.core.parser.impl.BilibiliFlvUrlParser;
import org.example.core.component.LiveStreamTask;
import org.example.pojo.liveConfig.BilibiliLiveConfig;
import org.example.pojo.liveConfig.LiveConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 直播下载任务制造工产
 * @author 燧枫
 * @date 2023/5/19 20:13
*/
public class LiveTaskFactory {

    private final Map<Class<? extends LiveConfig>, PlatformFlvUrlParser> parserMap;

    public LiveTaskFactory() {
        this.parserMap = new HashMap<>();
        // 添加更多的解析器
        this.parserMap.put(BilibiliLiveConfig.class, new BilibiliFlvUrlParser());
        // 如果你有更多的平台，你可以在这里添加它们的解析器
    }

    // 根据LiveConfig去创建下载任务
    public LiveStreamTask create(LiveConfig liveConfig) {
        PlatformFlvUrlParser parser = this.parserMap.get(liveConfig.getClass());
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported live config type: " + liveConfig.getClass());
        }
        try {
            String flvUrl = parser.getFlvUrl(liveConfig);
            LiveStreamTask task = new LiveStreamTask();
            task.setUrl(flvUrl);
            // 为任务添加请求头
            if (liveConfig instanceof BilibiliLiveConfig) {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
                headers.put("Origin", "https://live.bilibili.com");
                headers.put("Referer", "https://live.bilibili.com/");
                task.setHeaders(headers);
            }
            // 如果有其他平台的直播，可以在这里添加相应的请求头
            return task;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
