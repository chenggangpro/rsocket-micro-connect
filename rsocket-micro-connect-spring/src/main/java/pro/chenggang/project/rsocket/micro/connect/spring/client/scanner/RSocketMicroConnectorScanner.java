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
package pro.chenggang.project.rsocket.micro.connect.spring.client.scanner;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RSocketMicroConnector;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.RSocketMicroConnectorRegistry;

import java.util.Optional;
import java.util.Set;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class RSocketMicroConnectorScanner extends ClassPathBeanDefinitionScanner {

    public RSocketMicroConnectorScanner(@NonNull BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        this.processBeanDefinitions(beanDefinitions);
        return beanDefinitions;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

    protected void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        AbstractBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (AbstractBeanDefinition) holder.getBeanDefinition();
            if (ScopedProxyFactoryBean.class.getName().equals(definition.getBeanClassName())) {
                definition = (AbstractBeanDefinition) Optional.ofNullable(((RootBeanDefinition) definition).getDecoratedDefinition())
                        .map(BeanDefinitionHolder::getBeanDefinition)
                        .orElseThrow(() -> new IllegalStateException(
                                "The target bean definition of scoped proxy bean not found. Root bean definition[" + holder + "]")
                        );
            }
            String beanClassName = definition.getBeanClassName();
            log.debug("Creating RSocketMicroConnectorFactoryBean with name '{}' and '{}' rSocket micro connector interface ",
                    holder.getBeanName(),
                    beanClassName
            );
            try {
                Class<?> beanClass = RSocketMicroConnectUtil.classForName(beanClassName,
                        RSocketMicroConnectUtil.getClassLoaders(this.getClass().getClassLoader())
                );
                definition.setAttribute("factoryBeanObjectType", beanClass);
                definition.getConstructorArgumentValues().addIndexedArgumentValue(0, beanClass);
            } catch (ClassNotFoundException ignore) {
                // ignore
            }
            definition.setBeanClass(RSocketMicroConnectorFactoryBean.class);
            definition.getConstructorArgumentValues()
                    .addIndexedArgumentValue(1, new RuntimeBeanReference(RSocketMicroConnectorRegistry.class));
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            definition.setLazyInit(true);
            definition.setScope(SCOPE_SINGLETON);
        }
    }

    public void registerFilters() {
        addIncludeFilter(new AnnotationTypeFilter(RSocketMicroConnector.class));
        // exclude package-info.java
        addExcludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            return className.endsWith("package-info");
        });
    }

}
