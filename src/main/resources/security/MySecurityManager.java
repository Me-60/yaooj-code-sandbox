import java.security.Permission;

/**
 * 自定义安全管理器
 * 检测权限如下
 * checkExec
 * checkRead
 * checkWrite
 * checkDelete
 * checkConnect
 * checkPropertiesAccess
 */
public class MySecurityManager extends SecurityManager{

    static {
        System.out.println("====自定义检测禁止权限====");
    }

    /**
     * 检查所有权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {

        // 显示检测到的权限
        System.out.println("====权限检测有====" + perm);
    }

    @Override
    public void checkExec(String cmd) {

        // 抛出禁止权限SecurityException
        throw new SecurityException(cmd + "====该文件具有创建子进程权限====");
    }

    @Override
    public void checkRead(String file) {

        // 抛出禁止权限SecurityException
        throw new SecurityException(file + "====该文件具有读权限====");
    }

    @Override
    public void checkWrite(String file) {

        // 抛出禁止权限SecurityException
        throw new SecurityException(file + "====该文件具有写权限====");
    }

    @Override
    public void checkDelete(String file) {

        // 抛出禁止权限SecurityException
        throw new SecurityException(file + "====该文件具有删除权限====");
    }

    @Override
    public void checkConnect(String host, int port) {

        // 抛出禁止权限SecurityException
        throw new SecurityException(host + ":"+ port + "====该文件具有访问网络权限====");
    }

    @Override
    public void checkPropertiesAccess() {

        // 抛出禁止权限SecurityException
        throw new SecurityException("====该文件具有访问或修改系统属性权限====");
    }
}
