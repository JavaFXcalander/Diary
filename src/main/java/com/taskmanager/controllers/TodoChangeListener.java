package com.taskmanager.controllers;

/**
 * 用于通知Todo数据变更的接口
 */
public interface TodoChangeListener {
    /**
     * 当Todo数据发生变化时调用
     */
    void onTodoChanged();
}
