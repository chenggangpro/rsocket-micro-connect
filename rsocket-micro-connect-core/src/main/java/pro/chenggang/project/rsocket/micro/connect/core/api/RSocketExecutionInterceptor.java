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
package pro.chenggang.project.rsocket.micro.connect.core.api;

import lombok.NonNull;

/**
 * The RSocket execution interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketExecutionInterceptor {

    /**
     * The rsocket execution interceptor type.
     *
     * @return the interceptor type
     */
    default InterceptorType interceptorType() {
        return InterceptorType.ALL;
    }

    /**
     * The order of the rsocket execution interceptor.
     *
     * @return the int default to be 0
     */
    default int order() {
        return 0;
    }

    /**
     * The rsocket execution interceptor type.
     */
    enum InterceptorType {

        /**
         * All side interceptor.
         */
        ALL,

        /**
         * Client side interceptor.
         */
        CLIENT,

        /**
         * Server side interceptor.
         */
        SERVER,

        ;

        /**
         * Determine if this is a client side rsocket execution interceptor.
         *
         * @param rSocketExecutionInterceptor the rsocket execution interceptor
         * @return the true or false
         */
        public static boolean isClientSide(@NonNull RSocketExecutionInterceptor rSocketExecutionInterceptor) {
            return ALL.equals(rSocketExecutionInterceptor.interceptorType()) || CLIENT.equals(rSocketExecutionInterceptor.interceptorType());
        }

        /**
         * Determine if this is a server side rsocket execution interceptor.
         *
         * @param rSocketExecutionInterceptor the rsocket execution interceptor
         * @return the true or false
         */
        public static boolean isServerSide(@NonNull RSocketExecutionInterceptor rSocketExecutionInterceptor) {
            return ALL.equals(rSocketExecutionInterceptor.interceptorType()) || SERVER.equals(rSocketExecutionInterceptor.interceptorType());
        }
    }

}
