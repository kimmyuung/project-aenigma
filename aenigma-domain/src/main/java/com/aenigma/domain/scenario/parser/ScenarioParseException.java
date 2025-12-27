package com.aenigma.domain.scenario.parser;

/**
 * 시나리오 파싱 예외
 */
public class ScenarioParseException extends RuntimeException {

    public ScenarioParseException(String message) {
        super(message);
    }

    public ScenarioParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
