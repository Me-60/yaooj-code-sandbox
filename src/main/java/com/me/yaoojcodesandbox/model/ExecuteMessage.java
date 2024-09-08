package com.me.yaoojcodesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 */
@Data
public class ExecuteMessage {

    /**
     * 程序执行结果代码值
     */
    private Integer exitValue;

    /**
     * 程序正常输出信息（有的执行信息成功会有个正常输出信息，失败也有个正常输出信息）
     */
    private String message;

    /**
     * 程序异常输出信息
     */
    private String errorMessage;

    /**
     * 程序执行消耗时间
     */
    private Long executeTime;

    /**
     * 程序执行消耗内存
     */
    private Long executeMemory;
}
