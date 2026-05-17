package com.wutheringwaves.gacha.base;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
    // 通用测试配置
}
