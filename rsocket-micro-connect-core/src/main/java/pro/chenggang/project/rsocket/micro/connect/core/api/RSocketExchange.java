package pro.chenggang.project.rsocket.micro.connect.core.api;

import io.rsocket.Payload;
import io.rsocket.metadata.WellKnownMimeType;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RemoteRSocketInfo;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The RSocket Interceptor Exchange.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketExchange {

    /**
     * Gets RSocket exchange type.
     *
     * @return the RSocket exchange type
     */
    RSocketExchangeType getType();

    /**
     * Gets payload.
     *
     * @return the payload if there is one
     */
    Optional<Payload> getPayload();

    /**
     * Gets data mime type.
     *
     * @return the data mime type
     */
    WellKnownMimeType getDataMimeType();

    /**
     * Gets data mime type.
     *
     * @param <T>               the converted mime type
     * @param mimeTypeConverter the mime type converter
     * @return the data mime type
     */
    default <T> T getDataMimeType(@NonNull Function<String, T> mimeTypeConverter) {
        return mimeTypeConverter.apply(getDataMimeType().toString());
    }

    /**
     * Gets metadata mime type.
     *
     * @return the metadata mime type
     */
    WellKnownMimeType getMetadataMimeType();

    /**
     * Gets metadata mime type.
     *
     * @param <T>               the converted mime type
     * @param mimeTypeConverter the mime type converter
     * @return the metadata mime type
     */
    default <T> T getMetadataMimeType(@NonNull Function<String, T> mimeTypeConverter) {
        return mimeTypeConverter.apply(getMetadataMimeType().toString());
    }

    /**
     * Return a mutable map of attributes for the current exchange.
     *
     * @return the attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Return the attribute value if present.
     *
     * @param <T>  the attribute type
     * @param name the attribute name
     * @return the attribute value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <T> T getAttribute(String name) {
        return (T) getAttributes().get(name);
    }

    /**
     * Return the attribute value, or a default, fallback value.
     *
     * @param <T>          the attribute type
     * @param name         the attribute name
     * @param defaultValue a default value to return instead
     * @return the attribute value
     */
    @SuppressWarnings("unchecked")
    default <T> T getAttributeOrDefault(String name, T defaultValue) {
        return (T) getAttributes().getOrDefault(name, defaultValue);
    }

    /**
     * Return the remote rsocket info.
     *
     * @return the optional remote rsocket info
     */
    default Optional<RemoteRSocketInfo> getRemoteRSocketInfo() {
        return Optional.ofNullable((RemoteRSocketInfo) getAttributes().get(RemoteRSocketInfo.class.getName()));
    }

    /**
     * Gets error.
     *
     * @return the error
     */
    Optional<Throwable> getError();

}
