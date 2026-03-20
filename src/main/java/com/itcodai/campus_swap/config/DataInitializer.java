package com.itcodai.campus_swap.config;

import com.itcodai.campus_swap.entity.Tag;
import com.itcodai.campus_swap.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时初始化基础数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TagMapper tagMapper;

    private static final List<String> DEFAULT_TAGS = List.of(
            "校园生活", "数码好物", "书籍教材", "服饰穿搭", "生活用品",
            "运动健身", "美妆护肤", "二手闲置", "求购求换", "换物成功",
            "九成新", "免费赠送", "包邮", "可议价", "限时特惠"
    );

    @Override
    public void run(String... args) {
        if (tagMapper.selectCount(null) == 0) {
            for (String name : DEFAULT_TAGS) {
                Tag tag = new Tag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            log.info("初始化标签数据完成，共插入 {} 条", DEFAULT_TAGS.size());
        }
    }
}
