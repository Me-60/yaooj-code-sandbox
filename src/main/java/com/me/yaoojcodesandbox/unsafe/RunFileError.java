package com.me.yaoojcodesandbox.unsafe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 运行其他程序（例如木马程序，恶意用户写入的）
 * 通过Process类直接运行恶意程序或电脑其他程序
 */
public class RunFileError {

    public static void main(String[] args) throws IOException, InterruptedException {

        // 获取项目所在目录
        String userDir = System.getProperty("user.dir");

        // 设置写入文件路径
        String filePath = userDir + File.separator + "src/main/resources/危险程序代码.bat";

        // 运行写入文件并返回一个Process类子进程
        Process process = Runtime.getRuntime().exec(filePath);

        // 一直等待Process类进程运行结束
        process.waitFor();

        // 正常输出
        // 分批获取进程的输出，可能一次不会读完，对执行多次
        // runProcess.getInputStream()获取编译输入流（不需要纠结从输入流或输出流取结果，不同程序会把结果写入不同地方）
        // new InputStreamReader()创建了一个输入流读取器，读取编译输入流
        // new BufferedReader()以块、成批的方式读取输出
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // 编译结果信息（按行读取bufferedReader.readLine()）
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            System.out.println(compileOutputLine);
        }

        System.out.println("危险程序执行成功");
    }
}
