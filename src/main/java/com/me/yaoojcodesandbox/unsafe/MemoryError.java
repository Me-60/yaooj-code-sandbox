package com.me.yaoojcodesandbox.unsafe;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限占用空间（浪费系统内存）
 * 通过无限创建数组存入List中的方式来无限占用空间
 * 无限创建常量或对象，无法实现无限占用空间的目的，因为JVM会自动回收防止溢出
 * 虽然该程序按理论上讲，会无限占用系统内存最终死机，实际上由于JVM保护机制
 * 当内存达到一定程度会自动报错，错误为java.lang.OutOfMemoryError: Java heap space
 */
public class MemoryError {

    public static void main(String[] args) {

        // 规定一个存放整型数组List
        List<int[]> ints = new ArrayList<>();

        // 无限创建数组存入List中
        while (true) {
            ints.add(new int[1000]);
        }
    }
}
