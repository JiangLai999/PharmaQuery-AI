package com.pharmacy.service;

import com.pharmacy.dto.NlpParseResult;

/**
 * NLP服务接口 - 调用Python微服务进行实体识别
 */
public interface NlpService {

    /**
     * 解析用户自然语言查询
     * @param query 用户输入的自然语言
     * @return 结构化解析结果
     */
    NlpParseResult parseQuery(String query);
}
