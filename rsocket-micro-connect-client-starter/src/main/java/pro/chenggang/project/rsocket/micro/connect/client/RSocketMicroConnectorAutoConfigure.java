/*
 *    Copyright 2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package pro.chenggang.project.rsocket.micro.connect.client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import pro.chenggang.project.rsocket.micro.connect.spring.client.scanner.RSocketMicroConnectorScanner;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The rsocket micro connector autoconfigure.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
public class RSocketMicroConnectorAutoConfigure implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, EnvironmentAware {

    private ApplicationContext applicationContext;
    private List<String> additionalPackages;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        RSocketMicroConnectorScanner scanner = new RSocketMicroConnectorScanner(registry);
        scanner.setResourceLoader(this.applicationContext);
        scanner.registerFilters();
        scanner.scan(StringUtils.tokenizeToStringArray(getTargetPackages(),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS
        ));
    }

    private String getTargetPackages() {
        List<String> basePackages = AutoConfigurationPackages.get(this.applicationContext.getAutowireCapableBeanFactory());
        if (!CollectionUtils.isEmpty(additionalPackages)) {
            basePackages.addAll(additionalPackages);
        }
        return String.join(",", basePackages);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public void setEnvironment(Environment environment) {
        AbstractEnvironment abstractEnvironment = (AbstractEnvironment) environment;
        this.additionalPackages = abstractEnvironment.getPropertySources()
                .stream()
                .flatMap(propertySource -> {
                    if (!MapPropertySource.class.isAssignableFrom(propertySource.getClass())) {
                        return Stream.empty();
                    }
                    MapPropertySource mapPropertySource = (MapPropertySource) propertySource;
                    return mapPropertySource.getSource()
                            .entrySet()
                            .stream()
                            .filter(entry -> entry.getKey().startsWith("rsocket-micro-connect.client.micro-connector-package"))
                            .map(Entry::getValue);
                })
                .map(value -> {
                    if (Objects.isNull(value) || !OriginTrackedValue.class.isAssignableFrom(value.getClass())) {
                        return null;
                    }
                    Object originTrackedValue = ((OriginTrackedValue) value).getValue();
                    if (!(originTrackedValue instanceof String)) {
                        return null;
                    }
                    return (String) originTrackedValue;
                })
                .distinct()
                .collect(Collectors.toList());
    }
}
