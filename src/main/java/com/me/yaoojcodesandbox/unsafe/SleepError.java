package com.me.yaoojcodesandbox.unsafe;

/**
 * 无限休眠（阻塞程序执行）
 * 占用时间资源，导致程序卡死，不释放资源
 * 要是一直有用户提交此类休眠的代码，一直占用服务器资源，久而久之服务器会垮
 */
public class SleepError {

    public static void main(String[] args) throws InterruptedException {

        // 设置程序休眠时间
        long ONE_HOUR = 60 * 60 * 1000L;

        // 程序休眠一小时
        Thread.sleep(ONE_HOUR);

        System.out.println("一小时睡完了");
    }
}
