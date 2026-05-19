package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(RedisTemplate<Object, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 发送手机验证码
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1校验手机号
        if(RegexUtils.isPhoneInvalid(phone)) {
            //2如果不符合 返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3符合就生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4将验证码存入redis
//        session.setAttribute("code", code);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone ,code , LOGIN_CODE_TTL ,  TimeUnit.MINUTES);
        //5发送验证码
        //...   模拟
        log.debug("发送短信验证码成功，验证码：{}", code);
        //6返回成功信息
        return Result.ok();
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @param session   会话对象，用于存储登录信息
     * @return 登录结果
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        //2从redis获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.toString().equals(code)) {
            //3不一致    返回错误信息
            return Result.fail("验证码错误");
        }


        //4一致  根据手机号查询用户   select * from tb_user where phone = ?
        User user = query().eq("phone", phone).one();

        //5判断用户是否存在
        if(user == null) {
            //6如果不存在 创建新用户并保存
            user = createUserWithPhone(phone);
        }

        // 7.保存用户信息到 redis中
        // 7.1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
        // 7.2.将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user , UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        // 7.3.存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        //7.4 设置token有效期
        stringRedisTemplate.expire(tokenKey,  LOGIN_USER_TTL, TimeUnit.MINUTES);

        //8返回token
        return Result.ok(token);
    }


    /**
     * 创建用户
     * @param phone 手机号
     * @return 新创建的用户
     */
    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));
        //保存用户
        save(user);
        return user;
    }


}