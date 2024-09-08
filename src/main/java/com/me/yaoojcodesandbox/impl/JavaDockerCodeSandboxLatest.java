package com.me.yaoojcodesandbox.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.me.yaoojcodesandbox.model.ExecuteCodeRequest;
import com.me.yaoojcodesandbox.model.ExecuteCodeResponse;
import com.me.yaoojcodesandbox.model.ExecuteMessage;
import com.me.yaoojcodesandbox.model.JudgeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component("javaDockerCodeSandbox")
@Slf4j
public class JavaDockerCodeSandboxLatest extends JavaCodeSandboxTemplate {

    /**
     * DOCKER_CLIENT 为默认 docker client
     * IMAGE 为执行代码环境所需镜像
     * CONTAINER_VOLUME_PATH 为容器映射本地文件路径
     * CONTAINER_MEMORY 为容器内存
     * CONTAINER_CPU_COUNT 为容器CPU数量
     * PULL_IMAGE_TIMEOUT 为拉取镜像最大时间限制
     * EXEC_TIMEOUT 为容器执行命令最大时间限制
     */

    private static final DockerClient DOCKER_CLIENT;

    private static final String IMAGE = "openjdk:8-alpine";

    private static final long PULL_IMAGE_TIMEOUT = 5L;

    private static final String CONTAINER_VOLUME_PATH = "/tempRunCodeMapper";

    private static final long CONTAINER_MEMORY = 32000000L;

    private static final long CONTAINER_CPU_COUNT = 1L;

    private static final long EXEC_TIMEOUT = 5L;

    // 初始化 docker client 和执行代码所需镜像环境
    static {
        // 获取默认的 docker client builder 的实例
        DockerClientBuilder dockerClientBuilder = DockerClientBuilder.getInstance();

        // 通过 docker client builder 初始化一个默认的 docker client
        DOCKER_CLIENT = dockerClientBuilder.build();

        // 初始化 docker images 命令
        // 查看所有镜像操作
        ListImagesCmd listImagesCmd = DOCKER_CLIENT.listImagesCmd();

        // 执行并返回 docker 中镜像信息列表
        List<Image> images = listImagesCmd.exec();

        // 关闭资源
        listImagesCmd.close();

        // 初始化目标镜像名称及版本确认列表
        List<String> imageTags = new ArrayList<>();

        // 判断环境中是否有镜像存在，有则开始遍历提取镜像名称及版本
        // 注意区分镜像列表还是镜像名称及版本列表
        if (!images.isEmpty()) {

            // 遍历镜像列表
            for (Image image : images) {
                // 获取镜像名称及版本信息
                String imageTag = Arrays.stream(image.getRepoTags()).findFirst().orElse("none");
                System.out.println("====镜像名称及版本信息为：" + imageTag + "====");

                // 遍历当前的镜像名称及版本信息是否与目标相等
                // 相等的话先填入镜像名称及版本确认列表
                if (imageTag.equals(IMAGE)) {
                    imageTags.add(imageTag);
                }
            }
        }

        // 先判断镜像是否为空，后判断镜像名称及版本确认列表是否为空
        // 注意区分镜像列表还是镜像名称及版本列表
        // 这段有点绕
        // 只要一个满足就拉取镜像
        // 与上方的判断对应的
        // 上方不满足，则说明环境未存在镜像，则直接拉取镜像
        // 上方满足，前者条件不为空，后者要是也不为空，说明环境优美目标镜像，不需要拉取，反之，后者为空，说明环境有镜像未存在目标镜像，拉取镜像
        if (images.isEmpty() || imageTags.isEmpty()) {
            System.out.println("====目标镜像不存在，开始配置镜像====");
            PullImageCmd pullImageCmd = DOCKER_CLIENT.pullImageCmd(IMAGE);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("====拉取镜像异常为：" + throwable + "====");
                    // 一定要调用父类对应的方法，不然程序无法正常结束
                    super.onError(throwable);
                }

                @Override
                public void onComplete() {
                    System.out.println("====拉取镜像完毕====");
                    // 一定要调用父类对应的方法，不然程序无法正常结束
                    super.onComplete();
                }

                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("====拉取镜像过程情况说明：" + item.getStatus() + "====");
                    // 一定要调用父类对应的方法，不然程序无法正常结束
                    super.onNext(item);
                }
            };

            try {

                // 执行
                // 限制拉取镜像最大时间
                // completion 为限制时间内拉取镜像的完成情况
                boolean completion = pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion(PULL_IMAGE_TIMEOUT, TimeUnit.MINUTES);

                // 判断拉取镜像是否超时
                if (!completion) {

                    // 拉取镜像超时，回收所有资源
                    pullImageCmd.close();
                    DOCKER_CLIENT.close();

                    // 抛出超时异常
                    throw new TimeoutException("====拉取镜像超时，请检查docker环境（例如网络、docker仓库节点是否可以访问）====");
                }
            } catch (InterruptedException | TimeoutException | IOException e) {
                throw new RuntimeException("具体原因有：" + e.getMessage(),e);
            }

            // 拉取镜像完毕，回收资源
            pullImageCmd.close();
        } else {
            System.out.println("镜像环境正常");
        }
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        // 获取运行代码和输入用例
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();

        /*
         *  1.将用户代码保存为.java文件
         */
        File codeStrToJavaFile = super.codeStrToJavaFile(code);

        /*
         *  2.将用户代码.java文件编译为.class文件
         */
        try {
            ExecuteMessage executeMessage = super.compileJavaFile(codeStrToJavaFile);

            System.out.println(executeMessage);
        } catch (IOException e) {

            // 通过错误处理方法直接返回代码沙箱的错误或异常
            return super.getErrorResponse(e);
        }

        /*
         * 3.创建容器，上传编译文件（镜像已初始化好）,启动容器执行代码并获取执行结果，回收资源
         */
        // 初始化代码执行结果信息为空
        List<ExecuteMessage> executeMessageList = null;
        try {
            executeMessageList = execUserCodeAndGetMessage(codeStrToJavaFile,inputList);
        } catch (Exception e) {
            return super.getErrorResponse(e);
        }

        /*
         * 4.收集整理输出结果
         */
        ExecuteCodeResponse executeCodeResponse = handleExecuteMessage(executeMessageList);

        /*
         * 5.文件清理
         */
        boolean deleteFile = super.deleteFile(codeStrToJavaFile);

        // 未删除的话会日志输出文件绝对路径的
        if (!deleteFile) {
            log.error("deleteFile error, userCodeFilePath = {}",codeStrToJavaFile.getAbsolutePath());
        }

        return executeCodeResponse;
    }

    /**
     * 3.创建容器，上传编译文件（镜像已初始化好）,启动容器执行代码并获取执行结果，回收资源
     * （1）创建容器，上传编译文件（镜像已初始化好）
     * （2）启动容器，初始化监控容器执行命令过程内存使用命令，开启监控，初始化容器执行命令，容器执行代码,循环结束后关闭监控
     * （3）回收资源，结束交互式容器进程，释放内存，删除交互式容器，释放存储
     * @param userCodeFile 用户代码.java文件
     * @param inputList 习题输入用例
     * @return
     */
    @Override
    public List<ExecuteMessage> execUserCodeAndGetMessage(File userCodeFile, List<String> inputList) throws Exception {

        /*
         * （1）创建容器，上传编译文件（镜像已初始化好）
         */
        // 初始化 docker create container 命令
        // 创建容器操作
        CreateContainerCmd containerCmd = DOCKER_CLIENT.createContainerCmd(IMAGE);
        // 初始化容器相关配置，例如给容器复制一些文件等（只是大概了解）
        HostConfig hostConfig = new HostConfig();
        // 初始化容器映射，避免放在容器根目录而发生不必要环境错误或污染
        Volume volume = new Volume(CONTAINER_VOLUME_PATH);
        // 获取用户代码文件父目录绝对路径
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 将本地（系统）的文件 userCodeParentPath 绑定到容器映射，作用是把本地的文件同步到容器中，可以让容器访问
        Bind bind = new Bind(userCodeParentPath, volume);
        // 设置容器映射文件配置
        hostConfig.setBinds(bind);
        // 设置容器内存配置
        hostConfig.withMemory(CONTAINER_MEMORY);
        // 设置容器CPU配置
        hostConfig.withCpuCount(CONTAINER_CPU_COUNT);
        // 获取权限管理JSON配置
        // String securityConfigs = ResourceUtil.readUtf8Str("seccomp.json");
        // 设置容器权限管理配置（根据获得的权限管理JSON配置）
        // 可以限制读、写等操作，防止恶意代码造成损失
        // todo 该限制无法控制，目前暂不使用
        // hostConfig.withSecurityOpts(Collections.singletonList("seccomp=" + securityConfigs));

        // 执行创建容器并配置一些相关配置
        // 相关配置如下：
        // withHostConfig 映射本地文件供容器访问，访问编译后的代码可以直接执行，还有一个可以确保容器能成功访问文件并执行成功的好处
        // 输入、输出、错误、异常
        // withTty 创建一个交互式容器，能接受多次输入并且输出，考虑到习题会有多个测试用例的情况，不用一个测试用例创建一个容器，容器启动后
        // 一直保持运行，进行接受参数输出结果
        // withNetworkDisabled 为禁用容器网络，限制用户代码访问网络，造成系统损失
        // withReadonlyRootfs 为限制用户代码向root目录写文件
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec();
        // 获取容器ID
        String containerId = createContainerResponse.getId();
        // 判断容器是否创建成功
        if (containerId.isEmpty()) {
            IOException ioException = new IOException("====获取容器ID失败，容器创建出现异常====警告有====" + Arrays.toString(createContainerResponse.getWarnings()));
            getErrorResponse(ioException);
        }
        // 输出容器创建结果
        System.out.println("====容器创建结果为：" + createContainerResponse + "====");

        /*
         * （2）启动容器，初始化监控容器执行命令过程内存使用命令，开启监控，初始化容器执行命令，容器执行代码,循环结束后关闭监控
         */
        // 初始化 docker start container
        // 启动容器操作
        // 根据容器信息获取其id值
        StartContainerCmd startContainerCmd = DOCKER_CLIENT.startContainerCmd(containerId);
        // 执行
        startContainerCmd.exec();

        // 初始化 docker exec container command... 命令
        // 创建容器执行操作（这只是创建并未真正执行命令）
        ExecCreateCmd execCreateCmd = DOCKER_CLIENT.execCreateCmd(containerId);
        // 创建容器执行命令操作
        ExecStartCmd execStartCmd = DOCKER_CLIENT.execStartCmd(containerId);

        // 初始化所有用例执行结果信息
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        // 通过Spring框架的StopWatch类来统计执行程序消耗时间（本次统计并非精确到实际用户代码执行时间，只是从发送请求到docker、执行、完成、回调的时间）
        // 运行时间影响因素很多，如网络、docker环境等
        StopWatch stopWatch = new StopWatch();

        // 初始化 docker stats container 命令
        // 查看容器内存使用操作
        // 在容器执行命令前开启，提前创建，循环时直接使用即可，无需多次创建，浪费性能，记得最后关闭，这是类似监控程序，开启后会一直运行
        StatsCmd statsCmd = DOCKER_CLIENT.statsCmd(containerId);
        // 初始化容器内存信息为0
        final long[] execMemory = {0L};
        // 初始化查看容器内存回调参数
        // Statistics 统计信息中获取内存使用情况
        ResultCallback<Statistics> statisticsResultCallback = new ResultCallback<Statistics>() {

            @Override
            public void onNext(Statistics statistics) {

                // 获取容器执行命令最大内存占用值
                execMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), execMemory[0]);
                System.out.println("====容器执行命令过程最大内存占用为：====" + execMemory[0] + "====");

            }

            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        };

        // 根据输入用例，配置容器执行命令参数并执行
        for (String inputArgs : inputList) {

            // 处理输入用例之间的空格，得到一个输入用例数组，这样就容器执行命令的参数为多个，而不是当成一个参数来看
            String[] inputArgsArray = inputArgs.split(" ");

            // docker 人工输入实例：docker exec 884e445fbe30 java -cp /tempRunCodeMapper Main 1 2
            // 设置容器执行命令操作 command...
            // 通过 ArrayUtil.append 将多个元素进行拼接
            String[] cmdArray = ArrayUtil.append(new String[] {"java","-cp","/tempRunCodeMapper","Main"},inputArgsArray);

            // 配置容器执行命令以及执行创建
            ExecCreateCmdResponse execCreateCmdResponse = execCreateCmd
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            // 获取启动容器执行命令ID
            String execId = execCreateCmdResponse.getId();
            // 判断执行命令是否创建成功
            if (execId.isEmpty()) {
                IOException ioException = new IOException("====获取命令执行ID失败，命令执行创建出现异常====" + execCreateCmdResponse);
                getErrorResponse(ioException);
            }
            System.out.println("====容器执行创建结果为：" + execCreateCmdResponse + "====");

            // 初始化输入用例执行结果
            ExecuteMessage executeMessage = new ExecuteMessage();
            // 初始化用户代码执行时间为0
            long execTime = 0L;

            // 开启监控容器执行命令内存使用情况
            // 此命令为监控程序，设计为开启后常驻后台运行，所以之后要关闭
            // 容器执行命令扩大内存后，无法短时间释放内存，所以执行完第一次的内存，
            // 基本内存变化不大，最有可能就是突然增大，遇到算量大的参数
            statsCmd.exec(statisticsResultCallback);

            // 初始化输入用例执行信息为空
            final String[] message = {null};
            // 初始化输入用例执行错误信息为空
            final String[] errorMessage = {null};
            // 初始化启动容器执行命令回调参数
            // 执行需要时间，所以该启动为异步，也要注意阻塞程序便于获取执行结果
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {

                @Override
                public void onComplete() {
                    System.out.println("====命令在规定时间内执行完成====");
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    // 由于环境不稳定，每次循环执行创建命令来执行用户代码，会有一个异步回调响应
                    // 按道理只执行一次就可以了，获取结果，但是有时不稳定会多次输出，
                    // 后边输出的结果为空，这里的结果值通过byte[] payload数组来传值，结果为
                    // 的话，byte[] payload = [10] ，所以判断该数组长度是否不大于1的话
                    // 就是结果为空，所以不进行后边赋值操作，这样就可以避免代码执行结果意外
                    // 这里先留个隐患，就是我现在不确定错误输出的话，byte[] payload 是
                    // 什么样的，但是呢，我感觉应该他的长度肯定大于1的，之后再看吧
                    // replaceAll("[\\n\\r]","") 通过replaceAll和正则表达式替换字符串中的所有\n和\r字符
                    System.out.println("====异步执行信息====" + frame);
                    if (frame.getPayload().length <= 1) {
                        return;
                    }
                    // 获取执行结果输出类型，用于判断是正常输出还是异常错误输出
                    StreamType streamType = frame.getStreamType();
                    // 判断输出类型，确认输出内容类型
                    if (StreamType.STDERR.equals(streamType)) {
                        // todo 了解这块为啥要用final单个数组包装呢，errorMessage[0]
                        // 保存输入用例执行错误信息
                        errorMessage[0] = new String(frame.getPayload()).replaceAll("[\\n\\r]","");
                        System.out.println("====输出异常错误结果为：" + errorMessage[0] + "====");
                    } else {
                        // 保存输入用例执行信息
                        message[0] = new String(frame.getPayload()).replaceAll("[\\n\\r]","");
                        System.out.println("====输出结果为：" + message[0] + "====");
                    }
                    super.onNext(frame);
                }
            };

            // 执行容器命令（也以为执行用户代码）
            try {

                // 为了保证监控到每次输入用例运行过程所占用的内存，让主线程休眠一会，回调可以获取完整的内存占用情况
                Thread.sleep(1000L);

                // 开始检测
                stopWatch.start();

                // 执行
                // 限制容器执行命令最大时间
                // 防止恶意代码一直运行占用docker资源
                // completion 为规定时间内执行命令完成情况,true 为未超时，false 为超时
                boolean completion = execStartCmd
                        .withExecId(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(EXEC_TIMEOUT, TimeUnit.SECONDS);

                // 停止检测
                stopWatch.stop();
                // 获取用户代码执行时间（粗略统计）
                // 根据 completion 命令执行完成情况，设置命令执行完成时间
                if (completion) {
                    execTime = stopWatch.getLastTaskTimeMillis();
                }

                System.out.println("====用户代码执行时间====" + execTime + "====execTime = 0L 表示执行超时（正常情况下）====");

                // 为了保证监控到每次输入用例运行过程所占用的内存，让主线程休眠一会，回调可以获取完整的内存占用情况
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                throw new RuntimeException("程序执行异常" + e);
            }

            // 保存输入用例执行结果信息
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setExecuteTime(execTime);
            executeMessage.setExecuteMemory(execMemory[0]);
            executeMessageList.add(executeMessage);

            execMemory[0] = 0L;

            System.out.println("====当前内存是否被初始化呢====" + execMemory[0] + "====");
        }

        // 关闭容器执行命令所需要的资源配置（关闭监控）
        execCreateCmd.close();
        execStartCmd.close();
        statsCmd.close();

        System.out.println("====执行信息列表====" + executeMessageList + "====");

        /*
         * （3）回收资源，结束交互式容器进程，释放内存，删除交互式容器，释放存储
         */
        // 初始化 docker stop container 命令
        // 终止容器运行操作
        StopContainerCmd stopContainerCmd = DOCKER_CLIENT.stopContainerCmd(containerId);
        // 执行
        stopContainerCmd.exec();

        // 初始化 docker rm container 命令
        // 删除镜像操作
        RemoveContainerCmd removeContainerCmd = DOCKER_CLIENT.removeContainerCmd(containerId);
        // 执行
        removeContainerCmd.exec();
        return executeMessageList;
    }

    /**
     * 收集整理输出结果
     * @param executeMessageList 执行结果信息列表
     */
    @Override
    public ExecuteCodeResponse handleExecuteMessage(List<ExecuteMessage> executeMessageList) {

        // 初始化代码沙箱执行响应
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        // 初始化执行结果
        List<String> executeOutputList = new ArrayList<>();

        // 初始化用例执行消耗时间最大值，用于判断程序执行是否超时
        long maxExecuteTime = 0L;
        // 初始化用例执行占用内存最大值，用于判断程序执行是否符合规定内存
        long maxExecuteMemory = 0L;

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

            // 获取程序执行占用内存
            Long memory = executeMessage.getExecuteMemory();

            // 获取程序执行占用内存最大值
            if (memory != null) {
                maxExecuteMemory = Math.max(memory,maxExecuteMemory);
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

        // 设置程序执行占用内存
        judgeInfo.setMemory(maxExecuteMemory);

        // 设置响应程序执行信息
        executeCodeResponse.setJudgeInfo(judgeInfo);

        return executeCodeResponse;
    }
}
