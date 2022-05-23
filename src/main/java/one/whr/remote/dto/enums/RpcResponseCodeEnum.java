package one.whr.remote.dto.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {
    SUCCESS(200, "Remote call successful"),
    FAIL(500, "Remote call failed");

    private final int code;
    private final String message;
}
