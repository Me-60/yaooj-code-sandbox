package com.me.yaoojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 读取服务器文件（文件信息泄露）
 * 恶意用户通过相对路径获取项目配置文件，造成系统信息泄露，造成损失
 */
public class ReadFileError {

    public static void main(String[] args) throws IOException {

        // 获取项目所在目录
        String userDir = System.getProperty("user.dir");

        // 拼接项目配置文件路径（假设恶意用户猜的配置文件路径，况且配置文件路径一般都是下方这样的）
        String filePath = userDir + File.separator + "src/main/resources/application.yml";

        // 根据路径按行获取配置文件的详情
        List<String> allLines = Files.readAllLines(Paths.get(filePath));

        // 打印输出配置文件详情
        System.out.println(String.join("\n", allLines));
    }
}
