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
