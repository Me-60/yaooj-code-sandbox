package com.me.yaoojcodesandbox.test;

import cn.hutool.core.io.resource.ResourceUtil;
import com.me.yaoojcodesandbox.impl.JavaNativeCodeSandbox;
import com.me.yaoojcodesandbox.model.ExecuteCodeRequest;
import com.me.yaoojcodesandbox.model.ExecuteCodeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 该类主要为了测试JavaNativeCodeSandbox中的创建tempRunCodeStorage目录
 * 以及对于用户代码的目录和文件的保存创建是否成功
 * 除了测试是否创建成功外，该类也是开发JavaNativeCodeSandbox类整个
 * 设计的测试类
 */
public class TempRunCodeStorageNativeTest {

    public static void main(String[] args) {

        // 通过hutool的ResourceUtil.readStr()方法将读取路径为testCode/example/Main.java的目标文件
        // 模拟判题模块传输过来的请求参数，这里通过读取resource中的文件来设置ExecuteCodeRequest
        String code = ResourceUtil.readStr("testCode/example/Main.java", StandardCharsets.UTF_8);
        // String code = ResourceUtil.readStr("testCode/examplescanner/Main.java", StandardCharsets.UTF_8);交互式
        // String code = ResourceUtil.readStr("testCode/unsafe/MemoryError.java", StandardCharsets.UTF_8);
        // String code = ResourceUtil.readStr("testCode/unsafe/ReadFileError.java", StandardCharsets.UTF_8);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(Arrays.asList("1 2","3 4"))
                .language("java")
                .build();

        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
