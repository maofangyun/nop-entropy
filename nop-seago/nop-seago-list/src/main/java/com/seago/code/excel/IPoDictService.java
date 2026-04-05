package com.seago.code.excel;

import java.util.List;

/**
 * 字典服务接口
 */
public interface IPoDictService {
    /**
     * 根据字典名称获取选项列表
     */
    List<PoDictOption> getDictOptions(String dictName);
}
