package com.me.yaoojcodesandbox.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import com.me.yaoojcodesandbox.CodeSandbox;
import com.me.yaoojcodesandbox.constant.NonceConstant;
import com.me.yaoojcodesandbox.model.ExecuteCodeRequest;
import com.me.yaoojcodesandbox.model.ExecuteCodeResponse;
import com.me.yaoojcodesandbox.utils.JedisConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
@RequestMapping("/")
@Slf4j
public class CodeSandboxController {

    private final static String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC9rCZOTrr0W5iWRz6mQFUMKfUSyvpmNRj5ZNzNmmtN93nrbcjeWSWghOSdDIGc/piJOSSLcg+YuG4M97j+P7DiIPUSf56H/BcHoovQvAfxWYJ+diGfqPksF+Q4BH81yEebLjwuh3Rub9A+SOnJytqZSc6Mcli7U82PLaF2oHAFKwIDAQAB";

    private final static String PRIVATE_KEY = "MIICeQIBADANBgkqhkiG9w0BAQEFAASCAmMwggJfAgEAAoGBAL2sJk5OuvRbmJZHPqZAVQwp9RLK+mY1GPlk3M2aa033eettyN5ZJaCE5J0MgZz+mIk5JItyD5i4bgz3uP4/sOIg9RJ/nof8Fweii9C8B/FZgn52IZ+o+SwX5DgEfzXIR5suPC6HdG5v0D5I6cnK2plJzoxyWLtTzY8toXagcAUrAgMBAAECgYEAll4H8ETSSi7jhR/uNvcBdac9tyxb3vNxXSXtNsKZFzJ+4XojqMKb5en1I9xqVQAyBXfb1QEUBEsSVA2AB3CO1RbhcgvULcbxkfTLw7Sgjjfu6JVZSe9L6ZQ0fzAKL+H/GgPkr8WpvxaqfmZHdbzEmKvCSM+Pf79VkZNaEAP/HSECQQD38QFZpvpdMn2B166J/iUVPUgD88mE9gkB918xKTJXvVOTWjodbrCbw+ANo1ncHm5V9c5s7PWwVGBZZgn7TiL9AkEAw9ZRX6Ts29sYMrqYfKKvqqIeSBJJb3L7Ma9Wlf6u7TRZNcgtUnZ0GE/7/4cuYR1OaEDIf0zA5PrD2BjMf9jlRwJBAOZ+QPAUP7l2H6EeXZ7hCZ5GGvW5o8ScaOFQE0mDb86LLe/VOCN2bG5R2K81BWuRSqdU3LL8Uqa6udtB9dOO3w0CQQC5go5o3LEmfr/IZivGfNGNAK9QtEnuEexTn3WU9sG7n0gWgz4zjFPjJ/ldkC9l/T3l0eBbfliHT+gvDybAwLRHAkEAjKJyqU4tnabmzYp0fXIFo5WpXJpGvRBUyaMtUeKz+ivkVRcnhYVjAb/AIGn+0DUPYjm3YX1AXCqRMPtnao5n6g==";

    private final static String USER_UUID = "d0a5df71-4ac5-4d33-9c9d-d9f8454cc587";

    @Resource(name = "javaDockerCodeSandbox")
    private CodeSandbox codeSandbox;

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/execCode")
    public ExecuteCodeResponse execCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        final String timestamp = httpServletRequest.getHeader("timestamp");
        final String nonce = httpServletRequest.getHeader("nonce");
        final String username = httpServletRequest.getHeader("username");

        if (StringUtils.isAnyBlank(timestamp, nonce, username)) {
            httpServletResponse.setStatus(403);
            return null;
        }
        final long currentTime = System.currentTimeMillis() / 1000;
        final long timestamp1 = Long.valueOf(timestamp);
        if ((currentTime - timestamp1) > 5L) {
            httpServletResponse.setStatus(403);
            return null;
        }

        // 使用redis缓存来实现对于随机数存储，进行比对该随机数是否使用过，不过设置一定的缓存内存，超出后就及时清理
        if (nonceIsStored(nonce)) {
            httpServletResponse.setStatus(403);
            return null;
        }

        // todo 结合API开发平台项目，用户名是为了查询是否存在该用户，以及用户的唯一凭证，为了确保API的安全性

        final String encrypted = httpServletRequest.getHeader("encrypted");
        final byte[] decoded = Base64.decode(encrypted);
        RSA rsa = new RSA(PRIVATE_KEY, PUBLIC_KEY);
        final byte[] decrypted = rsa.decrypt(decoded, KeyType.PrivateKey);

        final String sign = DigestUtil.md5Hex(USER_UUID, StandardCharsets.UTF_8);

        byte[] signBytes = sign.getBytes(StandardCharsets.UTF_8);

        if (!Arrays.equals(decrypted,signBytes)) {
            httpServletResponse.setStatus(403);
            return null;
        }

        System.out.println("====加密====" + encrypted + "====随机数====" + nonce + "====时间戳====" + timestamp);


        System.out.println("====");
        System.out.println("====请求执行为：" + executeCodeRequest);

        // 判断执行请求参数是否为空
        if (executeCodeRequest == null) {
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            executeCodeResponse.setMessage("请求参数为空，系统异常");
            return executeCodeResponse;
        }

        return codeSandbox.executeCode(executeCodeRequest);
    }

    /**
     * 确认随机数是否已被存入
     *
     * @param nonce
     * @return
     */
    private boolean nonceIsStored(String nonce) {

        Jedis jedis = JedisConnectionFactory.getJedis();

        jedis.select(2);

        String nonceKey = NonceConstant.NONCE_KEY_PREFIX + nonce;

        String cacheNonce = jedis.get(nonceKey);

        if (cacheNonce != null) {
            log.info("随机数重复");
            return true;
        }

        jedis.setex(nonceKey, NonceConstant.NONCE_KEY_DURATION, nonce);

        jedis.close();
        return false;
    }
}
