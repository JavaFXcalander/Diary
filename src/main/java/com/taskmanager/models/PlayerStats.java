package com.taskmanager.models;

public class PlayerStats {
    private int level;
    private int currentXP;
    private int xpToNextLevel;
    private int gold;
    
    public PlayerStats() {
        this.level = 1;
        this.currentXP = 0;
        this.xpToNextLevel = 100;
        this.gold = 0;
    }
    
    public void addXP(int xp) {
        currentXP += xp;
        while (currentXP >= xpToNextLevel) {
            levelUp();
        }
    }
    
    private void levelUp() {
        level++;
        currentXP -= xpToNextLevel;
        xpToNextLevel = (int) (xpToNextLevel * 1.5); // Increase XP needed for next level
    }
    
    public void addGold(int amount) {
        gold += amount;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getCurrentXP() {
        return currentXP;
    }
    
    public int getXpToNextLevel() {
        return xpToNextLevel;
    }
    
    public int getGold() {
        return gold;
    }
    
    public double getXpProgress() {
        return (double) currentXP / xpToNextLevel;
    }
} 