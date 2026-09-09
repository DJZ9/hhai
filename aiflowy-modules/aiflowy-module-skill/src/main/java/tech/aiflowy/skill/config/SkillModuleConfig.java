package tech.aiflowy.skill.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@MapperScan("tech.aiflowy.skill.mapper")
@AutoConfiguration
public class SkillModuleConfig {

    public SkillModuleConfig() {
        System.out.println("启用模块 >>>>>>>>>> module-skill");
    }
}
