package com.taskmanager.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.concurrent.Task;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OllamaService {
    private static OllamaService instance;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String model;
    private List<Map<String, String>> conversationHistory;
    private List<String> weeklyAnynotes; // 存储这一周的anynotes内容
    
    private OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        this.baseUrl = "http://localhost:11434"; // Ollama默認端口
        this.model = "llama3.2";
        this.conversationHistory = new ArrayList<>();
        this.weeklyAnynotes = new ArrayList<>();
    }
    
    public static synchronized OllamaService getInstance() {
        if (instance == null) {
            instance = new OllamaService();
        }
        return instance;
    }
    
    /**
     * 檢查Ollama服務是否可用
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Ollama服務不可用: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 檢查指定模型是否已安裝
     */
    public boolean isModelAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonArray models = jsonResponse.getAsJsonArray("models");
                
                if (models != null) {
                    for (JsonElement modelElement : models) {
                        JsonObject modelObj = modelElement.getAsJsonObject();
                        String modelName = modelObj.get("name").getAsString();
                        if (modelName.contains(model)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("檢查模型時發生錯誤: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 發送消息給AI並獲取回應
     */
    public Task<String> sendMessageAsync(String message) {
        return new Task<String>() {
            @Override
            protected String call() throws Exception {
                return sendMessage(message);
            }
        };
    }
    
    /**
     * 同步發送消息
     */
    private String sendMessage(String message) throws IOException, InterruptedException {
        // 構建請求體
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", buildPromptWithHistory(message));
        requestBody.put("stream", false);
        
        // 添加系統提示
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        options.put("top_p", 0.9);
        requestBody.put("options", options);
        
        String jsonBody = gson.toJson(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            String aiResponse = jsonResponse.get("response").getAsString();
            
            // 保存對話歷史
            addToHistory("user", message);
            addToHistory("assistant", aiResponse);
            
            return aiResponse;
        } else {
            throw new IOException("API請求失敗，狀態碼: " + response.statusCode());
        }
    }
    
    /**
     * 構建包含對話歷史的提示
     */
    private String buildPromptWithHistory(String newMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【重要】只能使用中文回答，語氣友善自然。絕對不能使用英文或其他語言。\n");
        prompt.append("你是一位樂於助人的AI助手，既能回答具體問題，也能提供情感支持。\n");
        prompt.append("當用戶問具體問題時，請直接準確地回答；當用戶分享感受或困擾時，請給予溫暖的支持。\n");
        prompt.append("回答要簡潔明瞭，不要過度分析或引申。\n\n");
        
        // 添加這一週的anynotes背景信息
        if (weeklyAnynotes != null && !weeklyAnynotes.isEmpty()) {
            prompt.append("=== 用戶本週的日記記錄（僅作為背景參考，不要直接重複或引用） ===\n");
            for (String note : weeklyAnynotes) {
                prompt.append(note).append("\n");
            }
            prompt.append("=== 背景信息結束 ===\n\n");
            prompt.append("基於以上背景信息了解用戶近況，但請專注回應當前的對話，不要直接提及日記內容。\n\n");
        }
        
        // 添加當前對話歷史（最近3輪對話即可，避免過多舊信息）
        int historyLimit = Math.min(conversationHistory.size(), 6); // 3輪對話 = 6條消息
        if (historyLimit > 0) {
            prompt.append("=== 最近對話 ===\n");
            for (int i = Math.max(0, conversationHistory.size() - historyLimit); i < conversationHistory.size(); i++) {
                Map<String, String> entry = conversationHistory.get(i);
                if ("user".equals(entry.get("role"))) {
                    prompt.append("用戶: ").append(entry.get("content")).append("\n");
                } else {
                    prompt.append("助手: ").append(entry.get("content")).append("\n");
                }
            }
            prompt.append("=== 對話記錄結束 ===\n\n");
        }
        
        prompt.append("用戶: ").append(newMessage).append("\n");
        prompt.append("助手: ");
        
        // 調試輸出（可以在生產環境中移除）
        System.out.println("=== AI Prompt Debug ===");
        System.out.println("週anynotes數量: " + (weeklyAnynotes != null ? weeklyAnynotes.size() : 0));
        System.out.println("對話歷史數量: " + conversationHistory.size());
        System.out.println("========================");
        
        return prompt.toString();
    }
    
    /**
     * 添加對話到歷史記錄
     */
    private void addToHistory(String role, String content) {
        Map<String, String> entry = new HashMap<>();
        entry.put("role", role);
        entry.put("content", content);
        conversationHistory.add(entry);
        
        // 限制歷史記錄長度，避免消耗過多記憶體
        if (conversationHistory.size() > 20) {
            conversationHistory.remove(0);
        }
    }
    
    /**
     * 清除對話歷史
     */
    public void clearHistory() {
        conversationHistory.clear();
        System.out.println("已清除對話歷史，當前對話數量: " + conversationHistory.size());
    }
    
    /**
     * 獲取模型狀態信息
     */
    public String getModelStatus() {
        if (!isAvailable()) {
            return "Ollama服務未運行";
        } else if (!isModelAvailable()) {
            return "llama3.2模型未安裝";
        } else {
            return "llama3.2 - 已就緒";
        }
    }
    
    /**
     * 獲取對話歷史數量
     */
    public int getHistorySize() {
        return conversationHistory.size();
    }
    
    /**
     * 設置這一週的anynotes內容
     * @param weeklyAnynotes 這一週的anynotes列表
     */
    public void setWeeklyAnynotes(List<String> weeklyAnynotes) {
        this.weeklyAnynotes = new ArrayList<>(weeklyAnynotes);
    }
    
    /**
     * 清除週anynotes數據
     */
    public void clearWeeklyAnynotes() {
        if (this.weeklyAnynotes != null) {
            this.weeklyAnynotes.clear();
        }
    }
} 