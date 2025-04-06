package pro.chenggang.project.rsocket.micro.connect.core.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The RSocket exchange type.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum RSocketExchangeType {

	/**
     * The <a href="https://rsocket.io/about/protocol#frame-setup">Setup</a> exchange.
	 */
	SETUP(false),

	/**
	 * A <a href="https://rsocket.io/about/protocol#frame-fnf">Fire and Forget</a> exchange.
	 */
	FIRE_AND_FORGET(true),

	/**
	 * A <a href="https://rsocket.io/about/protocol#frame-request-response">Request Response</a> exchange.
	 */
	REQUEST_RESPONSE(true),

	/**
	 * A <a href="https://rsocket.io/about/protocol#frame-request-stream">Request Stream</a> exchange.
	 */
	REQUEST_STREAM(true),

	/**
	 * A <a href="https://rsocket.io/about/protocol#frame-request-channel">Request Channel</a> exchange.
	 */
	REQUEST_CHANNEL(true),

	/**
	 * A <a href="https://rsocket.io/about/protocol#frame-metadata-push">Metadata Push</a> exchange.
	 */
	METADATA_PUSH(true);

	private final boolean isRequest;

}