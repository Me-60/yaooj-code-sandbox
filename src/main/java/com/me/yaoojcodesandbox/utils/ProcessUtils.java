package com.me.yaoojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.me.yaoojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 进程工具类
 * 核心是Process进程类
 * 注意getInputStream与getOutputStream方法的获取对象是谁
 */
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     * @param runProcess
     * @param optionName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess,String optionName) {

        // 初始化执行信息
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 通过Spring框架的StopWatch类来统计执行程序消耗时间
            StopWatch stopWatch = new StopWatch();

            // 开始计时
            stopWatch.start();

            // runProcess.waitFor()此调用会等待终端执行完命令，结束后会有个返回值
            // 就像启动SpringBoot项目，要是有错误运行结束并返回一个130，一个道理，这里理解为有个返回值
            int exitValue = runProcess.waitFor();

            // 设置程序执行结果代码值
            executeMessage.setExitValue(exitValue);

            // 根据exitValue判断代码编译情况
            if(exitValue == 0) {
                // 正常退出（optionName代表操作名称，例如编译成功和执行成功）
                // 可以根据具体调用场景进行定义optionName
                System.out.println(optionName + "成功");

                // 正常输出
                // 分批获取进程的输出，可能一次不会读完，对执行多次
                // runProcess.getInputStream()获取编译输入流（不需要纠结从输入流或输出流取结果，不同程序会把结果写入不同地方）
                // new InputStreamReader()创建了一个输入流读取器，读取编译输入流
                // new BufferedReader()以块、成批的方式读取输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));

                // 通过StringBuilder类将读取的编译结果信息进行拼接（按行需拼）
                StringBuilder compileOutputStringBuilder = new StringBuilder();

                // 编译结果信息（按行读取bufferedReader.readLine()）
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }

                // 编译信息拼接完毕后，设置程序正常输出信息
                executeMessage.setMessage(compileOutputStringBuilder.toString().replaceAll("[\\n\\r]",""));
            } else {
                // 异常退出
                System.out.println(optionName + "失败，错误码为" + exitValue);

                // 编译失败的话既有正常输出，又有异常输出
                // 正常输出
                // 分批获取进程的输出，可能一次不会读完，对执行多次
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));

                // 通过StringBuilder类将读取的编译结果信息进行拼接（按行需拼）
                StringBuilder compileOutputStringBuilder = new StringBuilder();

                // 编译结果信息（按行读取bufferedReader.readLine()）
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }

                // 编译信息拼接完毕后，设置程序正常输出信息
                executeMessage.setMessage(compileOutputStringBuilder.toString().replaceAll("[\\n\\r]",""));

                // 异常输出
                // 分批获取进程的输出，可能一次不会读完，对执行多次
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));

                // 通过StringBuilder类将读取的编译结果信息进行拼接（按行需拼）
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();

                // 编译结果信息（按行读取bufferedReader.readLine()）
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine);
                }

                // 编译信息拼接完毕后，设置程序异常输出信息
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString().replaceAll("[\\n\\r]",""));
            }

            // 计时结束
            stopWatch.stop();

            // 设置程序执行消耗时间
            executeMessage.setExecuteTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 执行交互进程并获取信息
     * 但并没有真正意义上实现我们在控制台输入参数而得到结果的这种交互，只是不通过args数组接受参数
     * 通过Process进程类的输出完成参数的输入
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess,String args) {

        // 初始化执行信息
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 获取子进程的正常输入的输出流
            OutputStream outputStream = runProcess.getOutputStream();

            // 获取子进程的正常输入的输出流后，通过OutputStreamWriter就可以给子进程的输入传入内容了，不过由于OutputStreamWriter
            // 调用一次write方法就会调用一次指定的编码转换器，防止频繁调用浪费资源，采用BufferedWriter来完成内容输入
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            // 加工处理输入内容
            String[] strings = args.split(" ");
            // 将输入内容进行拼接
            String join = StrUtil.join("\n",strings) + "\n";

            // 写入输入内容
            bufferedWriter.write(join);
            // 相当于按了回车，执行输入的发送（刷新流）
            bufferedWriter.flush();

            // 分批获取进程的输出，可能一次不会读完，对执行多次
            // runProcess.getInputStream()获取编译输入流（不需要纠结从输入流或输出流取结果，不同程序会把结果写入不同地方）
            // new InputStreamReader()创建了一个输入流读取器，读取编译输入流
            // new BufferedReader()以块、成批的方式读取输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));

            // 通过StringBuilder类将读取的编译结果信息进行拼接（按行需拼）
            StringBuilder compileOutputStringBuilder = new StringBuilder();

            // 编译结果信息（按行读取bufferedReader.readLine()）
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }

            executeMessage.setMessage(compileOutputStringBuilder.toString().replaceAll("[\\n\\r]",""));

            // 释放资源，防止卡死
            bufferedWriter.close();
            outputStream.close();
            bufferedReader.close();
            runProcess.destroy();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return executeMessage;
    }
}
