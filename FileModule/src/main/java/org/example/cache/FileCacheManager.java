package org.example.cache;

import org.example.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.sleep;

/**
 * @author Genius
 * @date 2023/04/24 17:35
 **/

/**
 * 文件自动刷入管理类，不断监听文件是否需要自动写入
 */
public class FileCacheManager {

    private Logger logger = LoggerFactory.getLogger(FileCacheManager.class);
    private final List<FileCache> fileCaches;

    private long sleepTime; //睡眠时间

    private ExecutorService watchPool;  //巡逻线程

    private ExecutorService autoSyncer; //生产者线程

    public FileCacheManager(List<FileCache> fileCaches){
        this.fileCaches = fileCaches;
        initSleepTime();
        this.watchPool = Executors.newSingleThreadExecutor();
        this.autoSyncer = Executors.newFixedThreadPool(fileCaches.size());
    }

    /**
     * 根据文件缓存的刷盘时间得到一个最小的睡眠时间，减少空转
     */
    private void initSleepTime(){
        AtomicLong minSleepTime = new AtomicLong(Long.MAX_VALUE);
        fileCaches.forEach(item->{
            minSleepTime.set(Long.min(minSleepTime.get(), item.getSyncTime()));
        });
        this.sleepTime = minSleepTime.get();
    }

    public void start(){
        this.watchPool.submit(new Watcher());
    }

    class Watcher implements Runnable{

        @Override
        public void run() {
            for(;;){
                long now = TimeUtil.getCurrentSecond();
                for(FileCache cache:fileCaches){
                    BlockingQueue fileChannel = cache.getFileChannel();
                    if(fileChannel.isEmpty()){
                        if(cache.needAutoSync()){
                            logger.debug("检测到需要强制刷新的文件 {}",cache.getFileName());
                            autoSyncer.submit(new AutoSyncer(cache));
                        }
                    }
                }
                now -= TimeUtil.getCurrentSecond();
                if(now<sleepTime){
                    try {
                        Thread.sleep((sleepTime-now)*1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    class AutoSyncer implements Runnable{

        FileCache fileCache;

        public AutoSyncer(FileCache fileCache){
            this.fileCache = fileCache;
        }
        @Override
        public void run() {
            BlockingQueue fileChannel = this.fileCache.getFileChannel();
            if(fileChannel.isEmpty()){
                if(fileCache.needAutoSync()) {
                    fileCache.forceSync();
                }
            }
        }
    }

}