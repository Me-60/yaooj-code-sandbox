package com.me.yaoojcodesandbox.test;

import cn.hutool.core.io.resource.ResourceUtil;
import com.me.yaoojcodesandbox.CodeSandbox;
import com.me.yaoojcodesandbox.impl.JavaDockerCodeSandboxLatest;
import com.me.yaoojcodesandbox.model.ExecuteCodeRequest;
import com.me.yaoojcodesandbox.model.ExecuteCodeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TempRunCodeStorageDockerLatestTest {

    public static void main(String[] args) {

        // 通过hutool的ResourceUtil.readStr()方法将读取路径为testCode/example/Main.java的目标文件
        // 模拟判题模块传输过来的请求参数，这里通过读取resource中的文件来设置ExecuteCodeRequest
        String code = ResourceUtil.readStr("testCode/example/Main.java", StandardCharsets.UTF_8);
        // String code = ResourceUtil.readStr("testCode/examplescanner/Main.java", StandardCharsets.UTF_8);交互式
        // String code = ResourceUtil.readStr("testCode/unsafe/MemoryError.java", StandardCharsets.UTF_8);
        // String code = ResourceUtil.readStr("testCode/unsafe/MemoryError.java", StandardCharsets.UTF_8);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(Arrays.asList("5 8","68 93333","1 2"))
                .language("java")
                .build();

        CodeSandbox codeSandbox = new JavaDockerCodeSandboxLatest();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
