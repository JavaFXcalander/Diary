package com.taskmanager.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Event;
import com.google.api.client.util.DateTime;
import com.taskmanager.database.CalendarEventDatabase;
import com.taskmanager.models.CalendarEventModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleCalendarService {
    private static final String APPLICATION_NAME = "Diary";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/credential.json";

    private Calendar service;
    private String userId;
    
   
    public GoogleCalendarService(String userId) {
        this.userId = userId;
    }

    /**
     * 取得授權 URL，並使用 LocalServerReceiver 自動對應 Redirect URI
     */
    public String getAuthorizationUrl() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(new FileInputStream(CREDENTIALS_FILE_PATH))
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(
                        new java.io.File(TOKENS_DIRECTORY_PATH, userId)))
                .setAccessType("offline")
                .build();

        // 使用 LocalServerReceiver 取得正確的 redirect URI
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8889)
                .build();
        String redirectUri = receiver.getRedirectUri();
        System.err.println("redirectUri:" + redirectUri);
        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
    }

    /**
     * 檢查使用者是否已有授權
     */
    public boolean isUserAuthorized() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    JSON_FACTORY,
                    new InputStreamReader(new FileInputStream(CREDENTIALS_FILE_PATH))
            );

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(
                            new java.io.File(TOKENS_DIRECTORY_PATH, userId)))
                    .setAccessType("offline")
                    .build();

            Credential credential = flow.loadCredential(userId);
            if (credential != null && credential.getAccessToken() != null) {
                if (service == null) {
                    service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                            .setApplicationName(APPLICATION_NAME)
                            .build();
                }
                return true;
            }
            return false;
        } catch (IOException | GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * 執行 OAuth 授權流程並初始化 Calendar 服務
     */
    public void authorizeUser() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(new FileInputStream(CREDENTIALS_FILE_PATH))
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(
                        new java.io.File(TOKENS_DIRECTORY_PATH, userId)))
                .setAccessType("offline")
                .build();

        // 使用 LocalServerReceiver 並在授權後自動停止
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8889)
                .build();
        try {
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
                    .authorize(userId);
            service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } finally {
            receiver.stop(); // 授權結束後停止伺服器
        }
    }

    
    
    /**
     * 獲取指定日期的 Google Calendar 事件（優先使用資料庫快取）
     */
    public List<CalendarEvent> getEventsForDate(LocalDate date) throws IOException, GeneralSecurityException {
        // 先嘗試從資料庫獲取事件
        List<CalendarEventModel> dbEvents = CalendarEventDatabase.getInstance()
                .getEventsForUserAndDate(userId, date);
        
        // 檢查資料庫中是否有該日期的事件，且資料不超過 1 小時
        boolean hasRecentData = false;
        if (!dbEvents.isEmpty()) {
            long oldestUpdateTime = dbEvents.stream()
                    .mapToLong(CalendarEventModel::getUpdatedAt)
                    .min()
                    .orElse(0);
            
            // 如果資料更新時間在 1 小時內，則使用資料庫資料
            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
            hasRecentData = oldestUpdateTime > oneHourAgo;
        }
        
        if (hasRecentData) {
            System.out.println("從資料庫載入 " + date + " 的事件（共 " + dbEvents.size() + " 個）");
            // 將資料庫事件轉換為 CalendarEvent
            List<CalendarEvent> calendarEvents = new ArrayList<>();
            for (CalendarEventModel dbEvent : dbEvents) {
                CalendarEvent calendarEvent = new CalendarEvent(
                        dbEvent.getSummary(),
                        dbEvent.getDescription(),
                        dbEvent.getStartTime(),
                        dbEvent.getEndTime()
                );
                calendarEvents.add(calendarEvent);
            }
            return calendarEvents;
        } else {
            System.out.println("從 Google API 載入 " + date + " 的事件");
            return getEventsForDateFromAPI(date);
        }
    }

    /**
     * 直接從 Google API 獲取指定日期的事件（原始方法）
     */
    private List<CalendarEvent> getEventsForDateFromAPI(LocalDate date) throws IOException, GeneralSecurityException {
        if (!isUserAuthorized()) {
            return new ArrayList<>();
        }

        // 設定查詢時間範圍（當天 00:00 到 23:59）
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        // 轉換為 Google API 需要的 DateTime 格式
        DateTime timeMin = new DateTime(Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()));
        DateTime timeMax = new DateTime(Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant()));

        try {
            Events events = service.events().list("primary")
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<CalendarEvent> calendarEvents = new ArrayList<>();
            
            for (Event event : events.getItems()) {
                if (event.getStart() != null && event.getEnd() != null) {
                    long startTime = getEventTime(event.getStart());
                    long endTime = getEventTime(event.getEnd());
                    
                    CalendarEvent calendarEvent = new CalendarEvent(
                            event.getSummary() != null ? event.getSummary() : "無標題事件",
                            event.getDescription() != null ? event.getDescription() : "",
                            startTime,
                            endTime
                    );
                    calendarEvents.add(calendarEvent);
                    
                    // 建立資料庫事件模型並加入待儲存清單
                    CalendarEventModel eventModel = new CalendarEventModel();
                    eventModel.setGoogleEventId(event.getId());
                    eventModel.setUserEmail(userId); // 使用 userId 作為 userEmail
                    eventModel.setSummary(event.getSummary() != null ? event.getSummary() : "無標題事件");
                    eventModel.setDescription(event.getDescription() != null ? event.getDescription() : "");
                    eventModel.setStartTime(startTime);
                    eventModel.setEndTime(endTime);
                    eventModel.setEventDate(date.toString());
                    eventModel.setCreatedAt(System.currentTimeMillis());
                    eventModel.setUpdatedAt(System.currentTimeMillis());
                    
                    CalendarEventDatabase.getInstance().saveOrUpdateEvent(eventModel);
                }
            }
            
            
            return calendarEvents;
        } catch (IOException e) {
            System.err.println("獲取 Google Calendar 事件時發生錯誤: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 獲取指定日期範圍的 Google Calendar 事件
     */
    public List<CalendarEvent> getEventsForDateRange(LocalDate startDate, LocalDate endDate) throws IOException, GeneralSecurityException {
        if (!isUserAuthorized()) {
            return new ArrayList<>();
        }

        LocalDateTime startOfRange = startDate.atStartOfDay();
        LocalDateTime endOfRange = endDate.atTime(23, 59, 59);
        
        DateTime timeMin = new DateTime(Date.from(startOfRange.atZone(ZoneId.systemDefault()).toInstant()));
        DateTime timeMax = new DateTime(Date.from(endOfRange.atZone(ZoneId.systemDefault()).toInstant()));

        try {
            Events events = service.events().list("primary")
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<CalendarEvent> calendarEvents = new ArrayList<>();
            
            for (Event event : events.getItems()) {
                if (event.getStart() != null && event.getEnd() != null) {
                    long startTime = getEventTime(event.getStart());
                    long endTime = getEventTime(event.getEnd());
                    
                    CalendarEvent calendarEvent = new CalendarEvent(
                            event.getSummary() != null ? event.getSummary() : "無標題事件",
                            event.getDescription() != null ? event.getDescription() : "",
                            startTime,
                            endTime
                    );
                    calendarEvents.add(calendarEvent);
                    
                    // 建立資料庫事件模型並加入待儲存清單
                    CalendarEventModel eventModel = new CalendarEventModel();
                    eventModel.setGoogleEventId(event.getId());
                    eventModel.setUserEmail(userId); // 使用 userId 作為 userEmail
                    eventModel.setSummary(event.getSummary() != null ? event.getSummary() : "無標題事件");
                    eventModel.setDescription(event.getDescription() != null ? event.getDescription() : "");
                    eventModel.setStartTime(startTime);
                    eventModel.setEndTime(endTime);
                    
                    // 計算事件所屬的日期
                    LocalDate eventDate = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(startTime), 
                            ZoneId.systemDefault()
                    ).toLocalDate();
                    eventModel.setEventDate(eventDate.toString());
                    
                    eventModel.setCreatedAt(System.currentTimeMillis());
                    eventModel.setUpdatedAt(System.currentTimeMillis());
                    
                    CalendarEventDatabase.getInstance().saveOrUpdateEvent(eventModel);
                }
            }
            
            return calendarEvents;
        } catch (IOException e) {
            System.err.println("獲取 Google Calendar 事件時發生錯誤: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 從 Google Calendar 事件時間中提取毫秒時間戳
     */
    private long getEventTime(com.google.api.services.calendar.model.EventDateTime eventDateTime) {
        if (eventDateTime.getDateTime() != null) {
            return eventDateTime.getDateTime().getValue();
        } else if (eventDateTime.getDate() != null) {
            // 全天事件
            return eventDateTime.getDate().getValue();
        }
        return 0;
    }



    public static class CalendarEvent {
        private String summary;
        private String description;
        private long startTime;
        private long endTime;

        public CalendarEvent(String summary, String description, long startTime, long endTime) {
            this.summary = summary;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getSummary() {
            return summary;
        }

        public String getDescription() {
            return description;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }
    }
}
