package com.taskmanager.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.taskmanager.models.CalendarEventModel;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;


public class CalendarEventDatabase {
    private static CalendarEventDatabase instance;
    private ConnectionSource connectionSource;
    private Dao<CalendarEventModel, Integer> calendarEventDao;
    
    private CalendarEventDatabase() {
        try {
            // 使用與主資料庫相同的連接
            String databaseUrl = "jdbc:h2:./data/taskmanager;AUTO_SERVER=TRUE";
            connectionSource = new JdbcConnectionSource(databaseUrl);
            
            // 建立 DAO
            calendarEventDao = DaoManager.createDao(connectionSource, CalendarEventModel.class);
            
            // 建立表格（如果不存在）
            TableUtils.createTableIfNotExists(connectionSource, CalendarEventModel.class);
            
            System.out.println("CalendarEventDatabase 初始化成功");
        } catch (SQLException e) {
            System.err.println("初始化 CalendarEventDatabase 時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static synchronized CalendarEventDatabase getInstance() {
        if (instance == null) {
            instance = new CalendarEventDatabase();
        }
        return instance;
    }
    
    /**
     * 儲存或更新事件（根據 summary + userEmail + eventDate 去重）
     */
    public void saveOrUpdateEvent(CalendarEventModel event) {
        try {
            // 先檢查是否已存在相同的 Google Event ID 和用戶
            List<CalendarEventModel> existingEventsByGoogleId = calendarEventDao.queryForEq("googleEventId", event.getGoogleEventId());
            existingEventsByGoogleId = existingEventsByGoogleId.stream()
                    .filter(e -> e.getUserEmail().equals(event.getUserEmail()))
                    .toList();
            
            if (!existingEventsByGoogleId.isEmpty()) {
                // 更新現有事件
                CalendarEventModel existingEvent = existingEventsByGoogleId.get(0);
                existingEvent.setSummary(event.getSummary());
                existingEvent.setDescription(event.getDescription());
                existingEvent.setStartTime(event.getStartTime());
                existingEvent.setEndTime(event.getEndTime());
                existingEvent.setEventDate(event.getEventDate());
                existingEvent.setUpdatedAt(System.currentTimeMillis());
                
                calendarEventDao.update(existingEvent);
                System.out.println("更新事件: " + existingEvent.getSummary());
                return;
            }
            
            // 檢查是否已存在相同的 summary + userEmail + eventDate 組合
            List<CalendarEventModel> existingEventsBySummary = calendarEventDao.queryBuilder()
                    .where()
                    .eq("summary", event.getSummary())
                    .and()
                    .eq("userEmail", event.getUserEmail())
                    .and()
                    .eq("eventDate", event.getEventDate())
                    .query();
            
            if (!existingEventsBySummary.isEmpty()) {
                // 更新現有事件（根據 summary 找到的）
                CalendarEventModel existingEvent = existingEventsBySummary.get(0);
                existingEvent.setGoogleEventId(event.getGoogleEventId());
                existingEvent.setDescription(event.getDescription());
                existingEvent.setStartTime(event.getStartTime());
                existingEvent.setEndTime(event.getEndTime());
                existingEvent.setUpdatedAt(System.currentTimeMillis());
                
                calendarEventDao.update(existingEvent);
                System.out.println("根據 summary 更新事件: " + existingEvent.getSummary());
            } else {
                // 建立新事件
                calendarEventDao.create(event);
                System.out.println("建立新事件: " + event.getSummary());
            }
        } catch (SQLException e) {
            System.err.println("儲存事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 批量儲存事件
     */
    public void saveEvents(List<CalendarEventModel> events) {
        for (CalendarEventModel event : events) {
            saveOrUpdateEvent(event);
        }
        System.out.println("批量儲存完成，共 " + events.size() + " 個事件");
    }
    
    /**
     * 獲取指定用戶和日期的事件
     */
    public List<CalendarEventModel> getEventsForUserAndDate(String userEmail, LocalDate date) {
        try {
            return calendarEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .eq("eventDate", date.toString())
                    .query();
        } catch (SQLException e) {
            System.err.println("查詢事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 獲取指定用戶和日期範圍的事件
     */
    public List<CalendarEventModel> getEventsForUserAndDateRange(String userEmail, LocalDate startDate, LocalDate endDate) {
        try {
            return calendarEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .ge("eventDate", startDate.toString())
                    .and()
                    .le("eventDate", endDate.toString())
                    .query();
        } catch (SQLException e) {
            System.err.println("查詢日期範圍事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 刪除指定用戶的舊事件（超過指定日期）
     */
    public void deleteOldEvents(String userEmail, LocalDate beforeDate) {
        try {
            List<CalendarEventModel> oldEvents = calendarEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .lt("eventDate", beforeDate.toString())
                    .query();
            
            for (CalendarEventModel event : oldEvents) {
                calendarEventDao.delete(event);
            }
            
            System.out.println("刪除了 " + oldEvents.size() + " 個舊事件（" + beforeDate + " 之前）");
        } catch (SQLException e) {
            System.err.println("刪除舊事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 獲取最後更新時間
     */
    public long getLastUpdateTime(String userEmail) {
        try {
            List<CalendarEventModel> events = calendarEventDao.queryBuilder()
                    .orderBy("updatedAt", false)
                    .limit(1L)
                    .where()
                    .eq("userEmail", userEmail)
                    .query();
            
            if (!events.isEmpty()) {
                return events.get(0).getUpdatedAt();
            }
        } catch (SQLException e) {
            System.err.println("查詢最後更新時間時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 獲取事件統計資訊
     */
    public String getEventStats(String userEmail) {
        try {
            long totalEvents = calendarEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .countOf();
            
            long todayEvents = calendarEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .eq("eventDate", LocalDate.now().toString())
                    .countOf();
            
            long lastUpdate = getLastUpdateTime(userEmail);
            long hoursElapsed = (System.currentTimeMillis() - lastUpdate) / (1000 * 60 * 60);
            
            return String.format("總事件數: %d, 今日事件: %d, 最後更新: %d 小時前", 
                               totalEvents, todayEvents, hoursElapsed);
        } catch (SQLException e) {
            System.err.println("獲取事件統計時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return "統計資訊不可用";
        }
    }
    
    /**
     * 清理重複事件（根據 summary + userEmail + eventDate）
     */
    public void removeDuplicateEvents(String userEmail) {
        try {
            List<CalendarEventModel> allEvents = calendarEventDao.queryForEq("userEmail", userEmail);
            
            // 使用 Map 來追蹤已見過的事件組合
            java.util.Map<String, CalendarEventModel> uniqueEvents = new java.util.HashMap<>();
            java.util.List<CalendarEventModel> duplicatesToDelete = new java.util.ArrayList<>();
            
            for (CalendarEventModel event : allEvents) {
                String key = event.getSummary() + "|" + event.getUserEmail() + "|" + event.getEventDate();
                
                if (uniqueEvents.containsKey(key)) {
                    // 發現重複，保留較新的事件
                    CalendarEventModel existing = uniqueEvents.get(key);
                    if (event.getUpdatedAt() > existing.getUpdatedAt()) {
                        // 新事件較新，刪除舊的
                        duplicatesToDelete.add(existing);
                        uniqueEvents.put(key, event);
                    } else {
                        // 舊事件較新，刪除新的
                        duplicatesToDelete.add(event);
                    }
                } else {
                    uniqueEvents.put(key, event);
                }
            }
            
            // 刪除重複事件
            for (CalendarEventModel duplicate : duplicatesToDelete) {
                calendarEventDao.delete(duplicate);
            }
            
            System.out.println("已清理 " + duplicatesToDelete.size() + " 個重複事件");
            
        } catch (SQLException e) {
            System.err.println("清理重複事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理所有用戶的重複事件
     */
    public void removeAllDuplicateEvents() {
        try {
            // 獲取所有不同的用戶
            List<CalendarEventModel> allEvents = calendarEventDao.queryForAll();
            java.util.Set<String> userEmails = new java.util.HashSet<>();
            
            for (CalendarEventModel event : allEvents) {
                userEmails.add(event.getUserEmail());
            }
            
            // 為每個用戶清理重複事件
            for (String userEmail : userEmails) {
                removeDuplicateEvents(userEmail);
            }
            
        } catch (SQLException e) {
            System.err.println("清理所有重複事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 關閉資料庫連接
     */
    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
        } catch (Exception e) {
            System.err.println("關閉資料庫連接時發生錯誤: " + e.getMessage());
        }
    }
} 