package com.me.yaoojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 向服务器写文件（置入危险程序，例如木马程序）
 * 恶意用户通过相对路径写入危险文件，要是成功写入的话还会执行危险文件，造成系统崩溃
 */
public class WriteFileError {

    public static void main(String[] args) throws IOException {

        // 获取项目所在目录
        String userDir = System.getProperty("user.dir");

        // 设置创建文件路径
        String filePath = userDir + File.separator + "src/main/resources/危险程序代码.bat";

        // 定义危险程序代码
        // java -version，将版本信息输出到标准错误流中而不是标准输出流中，通过Process类完成该命令执行，而这行代码是可以正常运行的，什么也不会返回
        // java -version 2>&1 将标准错误流重新定义到标准输出流，这样Java版本信息会被发送到标准输出流，这样Process类会得到Java版本信息（有点绕）
        String errorProgram = "java -version 2>&1";

        // 将危险程序代码按照文件路径进行创建写入
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));

        System.out.println("危险程序被植入");
    }
}
