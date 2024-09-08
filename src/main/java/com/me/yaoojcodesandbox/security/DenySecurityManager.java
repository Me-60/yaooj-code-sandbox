package com.me.yaoojcodesandbox.security;

import java.security.Permission;

/**
 * 禁止所有权限安全管理器
 */
public class DenySecurityManager extends SecurityManager{

    static {
        System.out.println("====既检测又禁止权限====");
    }

    /**
     * 检测所有权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {

        // 抛出禁止权限SecurityException
        throw new SecurityException("====禁止权限====" + perm.toString());
    }
}
