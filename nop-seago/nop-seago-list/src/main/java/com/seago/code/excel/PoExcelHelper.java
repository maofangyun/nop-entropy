package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import com.seago.code.po.PropInfo;
import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.util.ApiStringHelper;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.lang.xml.XNode;
import io.nop.core.model.object.DynamicObject;
import io.nop.core.resource.IResource;
import io.nop.core.resource.impl.FileResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.excel.imp.ImportExcelParser;
import io.nop.excel.imp.model.ImportModel;
import io.nop.excel.model.ExcelWorkbook;
import io.nop.ooxml.xlsx.util.ExcelHelper;
import io.nop.xlang.api.XLang;
import io.nop.xlang.ast.XLangOutputMode;
import io.nop.xlang.xdsl.DslModelHelper;
import io.nop.core.reflect.bean.BeanTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.core.CoreErrors.ARG_CELL_POS;

/**
 * 核心辅助类：负责将 PoInfo 业务模型与 Nop 框架的 Excel 导入导出功能进行对接。
 */
public class PoExcelHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PoExcelHelper.class);

    private static final ErrorCode ERR_IMPORT_VALIDATION_FAIL = ErrorCode.define("nop.err.excel.import-validation-fail", "Excel导入校验失败");

    public static Set<String> collectDictNames(PoConfig poConfig) {
        Set<String> dictNames = new HashSet<>();
        if (poConfig != null && poConfig.getPos() != null) {
            for (PoInfo po : poConfig.getPos()) {
                dictNames.addAll(collectDictNames(po));
            }
        }
        return dictNames;
    }

    public static Set<String> collectDictNames(PoInfo poInfo) {
        Set<String> dictNames = new HashSet<>();
        if (poInfo != null && poInfo.getProps() != null) {
            for (PropInfo prop : poInfo.getProps()) {
                String dict = prop.getDict();
                if (dict != null && !dict.trim().isEmpty()) {
                    dictNames.add(dict.trim());
                }
            }
        }
        return dictNames;
    }

    public static List<Map<String, Object>> parseExcel(PoInfo poInfo, IResource resource) {
        return parseExcel(poInfo, resource, null);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> parseExcel(PoInfo poInfo, IResource resource, IPoDictService dictService) {
        if (poInfo == null) {
            return Collections.emptyList();
        }
        String poName = poInfo.getName();
        LOG.info("PoExcelHelper.parseExcel: poName={}, resource={}", poName, resource);

        if (dictService != null) {
            Set<String> dictNames = collectDictNames(poInfo);
            new PoDictGenerator(dictService).generateDictFiles(dictNames);
        }

        String impPath = "/excel/" + poName + ".imp.xml";
        ImportModel importModel = (ImportModel) ResourceComponentManager.instance().loadComponentModel(impPath);

        ExcelWorkbook wk = ExcelHelper.parseExcel(resource);
        
        // 1. 初始化错误收集器
        IEvalScope scope = XLang.newEvalScope();
        List<Map<String, Object>> runtimeErrors = new ArrayList<>();
        scope.setLocalValue("runtime_errors", runtimeErrors);

        ImportExcelParser parser = new ImportExcelParser(importModel, XLang.newCompileTool().allowUnregisteredScopeVar(true), scope);
        parser.setReturnDynamicObject(true);
        
        Object result;
        try {
            // 2. 执行解析
            result = parser.parseFromWorkbook(wk);
        } catch (NopException e) {
            // 处理非预期中断异常（如类型转换错误）
            String errorMsg = e.getDescription();
            if (errorMsg == null) errorMsg = e.getMessage();
            
            if (errorMsg != null && errorMsg.contains("单元格[")) {
                throw new NopException(ERR_IMPORT_VALIDATION_FAIL).description(errorMsg);
            }
            
            String cellPos = (String) e.getParam(ARG_CELL_POS);
            if (ApiStringHelper.isEmpty(cellPos)) cellPos = "未知";
            throw new NopException(ERR_IMPORT_VALIDATION_FAIL).description("单元格[" + cellPos + "]校验失败：" + errorMsg);
        }

        // 3. 处理收集到的运行时校验错误（多行多列汇总）
        if (!runtimeErrors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> err : runtimeErrors) {
                sb.append("单元格[").append(err.get("cellPos")).append("]校验失败：").append(err.get("msg")).append("\n");
            }
            throw new NopException(ERR_IMPORT_VALIDATION_FAIL).description(sb.toString().trim());
        }

        if (result == null) {
            return Collections.emptyList();
        }

        // 4. 转换并返回结果
        Object data = BeanTool.getProperty(result, poName);
        if (!(data instanceof List)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : (List<?>) data) {
            if (item instanceof Map) {
                list.add(new HashMap<>((Map<String, Object>) item));
            } else if (item instanceof DynamicObject) {
                list.add(new HashMap<>(((DynamicObject) item).toMap()));
            }
        }
        return list;
    }

    public static ExcelWorkbook buildExportWorkbook(PoInfo poInfo, List<?> data, IPoDictService dictService) {
        if (poInfo == null) return null;
        if (dictService != null) {
            Set<String> dictNames = collectDictNames(poInfo);
            new PoDictGenerator(dictService).generateDictFiles(dictNames);
        }

        try {
            IResource xgenResource = VirtualFileSystem.instance().getResource("/nop/excel/po-to-export.workbook.xml.xgen");
            IEvalScope evalScope = XLang.newEvalScope();
            evalScope.setLocalValue("po", poInfo);
            evalScope.setLocalValue("data", data);
            XNode workbookNode = (XNode) XLang.parseXpl(xgenResource, XLangOutputMode.node).invoke(evalScope);
            return (ExcelWorkbook) DslModelHelper.parseDslNode("/nop/schema/excel/workbook.xdef", workbookNode);
        } catch (Exception e) {
            throw NopException.adapt(e);
        }
    }

    public static File buildExportWorkbookFile(PoInfo poInfo, List<?> data, IPoDictService dictService) {
        ExcelWorkbook excelWorkbook = buildExportWorkbook(poInfo, data, dictService);
        File file = new File(System.getProperty("java.io.tmpdir"), poInfo.getName() + ".xlsx");
        ExcelHelper.saveExcel(new FileResource(file), excelWorkbook);
        return file;
    }
}
