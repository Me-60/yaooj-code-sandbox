package com.me.yaoojcodesandbox.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.me.yaoojcodesandbox.CodeSandbox;
import com.me.yaoojcodesandbox.model.ExecuteCodeRequest;
import com.me.yaoojcodesandbox.model.ExecuteCodeResponse;
import com.me.yaoojcodesandbox.model.ExecuteMessage;
import com.me.yaoojcodesandbox.model.JudgeInfo;
import com.me.yaoojcodesandbox.security.MySecurityManager;
import com.me.yaoojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandbox implements CodeSandbox {

    /**
     * GLOBAL_CODE_DIR_NAME为临时存储用户代码的目录
     * GLOBAL_JAVA_CLASS_NAME为存放用户代码的文件名
     * TIME_OUT为子进程执行用户代码最长时间
     * DANGER_CODE_KEYWORD_LIST为危险代码关键字列表，也可称为黑白名单
     * WORD_TREE为字典树
     * SECURITY_MANAGER_PATH为安全管理器路径
     * SECURITY_MANAGER_CLASS_PATH为安全管理器类名
     */
    private static final String GLOBAL_CODE_DIR_NAME = "tempRunCodeStorage";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;

    private static final List<String> DANGER_CODE_KEYWORD_LIST = Arrays.asList("Files","exec","Paths");

    private static final WordTree WORD_TREE;

    private static final String SECURITY_MANAGER_PATH = "D:\\star_project\\yaooj-code-sandbox\\src\\main\\resources\\security";

    private static final String SECURITY_MANAGER_CLASS_PATH = "MySecurityManager";

    static {
        // 初始化字典树
        WORD_TREE = new WordTree();

        // 设置字典树查询关键字
        WORD_TREE.addWords(DANGER_CODE_KEYWORD_LIST);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // 权限管控
        // System.setSecurityManager(new MySecurityManager());

        // 获取运行代码和输入用例
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();

        // 检验用户代码是否存在危险代码关键字
        // 这里使用的是hutool的字典树，使用算法为DFA
        // 通过字典树匹配用户代码是否存在危险代码关键字
        // 这种检验方式有缺陷，危险代码关键字涉及广（还有不同编程语言都有自己的危险代码关键字）
//        FoundWord foundWord = WORD_TREE.matchWord(code);

        // 通过字典树返回的结果确认用户代码存在危险代码关键词
//        if (foundWord != null) {
//            System.out.println("该代码包含危险代码关键词为：" + foundWord.getFoundWord());
//            return null;
//        }

        /*
         *  1.将用户代码保存为.java文件
         */
        // 获取System.getProperty("user.dir"): 这是Java标准库中的一个方法，用于获取系统属性。
        // "user.dir"是一个特定的系统属性键，它返回JVM的当前工作目录。这个目录通常是启动JVM时所在的目录，
        // 也就是你从哪个目录运行了Java程序，或者为项目目录。
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        // 判断存储临时运行代码目录是否存在，没有则新建
        // 通过hutool工具来完成判断与创建
        if(!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 将用户代码隔离存放
        // 根据UUID随机生成的数字作为存放用户代码的父目录然后拼接到存储临时运行代码所在路径上，达到一个用户代码所在目录的路径
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();

        // 这里我们将用户代码的文件名称固定为Main.java，同样用户在完成题目时public修饰的类名为Main，然后与代码父目录拼接
        // 为用户代码的绝对路径
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;

        // 将获取的用户代码根据用户代码路径进行创建，并统一字符编码
        File userCodeFile = FileUtil.writeString(code,userCodePath, StandardCharsets.UTF_8);

        /*
         *  2.将用户代码.java文件编译为.class文件
         */
        // 通过String类的format方法来完善编译命令，每个用户的代码路径不同所以需要动态获取.java文件的绝对路径来拼接
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());

        try {
            // 通过Process类在终端执行定义好的命令
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);

            // 调用进程工具类，执行命令返回执行信息
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception exception) {
            // 通过错误处理方法直接返回代码沙箱的错误或异常
            return getErrorResponse(exception);
        }

        /*
         * 3.执行代码，得到输出结果
         */
        // 初始化所有用例执行信息
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        // 执行代码次数依据输入用例次数
        for(String inputArgs : inputList) {
            // 通过String类的format方法来完善编译命令，动态获取用户代码路径以及输入用例
            // -Xmx256m代表指定JVM最大堆内存为256M，这样指定运行程序时最大内存，防止恶意程序发生内存溢出现象
            // -Xmx参数、JVM的堆内存限制，不等同于系统实际占用的最大资源，可能会超出
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s",SECURITY_MANAGER_PATH,userCodeParentPath,SECURITY_MANAGER_CLASS_PATH,inputArgs);

            try {
                // 通过Process类在终端执行定义好的命令
                Process runProcess = Runtime.getRuntime().exec(runCmd);

                // 创建类似一个守护线程并启动该线程，监管子进程运行时间，防止恶意阻塞程序
                new Thread(() -> {
                    try {
                        // 线程先休眠5s，在这五秒期间子进程没运行完可能会有问题
                        Thread.sleep(TIME_OUT);

                        // 判断执行命令子进程是否执行完毕
                        if (runProcess.isAlive()) {

                            // 子进程存在，说明程序超过最大运行时间，以防恶意阻塞程序并销毁子进程
                            runProcess.destroy();

                            System.out.println("程序超过最大运行时间，终止操作");
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                // 调用进程工具类，执行命令返回执行信息
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess,"运行");
                System.out.println(executeMessage);

                // 保存用例执行信息
                executeMessageList.add(executeMessage);
            } catch (Exception exception) {
                // 通过错误处理方法直接返回代码沙箱的错误或异常
                return getErrorResponse(exception);
            }
        }

        /*
         * 4.收集整理输出结果
         */
        // 初始化代码沙箱执行响应
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        // 初始化执行结果
        List<String> executeOutputList = new ArrayList<>();

        // 初始化用例执行消耗时间最大值，用于判断程序执行是否超时
        long maxExecuteTime = 0;

        for(ExecuteMessage executeMessage : executeMessageList) {

            // 获取用例执行异常信息
            String errorMessage = executeMessage.getErrorMessage();

            // 判断用例执行异常信息是否发生
            if(StrUtil.isNotBlank(errorMessage)) {

                // 设置响应异常信息
                executeCodeResponse.setMessage(errorMessage);

                // 设置响应状态为失败（代码执行中发生的错误，代码的原因）
                executeCodeResponse.setStatus(3);
                break;
            }

            // 保存用例结果正常信息
            executeOutputList.add(executeMessage.getMessage());

            // 获取程序执行消耗时间
            Long time = executeMessage.getExecuteTime();

            // 获取程序执行消耗时间最大值
            if(time != null) {
                maxExecuteTime = Math.max(time,maxExecuteTime);
            }
        }

        // 比较整理后的结果与程序执行信息数量是否相同，确保程序执行是否正常完成，从而设置程序执行状态
        if(executeOutputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }

        // 设置响应程序执行结果
        executeCodeResponse.setOutputList(executeOutputList);

        // 初始化程序执行各类信息
        JudgeInfo judgeInfo = new JudgeInfo();

        // 设置程序执行消耗时间
        judgeInfo.setTime(maxExecuteTime);

        // 此处并未设置程序执行消耗内存信息，要借助第三方库来获取内存占用，很麻烦，此处不做实现 judgeInfo.setMemory();

        // 设置响应程序执行信息
        executeCodeResponse.setJudgeInfo(judgeInfo);

        /*
         * 5.文件清理
         */
        // 防止服务器空间不足，删除代码目录
        // 判断用户代码父目录是否存在，为了防止父目录因一些原因而没有创建，在去执行删除发生报错，记住这个开发思路
        if (userCodeFile.getParentFile() != null) {
            // 这里删除要注意是不仅删除用户代码而且也要删除其一起创建的父目录
            boolean clear = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (clear ? "成功" : "失败"));
        }

        return executeCodeResponse;
    }

    /**
     * 获取代码沙箱的错误或异常响应
     * 从而便于区分代码本身错误和代码沙箱执行错误或异常
     * @param throwable
     * @return
     */
    // Throwable类是Java语言中所有错误和异常的Throwable类
    private ExecuteCodeResponse getErrorResponse(Throwable throwable) {

        // 初始化代码沙箱执行响应，代码沙箱执行时发生错误或异常，只要有这样的异常或错误，直接返回错误或异常响应
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        // 设置错误或异常响应值
        // 设置响应程序执行结果为空，因为是代码沙箱执行的问题，而不是代码的问题，所以代码运行结果为空
        executeCodeResponse.setOutputList(new ArrayList<>());

        // 设置响应程序执行消息为代码沙箱执行所发生的异常或错误
        executeCodeResponse.setMessage(throwable.getMessage());

        // 设置程序执行结果状态为2，表示代码沙箱发生的错误或异常而失败
        executeCodeResponse.setStatus(2);

        // 设置程序执行信息为空，因为代码沙箱产生的错误或异常导致代码正常运行，并未有程序执行信息
        executeCodeResponse.setJudgeInfo(new JudgeInfo());

        // 设置执行代码请求消息
        executeCodeResponse.setMessage("代码沙箱模块服务异常，执行代码终止");

        return executeCodeResponse;
    }
}
