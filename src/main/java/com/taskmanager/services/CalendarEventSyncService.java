package com.taskmanager.services;

import com.taskmanager.database.CalendarEventDatabase;
import com.taskmanager.database.DiaryDatabase;
import com.taskmanager.models.CalendarEventModel;
import com.taskmanager.models.DiaryModel;
import com.taskmanager.services.GoogleCalendarService.CalendarEvent;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

public class CalendarEventSyncService {
    private static CalendarEventSyncService instance;
    private final ScheduledExecutorService scheduler;
    private final CalendarEventDatabase eventDatabase;
    private GoogleCalendarService googleCalendarService;
    private String currentUserEmail;
    private DiaryDatabase database = DiaryDatabase.getInstance();

    
    // 同步設定
    private static final long SYNC_INTERVAL_HOURS = 6; // 每6小時同步一次
    private static final int SYNC_MONTHS_AHEAD = 3; // 同步未來3個月的事件
    private static final int KEEP_MONTHS_BEHIND = 1; // 保留過去1個月的事件
    
    private boolean isRunning = false;
    private long lastSyncTime = 0;
    
    private CalendarEventSyncService() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.eventDatabase = CalendarEventDatabase.getInstance();
    }
    
    public static synchronized CalendarEventSyncService getInstance() {
        if (instance == null) {
            instance = new CalendarEventSyncService();
        }
        return instance;
    }
    
    /**
     * 初始化同步服務
     */
    public void initialize(String userEmail) {
        this.currentUserEmail = userEmail;
        try {
            this.googleCalendarService = new GoogleCalendarService(userEmail);
            
            // 清理重複事件
            eventDatabase.removeDuplicateEvents(userEmail);
            
            System.out.println("CalendarEventSyncService 已初始化，用戶: " + userEmail);
        } catch (Exception e) {
            System.err.println("初始化 CalendarEventSyncService 時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 開始定期同步
     */
    public void startPeriodicSync() {
        if (isRunning || currentUserEmail == null || googleCalendarService == null) {
            System.out.println("同步服務已在運行或未正確初始化");
            return;
        }
        
        isRunning = true;
        
        // 立即執行一次同步
        performInitialSync();
        
        // 設定定期同步（每6小時）
        scheduler.scheduleAtFixedRate(
            this::performScheduledSync,
            SYNC_INTERVAL_HOURS,
            SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        );
        
        System.out.println("已啟動定期同步，每 " + SYNC_INTERVAL_HOURS + " 小時同步一次");
    }
    
    /**
     * 停止定期同步
     */
    public void stopPeriodicSync() {
        isRunning = false;
        scheduler.shutdown();
        System.out.println("已停止定期同步");
    }
    
    /**
     * 執行初始同步（應用程式啟動時）
     */
    private void performInitialSync() {
        CompletableFuture.runAsync(() -> {
            try {
                if (!googleCalendarService.isUserAuthorized()) {
                    System.out.println("用戶未授權 Google Calendar，跳過初始同步");
                    return;
                }
                
                System.out.println("開始執行初始同步...");
                
                // 檢查是否需要同步（距離上次同步超過6小時）
                long lastDbUpdate = eventDatabase.getLastUpdateTime(currentUserEmail);
                long hoursElapsed = (System.currentTimeMillis() - lastDbUpdate) / (1000 * 60 * 60);
                
                if (hoursElapsed < SYNC_INTERVAL_HOURS && lastDbUpdate > 0) {
                    System.out.println("距離上次同步僅 " + hoursElapsed + " 小時，跳過初始同步");
                    return;
                }
                
                syncEvents();
                
            } catch (Exception e) {
                System.err.println("初始同步時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 執行排程同步
     */
    private void performScheduledSync() {
        try {
            if (!googleCalendarService.isUserAuthorized()) {
                System.out.println("用戶未授權 Google Calendar，跳過排程同步");
                return;
            }
            
            System.out.println("開始執行排程同步...");
            syncEvents();
            
        } catch (Exception e) {
            System.err.println("排程同步時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 執行事件同步
     */
    private void syncEvents() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusMonths(KEEP_MONTHS_BEHIND);
            LocalDate endDate = today.plusMonths(SYNC_MONTHS_AHEAD);
            
            System.out.println("同步日期範圍: " + startDate + " 到 " + endDate);
            
            // 從 Google Calendar 獲取事件
            List<CalendarEvent> googleEvents = 
                googleCalendarService.getEventsForDateRange(startDate, endDate);
            
            // 轉換為資料庫模型
            List<CalendarEventModel> dbEvents = convertToDbEvents(googleEvents);
            
            // 儲存到本地資料庫
            eventDatabase.saveEvents(dbEvents);
            
            // 清理舊事件
            eventDatabase.deleteOldEvents(currentUserEmail, startDate);
            
            lastSyncTime = System.currentTimeMillis();
            
            System.out.println("同步完成，共處理 " + dbEvents.size() + " 個事件");
            
        } catch (Exception e) {
            System.err.println("同步事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 轉換 Google Calendar 事件為資料庫模型
     */
    private List<CalendarEventModel> convertToDbEvents(List<CalendarEvent> googleEvents) {
        List<CalendarEventModel> dbEvents = new ArrayList<>();
        
        for (CalendarEvent googleEvent : googleEvents) {
            try {
                // 計算事件日期
                LocalDate eventDate = Instant.ofEpochMilli(googleEvent.getStartTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                
                // 生成唯一的 Google Event ID（如果沒有的話）
                String googleEventId = generateEventId(googleEvent);
                
                CalendarEventModel dbEvent = new CalendarEventModel(
                    googleEventId,
                    currentUserEmail,
                    googleEvent.getSummary(),
                    googleEvent.getDescription(),
                    googleEvent.getStartTime(),
                    googleEvent.getEndTime(),
                    eventDate
                );
                
                // 設置是否為整日事件
                
                dbEvents.add(dbEvent);
                
            } catch (Exception e) {
                System.err.println("轉換事件時發生錯誤: " + e.getMessage());
            }
        }
        
        return dbEvents;
    }
    
    /**
     * 生成事件 ID（基於事件內容的雜湊）
     */
    private String generateEventId(CalendarEvent event) {
        String content = event.getSummary() + "_" + event.getStartTime() + "_" + event.getEndTime();
        return String.valueOf(content.hashCode());
    }
    
    /**
     * 手動觸發同步
     */
    public void manualSync() {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("手動觸發同步...");
                syncEvents();
            } catch (Exception e) {
                System.err.println("手動同步時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 從本地資料庫獲取事件（優先使用本地資料）
     */
    public List<CalendarEvent> getEventsForDate(LocalDate date) {
        try {
            // 首先從本地資料庫獲取
            List<CalendarEventModel> dbEvents = eventDatabase.getEventsForUserAndDate(currentUserEmail, date);
            
            if (!dbEvents.isEmpty()) {
                System.out.println("從本地資料庫載入 " + date + " 的事件: " + dbEvents.size() + " 個");
                return convertToGoogleEvents(dbEvents);
            }
            
            // 如果本地沒有，且用戶已授權，從 Google API 獲取
            if (googleCalendarService != null && googleCalendarService.isUserAuthorized()) {
                // 先檢查是否已經標記為空
                DiaryModel entry = database.getDiaryEntry(date, UserSession.getInstance().getCurrentUserEmail());
                if (entry != null && entry.isCalendarEmpty()) {
                    System.out.println("該日期已標記為空，跳過API呼叫");
                    return new ArrayList<>();
                }
                
                System.out.println("本地無資料，從 Google API 載入 " + date + " 的事件");
                List<CalendarEvent> googleEvents = googleCalendarService.getEventsForDate(date);
                
                // 如果從 Google API 獲取的事件為空，標記該日期為空
                if (googleEvents.isEmpty()) {
                    if (entry == null) {
                        entry = new DiaryModel(date);
                        entry.setUser(database.getUserEntry(UserSession.getInstance().getCurrentUserEmail()));
                    }
                    entry.setCalendarEmpty(true);
                    database.saveDiaryEntry(entry);
                    System.out.println("該日期沒有事件，已標記為空");
                }
                
                return googleEvents;
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            System.err.println("獲取事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 轉換資料庫事件為 Google Calendar 事件
     */
    private List<CalendarEvent> convertToGoogleEvents(List<CalendarEventModel> dbEvents) {
        List<CalendarEvent> googleEvents = new ArrayList<>();
        
        for (CalendarEventModel dbEvent : dbEvents) {
            // 根據時間判斷是否為整日事件
            boolean isAllDay = isAllDayFromTimes(dbEvent.getStartTime(), dbEvent.getEndTime());
            
            CalendarEvent googleEvent = 
                new CalendarEvent(
                    dbEvent.getSummary(),
                    dbEvent.getDescription(),
                    dbEvent.getStartTime(),
                    dbEvent.getEndTime(),
                    isAllDay
                );
            googleEvents.add(googleEvent);
        }
        
        return googleEvents;
    }
    
    /**
     * 根據開始和結束時間判斷是否為全天事件
     */
    private boolean isAllDayFromTimes(long startTime, long endTime) {
        java.time.LocalDateTime startDateTime = java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTime), 
                java.time.ZoneId.systemDefault()
        );
        java.time.LocalDateTime endDateTime = java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(endTime), 
                java.time.ZoneId.systemDefault()
        );
        
        java.time.LocalTime startTime_local = startDateTime.toLocalTime();
        java.time.LocalTime endTime_local = endDateTime.toLocalTime();
        
        // Google Calendar 整日事件：開始時間是 08:00，結束時間也是 08:00
        boolean googleAllDay = startTime_local.equals(java.time.LocalTime.of(8, 0)) &&
                              endTime_local.equals(java.time.LocalTime.of(8, 0));
        
        return googleAllDay;
    }
    
    /**
     * 獲取同步狀態資訊
     */
    public String getSyncStatus() {
        if (currentUserEmail == null) {
            return "同步服務未初始化";
        }
        
        String dbStats = eventDatabase.getEventStats(currentUserEmail);
        long hoursElapsed = (System.currentTimeMillis() - lastSyncTime) / (1000 * 60 * 60);
        
        return String.format("同步狀態: %s, 上次同步: %d 小時前, %s", 
                           isRunning ? "運行中" : "已停止", hoursElapsed, dbStats);
    }
    
    /**
     * 檢查是否需要同步
     */
    public boolean needsSync() {
        if (lastSyncTime == 0) return true;
        
        long hoursElapsed = (System.currentTimeMillis() - lastSyncTime) / (1000 * 60 * 60);
        return hoursElapsed >= SYNC_INTERVAL_HOURS;
    }
    
    /**
     * 關閉服務
     */
    public void shutdown() {
        stopPeriodicSync();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        System.out.println("CalendarEventSyncService 已關閉");
    }
} 