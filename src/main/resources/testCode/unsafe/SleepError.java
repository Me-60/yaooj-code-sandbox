public class Main {

    public static void main(String[] args) throws InterruptedException {

        // 设置程序休眠时间
        long ONE_HOUR = 60 * 60 * 100000000000000000000000L;

        // 程序休眠一小时
        Thread.sleep(ONE_HOUR);

        System.out.println("一小时睡完了");
    }
}
