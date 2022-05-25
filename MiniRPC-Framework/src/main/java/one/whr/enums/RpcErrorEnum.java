package one.whr.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public enum RpcErrorEnum {
    CLIENT_CONNECT_SERVER_FAILURE("client fails to connect to server"),
    SERVICE_INVOCATION_FAILURE("fail to invoke service"),
    SERVICE_NOT_FOUND("service not found"),
    SERVICE_NOT_IMPLEMENT_ANY_INTERFACE("service not implementing any interface"),
    REQUEST_NOT_MATCH_RESPONSE("response result mismatch");

    private final String message;
}
