package com.me.yaoojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DockerDemo01 {

    public static void main(String[] args) {

        // 获取默认的 docker client builder 的实例
        DockerClientBuilder dockerClientBuilder = DockerClientBuilder.getInstance();

        // 通过 docker client builder 初始化一个默认的 docker client
        DockerClient dockerClient = dockerClientBuilder.build();

        // 初始化 docker images 命令
        // 查看所有镜像操作
        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        // 执行并返回 docker 中镜像信息列表
        List<Image> images = listImagesCmd.exec();

        listImagesCmd.close();

        // 指定目标镜像
        String image = "hello-world:latest";

        // 初始化目标镜像名称及版本确认列表
        List<String> imageTags = new ArrayList<>();

        // 判断环境中是否有镜像存在，有则开始遍历提取镜像名称及版本
        // 注意区分镜像列表还是镜像名称及版本列表
        if (!images.isEmpty()) {

            // 遍历镜像列表
            for (Image image1 : images) {
                // 获取镜像名称及版本信息
                String imageTag = Arrays.stream(image1.getRepoTags()).findFirst().orElse("none");
                System.out.println("====镜像名称及版本信息为：" + imageTag + "====");

                // 遍历当前的镜像名称及版本信息是否与目标相等
                // 相等的话先填入镜像名称及版本确认列表
                if (imageTag.equals(image)) {
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
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("====拉取镜像异常为：" + throwable + "====");
                    super.onError(throwable);
                }

                @Override
                public void onComplete() {
                    System.out.println("====拉取镜像完毕====");
                    super.onComplete();
                }

                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("====拉取镜像过程情况说明：" + item.getStatus() + "====");
                    super.onNext(item);
                }
            };

            try {

                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();

                pullImageCmd.close();

//                boolean completion = imageResultCallback.awaitCompletion(3L, TimeUnit.MINUTES);
//                if (!completion) {
//                    // 拉取镜像超时，回收所有资源
//                    imageResultCallback.close();
//                    pullImageCmd.close();
//                    listImagesCmd.close();
//                    dockerClient.close();
//
//                    // 抛出超时异常
//                    throw new TimeoutException("====拉取镜像超时，请检查docker环境（例如网络、docker仓库节点是否可以访问）====");
//                }
            } catch (InterruptedException e) {
                throw new RuntimeException("拉取镜像异常：" + e);
            }
        } else {
            System.out.println("镜像环境正常");
        }
    }
}
