package com.hmdp;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
public class RedissionTest {

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedissonClient redissonClient2;
    @Resource
    private RedissonClient redissonClient3;

    private RLock lock;

    @BeforeEach
    void setup() {
        RLock lock1 =  redissonClient.getLock("order");
        RLock lock2 =  redissonClient2.getLock("order");
        RLock lock3 =  redissonClient3.getLock("order");

        // 创建联锁 multilock
        lock = redissonClient.getMultiLock(lock1, lock2, lock3);

    }

    @Test
    void method1() {
        //
    }


}
