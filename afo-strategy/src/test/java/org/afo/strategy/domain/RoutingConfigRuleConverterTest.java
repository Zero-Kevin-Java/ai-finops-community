package org.afo.strategy.domain;

import io.github.linpeilie.Converter;
import io.github.linpeilie.mapstruct.MapstructAutoConfiguration;
import org.afo.strategy.domain.vo.RoutingConfigRuleVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RoutingConfigRuleConverterTest.TestConfig.class)
@Tag("dev")
class RoutingConfigRuleConverterTest {

    @jakarta.annotation.Resource
    private Converter converter;

    @Test
    void shouldRegisterRoutingConfigRuleToVoConverter() {
        RoutingConfigRule rule = new RoutingConfigRule();
        rule.setRuleId(42L);
        rule.setTenantId("000000");
        rule.setRuleName("default route");
        rule.setStatus("0");

        RoutingConfigRuleVo vo = converter.convert(rule, RoutingConfigRuleVo.class);

        assertThat(vo).isNotNull();
        assertThat(vo.getRuleId()).isEqualTo(42L);
        assertThat(vo.getTenantId()).isEqualTo("000000");
        assertThat(vo.getRuleName()).isEqualTo("default route");
        assertThat(vo.getStatus()).isEqualTo("0");
    }

    @Configuration
    @Import(MapstructAutoConfiguration.class)
    @ComponentScan(basePackages = {
        "org.afo.strategy.domain",
        "org.afo.strategy.domain.bo",
        "org.afo.strategy.domain.vo"
    })
    static class TestConfig {
    }
}
