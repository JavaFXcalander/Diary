package com.taskmanager.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import java.io.IOException;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.taskmanager.models.DiaryModel;
import com.taskmanager.models.ProjectModel;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class DiaryDatabase {
    // ORMLite
    private static final String DATABASE_URL = "jdbc:h2:./diary_db;AUTO_SERVER=TRUE";
    private static DiaryDatabase instance;
    private ConnectionSource connectionSource;
    private Dao<DiaryModel, Integer> diaryDao;
    private Dao<ProjectModel, Integer> projectDao;

    private DiaryDatabase() {
        try {
            connectionSource = new JdbcConnectionSource(DATABASE_URL);
            
            // 創建資料表
            TableUtils.createTableIfNotExists(connectionSource, DiaryModel.class);
            TableUtils.createTableIfNotExists(connectionSource, ProjectModel.class);
            
            // 初始化 DAO
            diaryDao = DaoManager.createDao(connectionSource, DiaryModel.class);
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
            // 檢查當天是否已有日記
            List<DiaryModel> existingEntries = diaryDao.queryForEq("date", entry.getDate());
            if (!existingEntries.isEmpty()) {
                // 如果存在，更新現有的日記
                DiaryModel existingEntry = existingEntries.get(0);
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

    public DiaryModel getDiaryEntry(LocalDate date) {
        try {
            List<DiaryModel> entries = diaryDao.queryForEq("date", date);
            return entries.isEmpty() ? null : entries.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get diary entry", e);
        }
    }

    


    
    // 專案相關操作
    public void saveProject(ProjectModel project) {
        try {
            projectDao.createOrUpdate(project);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save project", e);
        }
    }

    public List<ProjectModel> getMonthlyProjects(LocalDate date) {
        try {
            LocalDate startOfMonth = date.withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
            
            return projectDao.queryBuilder()
                    .where()
                    .ge("startDate", startOfMonth)
                    .and()
                    .le("endDate", endOfMonth)
                    .query();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get monthly projects", e);
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
