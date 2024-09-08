package com.me.yaoojcodesandbox.impl;

import com.me.yaoojcodesandbox.model.ExecuteCodeRequest;
import com.me.yaoojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * Java 原生代码沙箱实现（直接复用模板方法）
 */
@Component("javaNativeCodeSandbox")
public class JavaNativeCodeSandboxLatest extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
