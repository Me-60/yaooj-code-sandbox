package com.me.yaoojcodesandbox.security;

import java.security.Permission;

/**
 * 默认安全管理器
 */
public class DefaultSecurityManager extends SecurityManager{

    static {
        System.out.println("====仅检测权限非禁止====");
    }

    /**
     * 检查所有权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("====权限检测有====" + perm);
    }
}
