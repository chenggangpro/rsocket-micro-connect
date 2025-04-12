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