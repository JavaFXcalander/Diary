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
import java.util.List;

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
            
            userDao.create(entry);
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
            throw new RuntimeException("Failed to get user entry for email: " + email, e);
        }
    }

    // 專案相關操作
    public void saveProject(ProjectModel project) {
        try {
            List<ProjectModel> existingProjects = projectDao.queryBuilder()
            .where()
            .eq("year", project.getYear())  // 條件1：年份匹配
            .and()                       // 邏輯"且"
            .eq("month", project.getMonth()) // 條件2：月份匹配
            .query();                    // 執行查詢

            ProjectModel existingProject = existingProjects.get(0);
            if (existingProject != null) {
                existingProject.setProject1(project.getProject1());
                // existingProject.setProject2(project.getProject2());
                // existingProject.setProject3(project.getProject3());
                // existingProject.setProject4(project.getProject4());
                // existingProject.setAbout1(project.getAbout1());
                // existingProject.setAbout2(project.getAbout2());
                // existingProject.setAbout3(project.getAbout3());
                // existingProject.setAbout4(project.getAbout4());
                // existingProject.setHabit1(project.getHabit1());
                // existingProject.setHabit2(project.getHabit2());
                // existingProject.setHabit3(project.getHabit3());
                // existingProject.setHabit4(project.getHabit4());
                projectDao.update(existingProject);
            } else {
                // 如果不存在，創建新的專案
                projectDao.create(project);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save project", e);
        }
    }

    public ProjectModel getProjectEntry(int year, int month) {
    try {
        List<ProjectModel> projects = projectDao.queryBuilder()
            .where()
            .eq("year", year)
            .and()
            .eq("month", month)
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
            throw new RuntimeException("Failed to close database connection", e);
        }
    }
}
