package com.me.yaoojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;

/**
 * 此类为了测试 java-docker 的各命令执行情况的 demo
 * 每个命令的执行都可以通过链式来增添相关条件，例如增加
 * 等待方法执行完毕阻塞程序的方法以及正常输出和异常输出
 * 可以看出 java-docker 底层就是拼接命令来完成docker
 * 的各种操作
 */
public class DockerDemo {

    public static void main(String[] args) throws InterruptedException {

        // 获取默认的 docker client builder 的实例
        DockerClientBuilder dockerClientBuilder = DockerClientBuilder.getInstance();

        // 通过 docker client builder 初始化一个默认的 docker client
        DockerClient dockerClient = dockerClientBuilder.build();

        // 通过 docker client 完成对docker的操作
        // 初始化 docker ping 命令
        // ping操作
        // PingCmd pingCmd = dockerClient.pingCmd();
        // 执行
        // pingCmd.exec();

        // 初始化 docker pull image 命令
        // 拉取镜像操作
        // 指定镜像资源及版本号
        // String image = "nginx:latest";
        // PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        // 初始化拉取镜像结果反馈
        /*PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {

            // 此方法大概就是拉取镜像下载过程中，每个一段时间会触发 onNext 方法，给予镜像拉取过程情况反馈
            @Override
            public void onNext(PullResponseItem item) {

                System.out.println("====拉取镜像过程情况说明：" + item.getStatus() + "====");
                // super.onNext(item);
            }
        };*/
        // 执行
        // 拉取镜像为异步执行，镜像下载会有一个下载时间，需要我们设置一个拉取镜像结果反馈
        // awaitCompletion() 等待拉取镜像执行完成
        // 可能会抛出异常
        /*pullImageCmd
                .exec(pullImageResultCallback)
                .awaitCompletion();*/

        // System.out.println("拉取镜像完毕");

        // 初始化 docker create container 命令
        // 创建容器操作
        // 指定容器镜像
        // String image = "nginx:latest";
        // CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        // 执行并带有一些文本的输出（echo）
        // 返回一个容器创建结果
        /*CreateContainerResponse createContainerResponse = containerCmd
                .withCmd("echo","Hello Nginx")
                .exec();*/
        // 输出容器创建结果
        // System.out.println("====容器创建结果为：" + createContainerResponse + "====");

        // 初始化 docker ps -a 命令
        // 查看所有容器信息操作
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        // 执行
        // 返回展示所有容器信息
        List<Container> containers = listContainersCmd
                .withShowAll(true)
                .exec();
        for (Container container : containers) {
            System.out.println("====容器信息为：" + container + "====");
        }

        // 初始化 docker start container
        // 启动容器操作
        // 根据容器信息获取其id值
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containers.get(0).getId());
        // 执行
        startContainerCmd.exec();

        // 初始化 docker logs container
        // 获取容器历史日志操作
        // 根据容器信息获取其id值
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containers.get(0).getId());
        // 初始化日志结果反馈
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback() {

            @Override
            public void onNext(Frame item) {
                String log = new String(item.getPayload());
                System.out.println("====日志信息为：" + log + "====");
            }
        };
        // 执行
        // 参数为日志结果反馈，该执行为异步
        // 容器日志可能庞大，读取时间久，设计为异步，日志可以间断性反馈，同时不妨碍后边代码的运行
        // awaitCompletion() 阻塞程序，等待日志全部获取，不然程序会直接结束，不会看到日志信息的
        logContainerCmd
                .withStdOut(true)
                .withStdErr(true)
                .exec(logContainerResultCallback)
                .awaitCompletion();

        // 初始化 docker rm container 命令
        // 删除容器操作
        // 根据容器信息获取其id值
        // RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containers.get(0).getId());
        // 执行（强制）
        /*removeContainerCmd
                .withForce(true)
                .exec();*/

        // 初始化 docker rmi image 命令
        // 删除镜像操作
        // 指定删除镜像
        // String image = "nginx:latest";
        // RemoveImageCmd removeImageCmd = dockerClient.removeImageCmd(image);
        // 执行
        // removeImageCmd.exec();
    }
}
