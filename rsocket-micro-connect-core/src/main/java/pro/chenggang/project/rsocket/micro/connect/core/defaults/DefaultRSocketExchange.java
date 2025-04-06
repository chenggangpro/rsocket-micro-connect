package pro.chenggang.project.rsocket.micro.connect.core.defaults;

import io.rsocket.Payload;
import io.rsocket.metadata.WellKnownMimeType;
import lombok.NonNull;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Default rsocket exchange.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class DefaultRSocketExchange implements RSocketExchange {

    private final RSocketExchangeType type;
    private final Payload payload;
    private final WellKnownMimeType dataMimeType;
    private final WellKnownMimeType metadataMimeType;
    private final Map<String, Object> attributes;
    private final Throwable throwable;

    /**
     * New exchange default rsocket exchange.
     *
     * @param type             the rsocket exchange type
     * @param dataMimeType     the data mime type
     * @param metadataMimeType the metadata mime type
     * @return the default rsocket exchange
     */
    public static DefaultRSocketExchange newExchange(@NonNull RSocketExchangeType type,
                                                     WellKnownMimeType dataMimeType,
                                                     WellKnownMimeType metadataMimeType) {
        return new DefaultRSocketExchange(type, null, dataMimeType, metadataMimeType, null, null);
    }

    /**
     * New exchange default rsocket exchange.
     *
     * @param type             the rsocket exchange type
     * @param dataMimeType     the data mime type
     * @param metadataMimeType the metadata mime type
     * @param throwable        the throwable
     * @return the default rsocket exchange
     */
    public static DefaultRSocketExchange newExchange(@NonNull RSocketExchangeType type,
                                                     WellKnownMimeType dataMimeType,
                                                     WellKnownMimeType metadataMimeType,
                                                     Throwable throwable) {
        return new DefaultRSocketExchange(type, null, dataMimeType, metadataMimeType, null, throwable);
    }

    /**
     * New exchange default rsocket exchange.
     *
     * @param type             the rsocket exchange type
     * @param payload          the payload
     * @param dataMimeType     the data mime type
     * @param metadataMimeType the metadata mime type
     * @return the default rsocket exchange
     */
    public static DefaultRSocketExchange newExchange(@NonNull RSocketExchangeType type,
                                                     Payload payload,
                                                     WellKnownMimeType dataMimeType,
                                                     WellKnownMimeType metadataMimeType) {
        return new DefaultRSocketExchange(type, payload, dataMimeType, metadataMimeType, null, null);
    }

    /**
     * New exchange default rsocket exchange.
     *
     * @param type             the rsocket exchange type
     * @param dataMimeType     the data mime type
     * @param metadataMimeType the metadata mime type
     * @param attributes       the attributes
     * @return the default rsocket exchange
     */
    public static DefaultRSocketExchange newExchange(@NonNull RSocketExchangeType type,
                                                     WellKnownMimeType dataMimeType,
                                                     WellKnownMimeType metadataMimeType,
                                                     Map<String, Object> attributes) {
        return new DefaultRSocketExchange(type, null, dataMimeType, metadataMimeType, attributes, null);
    }

    /**
     * New exchange default rsocket exchange.
     *
     * @param type             the rsocket exchange type
     * @param dataMimeType     the data mime type
     * @param metadataMimeType the metadata mime type
     * @param attributes       the attributes
     * @param throwable        the throwable
     * @return the default rsocket exchange
     */
    public static DefaultRSocketExchange newExchange(@NonNull RSocketExchangeType type,
                                                     WellKnownMimeType dataMimeType,
                                                     WellKnownMimeType metadataMimeType,
                                                     Map<String, Object> attributes,
                                                     Throwable throwable) {
        return new DefaultRSocketExchange(type, null, dataMimeType, metadataMimeType, attributes, throwable);
    }

    /**
     * New exchange default rsocket exchange.
     *
     * @param type             the rsocket exchange type
     * @param payload          the payload
     * @param dataMimeType     the data mime type
     * @param metadataMimeType the metadata mime type
     * @param attributes       the attributes
     * @return the default rsocket exchange
     */
    public static DefaultRSocketExchange newExchange(@NonNull RSocketExchangeType type,
                                                     Payload payload,
                                                     WellKnownMimeType dataMimeType,
                                                     WellKnownMimeType metadataMimeType,
                                                     Map<String, Object> attributes) {
        return new DefaultRSocketExchange(type, payload, dataMimeType, metadataMimeType, attributes, null);
    }

    private DefaultRSocketExchange(@NonNull RSocketExchangeType type,
                                   Payload payload,
                                   WellKnownMimeType dataMimeType,
                                   WellKnownMimeType metadataMimeType,
                                   Map<String, Object> attributes,
                                   Throwable throwable) {
        this.type = type;
        this.payload = payload;
        this.dataMimeType = dataMimeType;
        this.metadataMimeType = metadataMimeType;
        this.attributes = Objects.isNull(attributes) ? new ConcurrentHashMap<>() : attributes;
        this.throwable = throwable;
    }

    @Override
    public RSocketExchangeType getType() {
        return this.type;
    }

    @Override
    public Optional<Payload> getPayload() {
        return Optional.ofNullable(this.payload);
    }

    @Override
    public WellKnownMimeType getDataMimeType() {
        return this.dataMimeType;
    }

    @Override
    public WellKnownMimeType getMetadataMimeType() {
        return this.metadataMimeType;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Optional<Throwable> getError() {
        return Optional.ofNullable(this.throwable);
    }

}
