package com.taskmanager.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.taskmanager.database.CalendarEventDatabase;
import com.taskmanager.models.MoodleModel;

public class MoodleService {
    private static final String API_ENTRY = "https://moodle.ncku.edu.tw/webservice/rest/server.php";
    private static final String LOGIN_URL = "https://moodle.ncku.edu.tw/login/token.php";
    
    private static MoodleService instance;
    private String wstoken;
    private HttpClient httpClient;
    private Gson gson;
    private CalendarEventDatabase eventDatabase;
    
    // 緩存控制 - 24小時內不重複從API獲取
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24小時
    
    private MoodleService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.eventDatabase = CalendarEventDatabase.getInstance();
    }
    
    /**
     * 獲取 MoodleService 單例實例
     */
    public static synchronized MoodleService getInstance() {
        if (instance == null) {
            instance = new MoodleService();
        }
        return instance;
    }
    
    /**
     * 使用用戶名和密碼登錄 Moodle
     */
    public boolean login(String username, String password) throws IOException, InterruptedException {
        return login(username, password, null);
    }
    
    /**
     * 使用用戶名和密碼登錄 Moodle（帶用戶郵箱）
     */
    public boolean login(String username, String password, String userEmail) throws IOException, InterruptedException {
        String loginUrl = String.format("%s?username=%s&password=%s&service=moodle_mobile_app",
                LOGIN_URL,
                URLEncoder.encode(username, StandardCharsets.UTF_8),
                URLEncoder.encode(password, StandardCharsets.UTF_8));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        
        if (result.has("errorcode")) {
            System.err.println("Moodle login failed: " + result.toString());
            return false;
        }
        
        this.wstoken = result.get("token").getAsString();
        
        // 保存憑證到數據庫
        if (userEmail != null) {
            saveCredentials(userEmail, username, password);
        }
        
        return true;
    }
    
    /**
     * 自動恢復已保存的登錄狀態
     */
    public boolean autoRestoreLogin(String userEmail) {
        com.taskmanager.database.DiaryDatabase database = com.taskmanager.database.DiaryDatabase.getInstance();
        com.taskmanager.models.UserModel user = database.getUserEntry(userEmail);
        
        if (user != null && user.hasMoodleCredentials()) {
            try {
                // 使用保存的用戶名重新登錄以獲取新的token
                // 注意：我們不保存密碼，所以這裡需要重新輸入
                // 或者我們可以設計為token過期時提示用戶重新登錄
                this.wstoken = user.getMoodleToken();
                System.out.println("嘗試使用已保存的Moodle憑證");
                
                // 驗證token是否仍然有效
                if (isTokenValid()) {
                    System.out.println("自動恢復Moodle登錄狀態成功");
                    return true;
                } else {
                    System.out.println("保存的Moodle token已過期，需要重新登錄");
                    clearSavedCredentials(userEmail);
                    return false;
                }
            } catch (Exception e) {
                System.err.println("自動登錄失敗: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * 驗證當前token是否有效
     */
    private boolean isTokenValid() {
        try {
            String url = String.format("%s?moodlewsrestformat=json&wsfunction=core_webservice_get_site_info&wstoken=%s",
                    API_ENTRY, wstoken);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            
            return !result.has("errorcode");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 保存登錄憑證到數據庫
     */
    private void saveCredentials(String userEmail, String username, String password) {
        if (wstoken == null) return;
        
        com.taskmanager.database.DiaryDatabase database = com.taskmanager.database.DiaryDatabase.getInstance();
        com.taskmanager.models.UserModel user = database.getUserEntry(userEmail);
        
        if (user != null) {
            user.setMoodleToken(wstoken);
            user.setMoodleUsername(username);
            // 注意：我們不保存密碼以確保安全性
            user.setMoodleLastLoginTime(System.currentTimeMillis());
            database.saveUserEntry(user);
            System.out.println("Moodle憑證已保存到數據庫");
        }
    }
    
    /**
     * 清除保存的憑證
     */
    public void clearSavedCredentials(String userEmail) {
        com.taskmanager.database.DiaryDatabase database = com.taskmanager.database.DiaryDatabase.getInstance();
        com.taskmanager.models.UserModel user = database.getUserEntry(userEmail);
        
        if (user != null) {
            user.clearMoodleCredentials();
            database.saveUserEntry(user);
            this.wstoken = null;
            System.out.println("Moodle憑證已清除");
        }
    }
    
    /**
     * 獲取指定月份的日曆事件
     */
    public List<MoodleEvent> getCalendarEvents(LocalDate date) throws IOException, InterruptedException {
        return getCalendarEventsWithCache(date, null);
    }
    
    /**
     * 獲取指定月份的日曆事件（帶緩存功能）
     */
    public List<MoodleEvent> getCalendarEventsWithCache(LocalDate date, String userEmail) throws IOException, InterruptedException {
        if (userEmail == null) {
            // 沒有用戶信息，直接從API獲取
            return getCalendarEventsFromAPI(date);
        }
        
        // 獲取月份的第一天，作為緩存的鍵
        LocalDate monthStart = date.withDayOfMonth(1);
        
        // 檢查該月份是否需要更新
        long lastUpdate = eventDatabase.getMoodleLastUpdateTime(userEmail);
        boolean needUpdate = System.currentTimeMillis() - lastUpdate > CACHE_DURATION;
        
        // 優先從本地資料庫載入該月份的所有事件
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        List<MoodleModel> localEvents = eventDatabase.getMoodleEventsForUserAndDateRange(userEmail, monthStart, monthEnd);
        
        // 如果有本地數據且不需要更新，直接返回該日期的事件
        if (!localEvents.isEmpty() && !needUpdate) {
            List<MoodleEvent> monthEvents = convertToMoodleEvents(localEvents);
            List<MoodleEvent> dayEvents = monthEvents.stream()
                    .filter(event -> {
                        LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
                        return eventDate.equals(date);
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            if (!dayEvents.isEmpty()) {
                System.out.println("從本地資料庫載入 " + date + " 的事件: " + dayEvents.size() + " 個 (緩存有效)");
            }
            return dayEvents;
        }
        
        // 如果需要更新，同步整個月份的數據（只做一次）
        if (needUpdate) {
            try {
                System.out.println("從API同步整個月份 " + monthStart + " 到 " + monthEnd + " 的 Moodle 事件");
                
                // 清除該月份的舊數據
                eventDatabase.deleteOldMoodleEvents(userEmail, monthEnd.plusDays(1));
                
                // 獲取整個月的數據（一次API調用）
                List<MoodleEvent> monthEvents = getCalendarEventsFromAPI(monthStart);
                
                // 保存到本地資料庫
                if (!monthEvents.isEmpty()) {
                    saveMoodleEventsToDatabase(monthEvents, userEmail);
                    System.out.println("同步完成，共 " + monthEvents.size() + " 個事件");
                }
                
                // 返回指定日期的事件
                return monthEvents.stream()
                        .filter(event -> {
                            LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
                            return eventDate.equals(date);
                        })
                        .collect(java.util.stream.Collectors.toList());
                        
            } catch (Exception e) {
                System.err.println("載入 Moodle 事件時發生錯誤 (" + date + "): " + e.getMessage());
                // 如果API失敗，返回本地緩存的數據（即使過期）
                if (!localEvents.isEmpty()) {
                    System.out.println("API失敗，使用本地緩存數據");
                    List<MoodleEvent> monthEvents = convertToMoodleEvents(localEvents);
                    return monthEvents.stream()
                            .filter(event -> {
                                LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
                                return eventDate.equals(date);
                            })
                            .collect(java.util.stream.Collectors.toList());
                }
                return new ArrayList<>();
            }
        }
        
        // 返回本地事件中該日期的事件
        List<MoodleEvent> monthEvents = convertToMoodleEvents(localEvents);
        return monthEvents.stream()
                .filter(event -> {
                    LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
                    return eventDate.equals(date);
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 從API獲取日曆事件
     */
    private List<MoodleEvent> getCalendarEventsFromAPI(LocalDate date) throws IOException, InterruptedException {
        int year = date.getYear();
        int month = date.getMonthValue();
        
        String payload = String.format("year=%d&month=%d", year, month);
        String url = String.format("%s?moodlewsrestformat=json&wsfunction=core_calendar_get_calendar_monthly_view&wstoken=%s",
                API_ENTRY, wstoken);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        
        if (result.has("errorcode")) {
            throw new RuntimeException("Failed to get calendar events: " + result.toString());
        }
        
        List<MoodleEvent> events = parseCalendarEvents(result);
        
        // 獲取作業詳細信息和繳交狀態
        if (!events.isEmpty()) {
            enrichEventsWithSubmissionStatus(events);
        }
        
        return events;
    }
    
    /**
     * 從本地資料庫獲取 Moodle 事件
     */
    public List<MoodleModel> getLocalMoodleEvents(String userEmail, LocalDate date) {
        return eventDatabase.getMoodleEventsForUserAndDate(userEmail, date);
    }
    
    /**
     * 獲取指定日期範圍的本地 Moodle 事件
     */
    public List<MoodleModel> getLocalMoodleEventsRange(String userEmail, LocalDate startDate, LocalDate endDate) {
        return eventDatabase.getMoodleEventsForUserAndDateRange(userEmail, startDate, endDate);
    }
    
    /**
     * 將 MoodleEvent 保存到資料庫
     */
    private void saveMoodleEventsToDatabase(List<MoodleEvent> events, String userEmail) {
        if (events.isEmpty()) {
            return;
        }
        
        List<MoodleModel> moodleModels = new ArrayList<>();
        
        // 使用 Set 來追蹤已處理的事件，避免重複
        java.util.Set<String> processedEvents = new java.util.HashSet<>();
        
        for (MoodleEvent event : events) {
            // 創建唯一鍵：assignmentId + courseId + timestart
            String eventKey = event.getAssignmentId() + "_" + event.getCourseId() + "_" + event.getTimestart();
            
            if (processedEvents.contains(eventKey)) {
                continue; // 跳過重複事件
            }
            processedEvents.add(eventKey);
            
            MoodleModel model = new MoodleModel();
            model.setUserEmail(userEmail);
            model.setName(event.getName());
            model.setTimestart(event.getTimestart());
            model.setUrl(event.getUrl());
            model.setCourseName(event.getCourseName());
            model.setCourseId(event.getCourseId());
            model.setAssignmentId(event.getAssignmentId());
            model.setSubmissionStatus(event.getSubmissionStatus());
            
            // 設置事件日期
            LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
            model.setEventDateFromLocalDate(eventDate);
            
            moodleModels.add(model);
        }
        
        // 批量保存到資料庫（使用改良的方法）
        eventDatabase.saveMoodleEvents(moodleModels);
        
        System.out.println("保存了 " + moodleModels.size() + " 個唯一的 Moodle 事件");
    }
    
    /**
     * 將 MoodleModel 轉換為 MoodleEvent
     */
    private List<MoodleEvent> convertToMoodleEvents(List<MoodleModel> models) {
        List<MoodleEvent> events = new ArrayList<>();
        
        for (MoodleModel model : models) {
            MoodleEvent event = new MoodleEvent();
            event.setId(model.getId());
            event.setName(model.getName());
            
            event.setTimestart(model.getTimestart());
            event.setUrl(model.getUrl());
            event.setCourseName(model.getCourseName());
            event.setCourseId(model.getCourseId());
            event.setAssignmentId(model.getAssignmentId());
            event.setSubmissionStatus(model.getSubmissionStatus());
            
            events.add(event);
        }
        
        return events;
    }
    
    /**
     * 強制同步指定用戶的 Moodle 事件
     */
    public void forceSyncMoodleEvents(String userEmail, LocalDate startDate, LocalDate endDate) {
        try {
            System.out.println("強制同步 Moodle 事件：" + startDate + " 到 " + endDate);
            
            // 收集整個範圍的事件，避免重複API調用
            List<MoodleEvent> allEvents = new ArrayList<>();
            
            // 按月份獲取，因為API是按月份提供數據的
            LocalDate currentMonth = startDate.withDayOfMonth(1);
            java.util.Set<String> processedMonths = new java.util.HashSet<>();
            
            while (!currentMonth.isAfter(endDate)) {
                String monthKey = currentMonth.toString().substring(0, 7); // YYYY-MM
                
                if (!processedMonths.contains(monthKey)) {
                    processedMonths.add(monthKey);
                    
                    try {
                        List<MoodleEvent> monthEvents = getCalendarEventsFromAPI(currentMonth);
                        
                        // 過濾出在指定範圍內的事件
                        for (MoodleEvent event : monthEvents) {
                            LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
                            if (!eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)) {
                                allEvents.add(event);
                            }
                        }
                        
                        System.out.println("同步了 " + monthKey + " 的 " + monthEvents.size() + " 個事件");
                    } catch (Exception e) {
                        System.err.println("同步 " + monthKey + " 的事件失敗: " + e.getMessage());
                    }
                }
                
                currentMonth = currentMonth.plusMonths(1);
            }
            
            // 保存所有事件到資料庫
            if (!allEvents.isEmpty()) {
                saveMoodleEventsToDatabase(allEvents, userEmail);
            }
            
            System.out.println("強制同步 Moodle 事件完成，共處理 " + allEvents.size() + " 個事件");
        } catch (Exception e) {
            System.err.println("強制同步 Moodle 事件失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清除用戶的 Moodle 事件緩存
     */
    public void clearMoodleCache(String userEmail) {
        eventDatabase.clearMoodleEvents(userEmail);
        System.out.println("已清除用戶 " + userEmail + " 的 Moodle 事件緩存");
    }
    
    /**
     * 解析日曆事件數據
     */
    private List<MoodleEvent> parseCalendarEvents(JsonObject calendarData) {
        List<MoodleEvent> events = new ArrayList<>();
        
        if (!calendarData.has("weeks")) {
            return events;
        }
        
        JsonArray weeks = calendarData.getAsJsonArray("weeks");
        for (JsonElement weekElement : weeks) {
            JsonObject week = weekElement.getAsJsonObject();
            JsonArray days = week.getAsJsonArray("days");
            
            for (JsonElement dayElement : days) {
                JsonObject day = dayElement.getAsJsonObject();
                if (day.has("events")) {
                    JsonArray dayEvents = day.getAsJsonArray("events");
                    
                    for (JsonElement eventElement : dayEvents) {
                        JsonObject eventObj = eventElement.getAsJsonObject();
                        
                        // 只處理作業事件
                        if (eventObj.has("modulename") && 
                            "assign".equals(eventObj.get("modulename").getAsString())) {
                            
                            MoodleEvent event = parseMoodleEvent(eventObj);
                            if (event != null) {
                                events.add(event);
                            }
                        }
                    }
                }
            }
        }
        
        return events;
    }
    
    /**
     * 解析單個 Moodle 事件
     */
    private MoodleEvent parseMoodleEvent(JsonObject eventObj) {
        try {
            MoodleEvent event = new MoodleEvent();
            
            event.setId(eventObj.get("id").getAsInt());
            event.setName(eventObj.get("name").getAsString());
            event.setDescription(eventObj.has("description") ? eventObj.get("description").getAsString() : "");
            event.setTimestart(eventObj.get("timestart").getAsLong());
            event.setUrl(eventObj.has("url") ? eventObj.get("url").getAsString() : "");
            
            // 解析作業ID (instance 字段)
            if (eventObj.has("instance")) {
                event.setAssignmentId(eventObj.get("instance").getAsInt());
            }
            
            // 解析課程信息
            if (eventObj.has("course")) {
                JsonObject courseObj = eventObj.getAsJsonObject("course");
                event.setCourseName(courseObj.get("fullname").getAsString());
                event.setCourseId(courseObj.get("id").getAsInt());
            }
            
            return event;
        } catch (Exception e) {
            System.err.println("Failed to parse Moodle event: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 豐富事件信息，添加繳交狀態
     */
    private void enrichEventsWithSubmissionStatus(List<MoodleEvent> events) throws IOException, InterruptedException {
        // 收集所有課程ID
        Map<Integer, List<MoodleEvent>> eventsByCourse = new HashMap<>();
        for (MoodleEvent event : events) {
            eventsByCourse.computeIfAbsent(event.getCourseId(), k -> new ArrayList<>()).add(event);
        }
        
        // 為每個課程獲取作業信息
        for (Integer courseId : eventsByCourse.keySet()) {
            try {
                JsonObject assignments = getAssignments(courseId);
                Map<Integer, JsonObject> assignmentMap = parseAssignments(assignments);
                
                // 獲取繳交狀態
                List<Integer> assignmentIds = new ArrayList<>(assignmentMap.keySet());
                if (!assignmentIds.isEmpty()) {
                    Map<Integer, JsonObject> submissionStatus = getSubmissionStatus(assignmentIds);
                    
                    // 更新事件信息
                    for (MoodleEvent event : eventsByCourse.get(courseId)) {
                        JsonObject assignment = assignmentMap.get(event.getAssignmentId());
                        if (assignment != null) {
                            JsonObject submission = submissionStatus.get(event.getAssignmentId());
                            updateEventWithSubmissionInfo(event, assignment, submission);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to get assignment info for course " + courseId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * 獲取課程的作業信息
     */
    private JsonObject getAssignments(int courseId) throws IOException, InterruptedException {
        String payload = String.format("courseids[0]=%d", courseId);
        String url = String.format("%s?moodlewsrestformat=json&wsfunction=mod_assign_get_assignments&wstoken=%s",
                API_ENTRY, wstoken);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
    
    /**
     * 解析作業信息
     */
    private Map<Integer, JsonObject> parseAssignments(JsonObject assignmentsData) {
        Map<Integer, JsonObject> assignmentMap = new HashMap<>();
        
        if (assignmentsData.has("courses")) {
            JsonArray courses = assignmentsData.getAsJsonArray("courses");
            for (JsonElement courseElement : courses) {
                JsonObject course = courseElement.getAsJsonObject();
                if (course.has("assignments")) {
                    JsonArray assignments = course.getAsJsonArray("assignments");
                    for (JsonElement assignElement : assignments) {
                        JsonObject assignment = assignElement.getAsJsonObject();
                        int assignmentId = assignment.get("id").getAsInt();
                        assignmentMap.put(assignmentId, assignment);
                    }
                }
            }
        }
        
        return assignmentMap;
    }
    
    /**
     * 獲取作業繳交狀態
     */
    private Map<Integer, JsonObject> getSubmissionStatus(List<Integer> assignmentIds) throws IOException, InterruptedException {
        Map<Integer, JsonObject> statusMap = new HashMap<>();
        
        for (Integer assignmentId : assignmentIds) {
            try {
                String payload = String.format("assignid=%d", assignmentId);
                String url = String.format("%s?moodlewsrestformat=json&wsfunction=mod_assign_get_submission_status&wstoken=%s",
                        API_ENTRY, wstoken);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                
                if (!result.has("errorcode")) {
                    statusMap.put(assignmentId, result);
                }
            } catch (Exception e) {
                System.err.println("Failed to get submission status for assignment " + assignmentId + ": " + e.getMessage());
            }
        }
        
        return statusMap;
    }
    
    /**
     * 更新事件的繳交信息
     */
    private void updateEventWithSubmissionInfo(MoodleEvent event, JsonObject assignment, JsonObject submission) {
        try {
            // 設置作業ID
            event.setAssignmentId(assignment.get("id").getAsInt());
            
            // 檢查繳交狀態
            if (submission.has("lastattempt")) {
                JsonObject lastAttempt = submission.getAsJsonObject("lastattempt");
                
                // 檢查個人繳交或團隊繳交
                JsonObject submissionObj = null;
                if (lastAttempt.has("submission") && !lastAttempt.get("submission").isJsonNull()) {
                    submissionObj = lastAttempt.getAsJsonObject("submission");
                } else if (lastAttempt.has("teamsubmission") && !lastAttempt.get("teamsubmission").isJsonNull()) {
                    submissionObj = lastAttempt.getAsJsonObject("teamsubmission");
                }
                
                if (submissionObj != null && submissionObj.has("status")) {
                    String status = submissionObj.get("status").getAsString();
                    event.setSubmissionStatus(status);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to update event with submission info: " + e.getMessage());
        }
    }
    
    public String getWstoken() {
        return wstoken;
    }
    
    /**
     * Moodle 事件數據類
     */
    public static class MoodleEvent {
        private int id;
        private String name;
        private String description;
        private long timestart;
        private String url;
        private String courseName;
        private int courseId;
        private int assignmentId;
        private String submissionStatus;
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public long getTimestart() { return timestart; }
        public void setTimestart(long timestart) { this.timestart = timestart; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        
        public int getCourseId() { return courseId; }
        public void setCourseId(int courseId) { this.courseId = courseId; }
        
        public int getAssignmentId() { return assignmentId; }
        public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }
                
        public String getSubmissionStatus() { return submissionStatus; }
        public void setSubmissionStatus(String submissionStatus) { this.submissionStatus = submissionStatus; }
        
        /**
         * 獲取事件的截止日期
         */
        public LocalDate getDueDate() {
            return LocalDate.ofEpochDay(timestart / 86400);
        }
        
        /**
         * 獲取簡短的顯示文本
         */
        public String getDisplayText() {
            return String.format("[%s] %s", courseName, name);
        }
        
        public String getStatusColor() {
            // 如果已繳交
            if ("submitted".equals(submissionStatus)) {
                return "#10b981"; // 綠色
            }
            
            // 如果逾期
            if (timestart * 1000 < System.currentTimeMillis()) {
                return "#10b981"; // 紅色
            }
            
            // 未繳交但未逾期
            return "#f59e0b"; // 黃色
        }
        
        /**
         * 獲取狀態描述
         */
        public String getStatusDescription() {
           
            
            if ("submitted".equals(submissionStatus)) {
                return "已繳交";
            }
            
            if (timestart * 1000 < System.currentTimeMillis()) {
                return "已繳交";
            }
            
            return "未繳交";
        }
        
        @Override
        public String toString() {
            return String.format("MoodleEvent{id=%d, name='%s', course='%s', timestart=%d}", 
                    id, name, courseName, timestart);
        }
    }
} 