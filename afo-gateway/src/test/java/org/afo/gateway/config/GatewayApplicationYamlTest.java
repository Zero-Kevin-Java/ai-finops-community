package org.afo.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class GatewayApplicationYamlTest {

    private static final YamlPropertySourceLoader YAML_LOADER = new YamlPropertySourceLoader();

    @Test
    void gatewayBaseConfigProvidesMybatisMapperScanProperties() throws IOException {
        assertProperty("application.yml", "mybatis-plus.mapperPackage", "org.afo.**.mapper");
        assertProperty("application.yml", "mybatis-plus.mapperLocations", "classpath*:mapper/**/*Mapper.xml");
        assertProperty("application.yml", "mybatis-plus.typeAliasesPackage", "org.afo.**.domain");
    }

    @Test
    void gatewayBaseConfigProvidesCommonWebSecurityProperties() throws IOException {
        assertProperty("application.yml", "sse.path", "/resource/sse");
        assertProperty("application.yml", "websocket.path", "/resource/websocket");
        assertProperty("application.yml", "security.excludes[0]", "/*.html");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "application-dev.yml",
        "application-online.yml",
        "application-prod.yml"
    })
    void gatewayProfilesProvideDynamicDatasource(String resourceName) throws IOException {
        assertPresent(resourceName, "spring.datasource.type");
        assertPresent(resourceName, "spring.datasource.dynamic.primary");
        assertPresent(resourceName, "spring.datasource.dynamic.datasource.master.driverClassName");
        assertPresent(resourceName, "spring.datasource.dynamic.datasource.master.url");
        assertPresent(resourceName, "spring.datasource.dynamic.datasource.master.username");
    }

    private static void assertProperty(String resourceName, String key, String expectedValue) throws IOException {
        assertThat(resolveProperty(resourceName, key))
            .as("%s should define %s", resourceName, key)
            .isEqualTo(expectedValue);
    }

    private static void assertPresent(String resourceName, String key) throws IOException {
        assertThat(resolveProperty(resourceName, key))
            .as("%s should define %s", resourceName, key)
            .isNotNull();
    }

    private static Object resolveProperty(String resourceName, String key) throws IOException {
        List<PropertySource<?>> propertySources = YAML_LOADER.load(resourceName, new ClassPathResource(resourceName));
        return propertySources.stream()
            .filter(propertySource -> propertySource.containsProperty(key))
            .map(propertySource -> propertySource.getProperty(key))
            .findFirst()
            .orElse(null);
    }
}
