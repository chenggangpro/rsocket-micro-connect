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
package pro.chenggang.project.rsocket.micro.connect.spring.proxy;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketRequesterRegistry;

import java.io.Serial;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil.unwrapThrowable;

/**
 * The RSocket Micro Connector Proxy.
 *
 * @param <T> the type parameter
 * @author Gang Cheng
 */
public class RSocketMicroConnectorProxy<T> implements InvocationHandler, Serializable {

    @Serial
    private static final long serialVersionUID = 3180228632844338801L;
    private static final int ALLOWED_MODES = Lookup.PRIVATE | Lookup.PROTECTED | Lookup.PACKAGE | Lookup.PUBLIC;
    private static final Constructor<Lookup> lookupConstructor;
    private static final Method privateLookupInMethod;

    static {
        Method privateLookupIn;
        try {
            privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, Lookup.class);
        } catch (NoSuchMethodException e) {
            privateLookupIn = null;
        }
        privateLookupInMethod = privateLookupIn;

        Constructor<Lookup> lookup = null;
        if (privateLookupInMethod == null) {
            // JDK 1.8
            try {
                lookup = Lookup.class.getDeclaredConstructor(Class.class, int.class);
                lookup.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "There is neither 'privateLookupIn(Class, Lookup)' nor 'Lookup(Class, int)' method " +
                                "in java.lang.invoke.MethodHandles.",
                        e
                );
            } catch (Exception e) {
                lookup = null;
            }
        }
        lookupConstructor = lookup;
    }

    private final Class<T> connectorInterface;
    private final RSocketRequesterRegistry rSocketRequesterRegistry;
    @Nullable
    private final ConversionService conversionService;
    private final List<RSocketMicroConnectorExecutionCustomizer> connectorExecutionCustomizers;
    private final Map<Method, MicroConnectorMethodInvoker> connectorMethodCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new rsocket connector proxy.
     *
     * @param connectorInterface            the connector interface
     * @param rSocketRequesterRegistry      the rsocket requester registry
     * @param conversionService             the conversion service
     * @param connectorExecutionCustomizers the connector execution customizer list
     */
    public RSocketMicroConnectorProxy(Class<T> connectorInterface,
                                      RSocketRequesterRegistry rSocketRequesterRegistry,
                                      ConversionService conversionService,
                                      List<RSocketMicroConnectorExecutionCustomizer> connectorExecutionCustomizers) {
        this.connectorInterface = connectorInterface;
        this.rSocketRequesterRegistry = rSocketRequesterRegistry;
        this.conversionService = conversionService;
        this.connectorExecutionCustomizers = connectorExecutionCustomizers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            }
            return cachedInvoker(method).invoke(proxy, method, args, rSocketRequesterRegistry);
        } catch (Throwable t) {
            throw unwrapThrowable(t);
        }
    }

    private MicroConnectorMethodInvoker cachedInvoker(Method method) throws Throwable {
        try {
            return connectorMethodCache.computeIfAbsent(method, m -> {
                        if (m.isDefault()) {
                            try {
                                if (privateLookupInMethod == null) {
                                    return new DefaultMicroConnectorMethodInvoker(getMethodHandleJava8(method));
                                }
                                return new DefaultMicroConnectorMethodInvoker(getMethodHandleJava9(method));
                            } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                                     NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            return new PlainMicroConnectorMethodInvoker(new RSocketMicroConnectorMethod(connectorInterface,
                                    method,
                                    conversionService,
                                    connectorExecutionCustomizers
                            ));
                        }
                    }
            );
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            throw cause == null ? re : cause;
        }
    }

    private MethodHandle getMethodHandleJava9(Method method) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return ((Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup()))
                .findSpecial(declaringClass,
                        method.getName(),
                        MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                        declaringClass
                );
    }

    private MethodHandle getMethodHandleJava8(Method method) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return lookupConstructor.newInstance(declaringClass, ALLOWED_MODES)
                .unreflectSpecial(method, declaringClass);
    }

    /**
     * The Micro Connector Method invoker.
     */
    interface MicroConnectorMethodInvoker {

        /**
         * Invoke connector method.
         *
         * @param proxy                    the proxy
         * @param method                   the method
         * @param args                     the args
         * @param rSocketRequesterRegistry the rsocket requester registry
         * @return the object
         * @throws Throwable the throwable
         */
        Object invoke(Object proxy,
                      Method method,
                      Object[] args,
                      RSocketRequesterRegistry rSocketRequesterRegistry) throws Throwable;
    }

    private static class PlainMicroConnectorMethodInvoker implements MicroConnectorMethodInvoker {

        private final RSocketMicroConnectorMethod rSocketMicroConnectorMethod;

        /**
         * Instantiates a new Plain connector method invoker.
         *
         * @param rSocketMicroConnectorMethod the rsocket connector method
         */
        public PlainMicroConnectorMethodInvoker(RSocketMicroConnectorMethod rSocketMicroConnectorMethod) {
            this.rSocketMicroConnectorMethod = rSocketMicroConnectorMethod;
        }

        @Override
        public Object invoke(Object proxy,
                             Method method,
                             Object[] args,
                             RSocketRequesterRegistry rSocketRequesterRegistry) throws Throwable {
            return rSocketMicroConnectorMethod.execute(rSocketRequesterRegistry, args);
        }
    }

    private static class DefaultMicroConnectorMethodInvoker implements MicroConnectorMethodInvoker {

        private final MethodHandle methodHandle;

        /**
         * Instantiates a new Default connector method invoker.
         *
         * @param methodHandle the method handle
         */
        public DefaultMicroConnectorMethodInvoker(MethodHandle methodHandle) {
            this.methodHandle = methodHandle;
        }

        @Override
        public Object invoke(Object proxy,
                             Method method,
                             Object[] args,
                             RSocketRequesterRegistry rSocketRequesterRegistry) throws Throwable {
            return methodHandle.bindTo(proxy).invokeWithArguments(args);
        }
    }

}
