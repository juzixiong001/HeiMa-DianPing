package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

public class LoginIntercepter implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public LoginIntercepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO、 1获取请求头中的token
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }
        //  2基于token获取redis用户信息
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        //  3判断用户是否存在
        if(userMap.isEmpty()) {
            //4不存在 拦截  返回401状态码
            response.setStatus(401);
            return false;
        }
        //5将查询Hash数据转成UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //6存在 保存信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        //7刷新token有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        //8放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
