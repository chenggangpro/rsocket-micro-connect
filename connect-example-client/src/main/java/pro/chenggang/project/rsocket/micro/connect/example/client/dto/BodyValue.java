package pro.chenggang.project.rsocket.micro.connect.example.client.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@Builder
@Jacksonized
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BodyValue {

    private final String name;

    private final String value;
}
