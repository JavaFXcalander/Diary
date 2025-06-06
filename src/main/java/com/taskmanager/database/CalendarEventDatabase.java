package com.taskmanager.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.taskmanager.models.CalendarEventModel;
import com.taskmanager.models.MoodleModel;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;


public class CalendarEventDatabase {
    private static CalendarEventDatabase instance;
    private ConnectionSource connectionSource;
    private Dao<CalendarEventModel, Integer> calendarEventDao;
    private Dao<MoodleModel, Integer> moodleEventDao;

    private CalendarEventDatabase() {
        try {
            // 使用與主資料庫相同的連接
            String databaseUrl = "jdbc:h2:./data/taskmanager;AUTO_SERVER=TRUE";
            connectionSource = new JdbcConnectionSource(databaseUrl);
            
            // 建立 DAO
            calendarEventDao = DaoManager.createDao(connectionSource, CalendarEventModel.class);
            moodleEventDao = DaoManager.createDao(connectionSource, MoodleModel.class);

            
            // 建立表格（如果不存在）
            TableUtils.createTableIfNotExists(connectionSource, CalendarEventModel.class);
            TableUtils.createTableIfNotExists(connectionSource, MoodleModel.class);

            
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
            System.out.println("嘗試儲存事件: " + event.getSummary() + " (" + event.getEventDate() + ") GoogleID: " + event.getGoogleEventId());
            
            // 檢查是否已存在相同的 Google Event ID（現在包含日期）
            List<CalendarEventModel> existingEventsByGoogleId = calendarEventDao.queryBuilder()
                    .where()
                    .eq("googleEventId", event.getGoogleEventId())
                    .and()
                    .eq("userEmail", event.getUserEmail())
                    .query();
            
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
                System.out.println("根據GoogleID更新事件: " + existingEvent.getSummary() + " (" + existingEvent.getEventDate() + ") ID:" + existingEvent.getId());
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
                System.out.println("根據summary更新事件: " + existingEvent.getSummary() + " (" + existingEvent.getEventDate() + ") ID:" + existingEvent.getId());
            } else {
                // 建立新事件
                calendarEventDao.create(event);
                System.out.println("建立新事件: " + event.getSummary() + " (" + event.getEventDate() + ") ID:" + event.getId());
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
        int created = 0;
        int updated = 0;
        
        for (CalendarEventModel event : events) {
            try {
                // 記錄操作前的事件總數
                long beforeCount = calendarEventDao.queryBuilder()
                        .where()
                        .eq("googleEventId", event.getGoogleEventId())
                        .and()
                        .eq("userEmail", event.getUserEmail())
                        .countOf();
                
            saveOrUpdateEvent(event);
                
                // 檢查是否是新建還是更新
                if (beforeCount == 0) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                System.err.println("批量保存事件時發生錯誤: " + e.getMessage());
            }
        }
        
        System.out.println("批量儲存完成 - 新建: " + created + " 個，更新: " + updated + " 個事件，總共處理: " + events.size() + " 個");
    }
    
    /**
     * 獲取指定用戶和日期的事件
     */
    public List<CalendarEventModel> getEventsForUserAndDate(String userEmail, LocalDate date) {
        try {
            System.out.println("查詢資料庫: userEmail=" + userEmail + ", date=" + date.toString());
            
            List<CalendarEventModel> events = calendarEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .eq("eventDate", date.toString())
                    .query();
            
            System.out.println("資料庫查詢結果: " + events.size() + " 個事件");
            for (CalendarEventModel event : events) {
                System.out.println("  - " + event.getSummary() + " (ID:" + event.getId() + ")");
            }
            
            return events;
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
    
    // ==== Moodle 事件管理方法 ====
    
    /**
     * 儲存或更新 Moodle 事件
     */
    public void saveOrUpdateMoodleEvent(MoodleModel event) {
        try {
            // 檢查是否已存在相同的事件（根據 assignmentId + courseId + userEmail + eventDate）
            List<MoodleModel> existingEvents = moodleEventDao.queryBuilder()
                    .where()
                    .eq("assignmentId", event.getAssignmentId())
                    .and()
                    .eq("courseId", event.getCourseId())
                    .and()
                    .eq("userEmail", event.getUserEmail())
                    .and()
                    .eq("eventDate", event.getEventDate())
                    .query();
            
            if (!existingEvents.isEmpty()) {
                // 更新現有事件
                MoodleModel existingEvent = existingEvents.get(0);
                boolean hasChanges = false;
                
                if (!existingEvent.getName().equals(event.getName())) {
                    existingEvent.setName(event.getName());
                    hasChanges = true;
                }
                if (existingEvent.getTimestart() != event.getTimestart()) {
                    existingEvent.setTimestart(event.getTimestart());
                    hasChanges = true;
                }
                if (!java.util.Objects.equals(existingEvent.getSubmissionStatus(), event.getSubmissionStatus())) {
                    existingEvent.setSubmissionStatus(event.getSubmissionStatus());
                    hasChanges = true;
                }
                
                if (hasChanges) {
                    existingEvent.setUpdatedAt(System.currentTimeMillis());
                    moodleEventDao.update(existingEvent);
                    // 只在有實際變化時輸出日誌
                    System.out.println("更新 Moodle 事件: " + existingEvent.getName());
                }
                // 如果沒有變化，不輸出日誌
            } else {
                // 建立新事件
                moodleEventDao.create(event);
                System.out.println("建立新 Moodle 事件: " + event.getName());
            }
        } catch (SQLException e) {
            System.err.println("儲存 Moodle 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 批量儲存 Moodle 事件
     */
    public void saveMoodleEvents(List<MoodleModel> events) {
        if (events.isEmpty()) {
            return;
        }
        
        int newEvents = 0;
        int updatedEvents = 0;
        
        for (MoodleModel event : events) {
            try {
                // 檢查是否已存在相同的事件
                List<MoodleModel> existingEvents = moodleEventDao.queryBuilder()
                        .where()
                        .eq("assignmentId", event.getAssignmentId())
                        .and()
                        .eq("courseId", event.getCourseId())
                        .and()
                        .eq("userEmail", event.getUserEmail())
                        .and()
                        .eq("eventDate", event.getEventDate())
                        .query();
                
                if (existingEvents.isEmpty()) {
                    moodleEventDao.create(event);
                    newEvents++;
                } else {
                    // 靜默更新，不輸出日誌
                    MoodleModel existingEvent = existingEvents.get(0);
                    existingEvent.setName(event.getName());
                    existingEvent.setTimestart(event.getTimestart());
                    existingEvent.setSubmissionStatus(event.getSubmissionStatus());
                    existingEvent.setUpdatedAt(System.currentTimeMillis());
                    moodleEventDao.update(existingEvent);
                    updatedEvents++;
                }
            } catch (SQLException e) {
                System.err.println("儲存 Moodle 事件時發生錯誤: " + e.getMessage());
            }
        }
        
        System.out.println("批量儲存完成 - 新增: " + newEvents + " 個，更新: " + updatedEvents + " 個事件");
    }
    
    /**
     * 獲取指定用戶和日期的 Moodle 事件
     */
    public List<MoodleModel> getMoodleEventsForUserAndDate(String userEmail, LocalDate date) {
        try {
            return moodleEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .eq("eventDate", date.toString())
                    .query();
        } catch (SQLException e) {
            System.err.println("查詢 Moodle 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 獲取指定用戶和日期範圍的 Moodle 事件
     */
    public List<MoodleModel> getMoodleEventsForUserAndDateRange(String userEmail, LocalDate startDate, LocalDate endDate) {
        try {
            return moodleEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .ge("eventDate", startDate.toString())
                    .and()
                    .le("eventDate", endDate.toString())
                    .query();
        } catch (SQLException e) {
            System.err.println("查詢 Moodle 日期範圍事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 清除指定用戶的舊 Moodle 事件
     */
    public void deleteOldMoodleEvents(String userEmail, LocalDate beforeDate) {
        try {
            List<MoodleModel> oldEvents = moodleEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .lt("eventDate", beforeDate.toString())
                    .query();
            
            for (MoodleModel event : oldEvents) {
                moodleEventDao.delete(event);
            }
            
            System.out.println("刪除了 " + oldEvents.size() + " 個舊 Moodle 事件（" + beforeDate + " 之前）");
        } catch (SQLException e) {
            System.err.println("刪除舊 Moodle 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理重複的 Moodle 事件
     */
    public void removeDuplicateMoodleEvents(String userEmail) {
        try {
            List<MoodleModel> allEvents = moodleEventDao.queryForEq("userEmail", userEmail);
            
            java.util.Map<String, MoodleModel> uniqueEvents = new java.util.HashMap<>();
            java.util.List<MoodleModel> duplicatesToDelete = new java.util.ArrayList<>();
            
            for (MoodleModel event : allEvents) {
                String key = event.getAssignmentId() + "|" + event.getUserEmail() + "|" + event.getEventDate();
                
                if (uniqueEvents.containsKey(key)) {
                    MoodleModel existing = uniqueEvents.get(key);
                    if (event.getUpdatedAt() > existing.getUpdatedAt()) {
                        duplicatesToDelete.add(existing);
                        uniqueEvents.put(key, event);
                    } else {
                        duplicatesToDelete.add(event);
                    }
                } else {
                    uniqueEvents.put(key, event);
                }
            }
            
            for (MoodleModel duplicate : duplicatesToDelete) {
                moodleEventDao.delete(duplicate);
            }
            
            System.out.println("已清理 " + duplicatesToDelete.size() + " 個重複 Moodle 事件");
            
        } catch (SQLException e) {
            System.err.println("清理重複 Moodle 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 獲取 Moodle 事件最後更新時間
     */
    public long getMoodleLastUpdateTime(String userEmail) {
        try {
            List<MoodleModel> events = moodleEventDao.queryBuilder()
                    .orderBy("updatedAt", false)
                    .limit(1L)
                    .where()
                    .eq("userEmail", userEmail)
                    .query();
            
            if (!events.isEmpty()) {
                return events.get(0).getUpdatedAt();
            }
        } catch (SQLException e) {
            System.err.println("查詢 Moodle 最後更新時間時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 清除指定用戶的所有 Moodle 事件（重新同步時使用）
     */
    public void clearMoodleEvents(String userEmail) {
        try {
            List<MoodleModel> userEvents = moodleEventDao.queryForEq("userEmail", userEmail);
            for (MoodleModel event : userEvents) {
                moodleEventDao.delete(event);
            }
            System.out.println("已清除 " + userEvents.size() + " 個 Moodle 事件");
        } catch (SQLException e) {
            System.err.println("清除 Moodle 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理特定事件在錯誤日期的記錄
     */
    public void cleanupIncorrectEventRecords(String userEmail, String eventSummary, LocalDate incorrectDate) {
        try {
            List<CalendarEventModel> incorrectEvents = calendarEventDao.queryBuilder()
                    .where()
                    .eq("userEmail", userEmail)
                    .and()
                    .eq("summary", eventSummary)
                    .and()
                    .eq("eventDate", incorrectDate.toString())
                    .query();
            
            for (CalendarEventModel event : incorrectEvents) {
                calendarEventDao.delete(event);
                System.out.println("刪除錯誤的事件記錄: " + event.getSummary() + " (" + event.getEventDate() + ") ID:" + event.getId());
            }
            
            if (incorrectEvents.size() > 0) {
                System.out.println("已清理 " + incorrectEvents.size() + " 個錯誤的事件記錄");
            }
            
        } catch (SQLException e) {
            System.err.println("清理錯誤事件記錄時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 