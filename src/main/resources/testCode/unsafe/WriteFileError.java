import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
public class Main {

    public static void main(String[] args) throws IOException {

        // 获取项目所在目录
        String userDir = System.getProperty("user.dir");

        // 设置创建文件路径
        String filePath = userDir + File.separator + "src/main/resources/危险程序代码.bat";

        // 定义危险程序代码
        String errorProgram = "java -version 2>&1";

        // 将危险程序代码按照文件路径进行创建写入
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));

        System.out.println("危险程序被植入");
    }
}
