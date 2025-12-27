package com.aenigma.domain.scenario.parser;

/**
 * 시나리오 파서 인터페이스
 * 
 * 다양한 형식(Markdown, JSON, Excel 등)의 시나리오 파일을 파싱합니다.
 */
public interface ScenarioParser {

    /**
     * 파일 내용을 파싱하여 ParsedScenario 객체로 변환
     * 
     * @param content 파일 내용 (문자열)
     * @return 파싱된 시나리오 데이터
     * @throws ScenarioParseException 파싱 실패 시
     */
    ParsedScenario parse(String content) throws ScenarioParseException;

    /**
     * 지원하는 파일 확장자 반환
     */
    String getSupportedExtension();
}
