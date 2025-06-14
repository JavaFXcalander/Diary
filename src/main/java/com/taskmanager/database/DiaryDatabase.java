package com.taskmanager.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.taskmanager.models.DiaryModel;
import com.taskmanager.models.UserModel;
import com.taskmanager.models.ProjectModel;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class DiaryDatabase {
    // ORMLite
    private static final String DATABASE_URL = "jdbc:h2:./diary_db;AUTO_SERVER=TRUE";
    private static DiaryDatabase instance;
    private ConnectionSource connectionSource;
    private Dao<DiaryModel, Integer> diaryDao;
    private Dao<UserModel, Integer> userDao;
    private Dao<ProjectModel, Integer> projectDao;

    private DiaryDatabase() {
        try {
            connectionSource = new JdbcConnectionSource(DATABASE_URL);
            
            // 創建資料表
            TableUtils.createTableIfNotExists(connectionSource, DiaryModel.class);
            TableUtils.createTableIfNotExists(connectionSource, UserModel.class);
            TableUtils.createTableIfNotExists(connectionSource, ProjectModel.class);
            
            // 執行資料庫遷移
            
            // 初始化 DAO
            diaryDao = DaoManager.createDao(connectionSource, DiaryModel.class);
            userDao = DaoManager.createDao(connectionSource, UserModel.class);
            projectDao = DaoManager.createDao(connectionSource, ProjectModel.class);

       } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static DiaryDatabase getInstance() {
        if (instance == null) {
            instance = new DiaryDatabase();
        }
        return instance;
    }

    // 日記相關操作
    public void saveDiaryEntry(DiaryModel entry) {
        try {
            // 檢查當天是否已有日記 - can't use queryForEq with serialized fields
            DiaryModel existingEntry = getDiaryEntry(entry.getDate(), entry.getUser().getEmail());
            
            if (existingEntry != null) {
                // 如果存在，更新現有的日記
                existingEntry.setDDay(entry.getDDay());
                existingEntry.setPriority(entry.getPriority());
                existingEntry.setRoutine(entry.getRoutine());
                existingEntry.setBudget(entry.getBudget());
                existingEntry.setPhotoCollage(entry.getPhotoCollage());
                existingEntry.setBreakfast(entry.getBreakfast());
                existingEntry.setLunch(entry.getLunch());
                existingEntry.setDinner(entry.getDinner());
                existingEntry.setSnack(entry.getSnack());
                existingEntry.setAnynotes(entry.getAnynotes());
                existingEntry.setTodo(entry.getTodo());
                existingEntry.setCalendarEmpty(entry.isCalendarEmpty());
               
                diaryDao.update(existingEntry);
            } else {
                // 如果不存在，創建新的日記
                diaryDao.create(entry);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save diary entry", e);
        }
    }
    //拿到資料

    
    public DiaryModel getDiaryEntry(LocalDate date, String userEmail) {
        try {
            // Since we can't query directly on serialized fields, we need to retrieve all entries
            // and filter them in memory
            List<DiaryModel> allEntries = diaryDao.queryForAll();
            
            // Filter entries by date and user manually
            for (DiaryModel entry : allEntries) {
                LocalDate entryDate = entry.getDate();
                if (entryDate != null && entryDate.equals(date)) {
                    
                    // Otherwise, check if the entry belongs to the specified user
                    if (entry.getUser() != null && userEmail.equals(entry.getUser().getEmail())) {
                        return entry;
                    }
                }
            }
            
            // No matching entry found
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get diary entry", e);
        }
    }

    // 日記相關操作
    public void saveUserEntry(UserModel entry) {
        try {
            // 檢查用戶是否已存在
            UserModel existingUser = getUserEntry(entry.getEmail());
            
            if (existingUser != null) {
                // 如果用戶已存在，更新現有用戶的信息
                existingUser.setHashedPassword(entry.getHashedPassword());
                existingUser.setMoodleToken(entry.getMoodleToken());
                existingUser.setMoodleUsername(entry.getMoodleUsername());
                existingUser.setMoodleLastLoginTime(entry.getMoodleLastLoginTime());
                
                userDao.update(existingUser);
                System.out.println("已更新用戶信息: " + entry.getEmail());
            } else {
                // 如果用戶不存在，創建新用戶
            userDao.create(entry);
                System.out.println("已創建新用戶: " + entry.getEmail());
            }
        } catch (SQLException e) {
            // Wrap SQLException in a RuntimeException to simplify error handling upstream,
            // consistent with other database operations in this class.
            throw new RuntimeException("Failed to save user entry: " + e.getMessage(), e);
        }
    }

    public UserModel getUserEntry(String email) {
        try {
            // Query directly for the email, which is more efficient.
            // Assumes 'email' field is indexed or query is on a small table.
            List<UserModel> users = userDao.queryForEq("email", email);
            if (users != null && !users.isEmpty()) {
                return users.get(0); // Email should be unique
            }
            return null;
        } catch (SQLException e) {
            // 如果是因為字段不存在的錯誤，說明需要資料庫遷移
            if (e.getMessage().contains("Column") && e.getMessage().contains("not found")) {
                System.err.println("資料庫結構問題，需要重新啟動應用程序進行遷移: " + e.getMessage());
                return null;
            }
            throw new RuntimeException("Failed to get user entry for email: " + email, e);
        }
    }

    // 專案相關操作
    public void saveProject(ProjectModel project , String userEmail) {
        try {
            UserModel user = getUserEntry(userEmail);
            if (user == null) throw new RuntimeException("User not found");

            project.setUser(user); // 确保关联用户

            List<ProjectModel> existingProjects = projectDao.queryBuilder()
            .where()
            .eq("year", project.getYear())
            .and()
            .eq("month", project.getMonth())
            .and()
            .eq("user_id", user.getId())
            .query();

           if (!existingProjects.isEmpty()) {
                ProjectModel existingProject = existingProjects.get(0);
                existingProject.setProject1(project.getProject1());
                existingProject.setProject2(project.getProject2());
                existingProject.setProject3(project.getProject3());
                existingProject.setProject4(project.getProject4());
                existingProject.setAbout1(project.getAbout1());
                existingProject.setAbout2(project.getAbout2());
                existingProject.setAbout3(project.getAbout3());
                existingProject.setAbout4(project.getAbout4());
                existingProject.setHabit1(project.getHabit1());
                existingProject.setHabit2(project.getHabit2());
                existingProject.setHabit3(project.getHabit3());
                existingProject.setHabit4(project.getHabit4());
                existingProject.setDailyChecks(project.getDailyChecks());
                projectDao.update(existingProject);
            } else {
                projectDao.create(project);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save project", e);
        }
    }

    public ProjectModel getProjectEntry(int year, int month, String userEmail) {
        try {
            UserModel user = getUserEntry(userEmail);
            if (user == null) return null;
            List<ProjectModel> projects = projectDao.queryBuilder()
                .where()
                .eq("year", year)
                .and()
                .eq("month", month)
                .and()
                .eq("user_id", user.getId())
                .query();

            return projects.isEmpty() ? null : projects.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get project entry", e);
        }
    }
    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定用户这一周的anynotes内容
     * @param userEmail 用户邮箱
     * @param currentDate 当前日期，用于确定本周范围
     * @return 这一周的anynotes内容列表，按日期排序
     */
    public List<String> getWeeklyAnynotes(String userEmail, LocalDate currentDate) {
        try {
            List<String> weeklyAnynotes = new ArrayList<>();
            
            // 计算本周的开始日期（周一）和结束日期（周日）
            LocalDate startOfWeek = currentDate.with(java.time.DayOfWeek.MONDAY);
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            
            // 获取所有日记条目
            List<DiaryModel> allEntries = diaryDao.queryForAll();
            
            // 过滤出指定用户在本周的日记条目
            for (DiaryModel entry : allEntries) {
                if (entry.getDate() != null && entry.getUser() != null && 
                    userEmail.equals(entry.getUser().getEmail())) {
                    
                    LocalDate entryDate = entry.getDate();
                    if (!entryDate.isBefore(startOfWeek) && !entryDate.isAfter(endOfWeek)) {
                        String anynotes = entry.getAnynotes();
                        if (anynotes != null && !anynotes.trim().isEmpty()) {
                            // 格式化：日期 + anynotes内容
                            String formattedEntry = String.format("%s: %s", 
                                entryDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")), 
                                anynotes.trim());
                            weeklyAnynotes.add(formattedEntry);
                        }
                    }
                }
            }
            
            // 按日期排序
            weeklyAnynotes.sort((a, b) -> {
                String dateA = a.split(":")[0];
                String dateB = b.split(":")[0];
                return dateA.compareTo(dateB);
            });
            
            return weeklyAnynotes;
            
        } catch (SQLException e) {
            System.err.println("获取周anynotes时发生错误: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
}
