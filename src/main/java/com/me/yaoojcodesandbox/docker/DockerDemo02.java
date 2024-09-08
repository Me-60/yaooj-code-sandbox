package com.me.yaoojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;

import java.util.List;

public class DockerDemo02 {

    public static void main(String[] args) {

        // 获取默认的 docker client builder 的实例
        DockerClientBuilder dockerClientBuilder = DockerClientBuilder.getInstance();

        // 通过 docker client builder 初始化一个默认的 docker client
        DockerClient dockerClient = dockerClientBuilder.build();

        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();

        List<Container> containers = listContainersCmd.withShowAll(true).exec();

        listContainersCmd.close();

        System.out.println(containers);
    }
}
