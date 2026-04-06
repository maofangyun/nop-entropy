package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import com.seago.code.po.PropInfo;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.FileHelper;
import io.nop.core.lang.eval.EvalScopeImpl;
import io.nop.core.lang.xml.XNode;
import io.nop.core.model.object.DynamicObject;
import io.nop.core.resource.IResource;
import io.nop.core.resource.impl.FileResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.excel.imp.model.ImportModel;
import io.nop.excel.model.ExcelWorkbook;
import io.nop.ooxml.xlsx.imp.XlsxObjectLoader;
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

/**
 * 核心类：动态解析 PoInfo 模型转换为 Nop 框架原生支持的 imp.xml DSL 节点或 ExcelWorkbook。
 */
public class PoExcelHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PoExcelHelper.class);

    /**
     * 收集 PoConfig 中所有 PoInfo 属性引用的字典名称
     */
    public static Set<String> collectDictNames(PoConfig poConfig) {
        Set<String> dictNames = new HashSet<>();
        if (poConfig != null && poConfig.getPos() != null) {
            for (PoInfo po : poConfig.getPos()) {
                dictNames.addAll(collectDictNames(po));
            }
        }
        return dictNames;
    }

    /**
     * 收集单个 PoInfo 中引用的所有字典名称
     */
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

    public static List<Map<String, Object>> parseExcel(PoConfig poConfig, String poName, IResource resource) {
        return parseExcel(poConfig, poName, resource, null);
    }

    public static List<Map<String, Object>> parseExcel(PoConfig poConfig, String poName, IResource resource, IPoDictService dictService) {
        LOG.info("PoExcelHelper.parseExcel: poName={}, resource={}", poName, resource);

        // 1. 动态生成字典文件
        if (dictService != null) {
            Set<String> dictNames = collectDictNames(poConfig);
            new PoDictGenerator(dictService).generateDictFiles(dictNames);
        }

        String moduleId = poConfig.getPackageName().replace('.', '/');
        String impPath = "/" + moduleId + "/excel/" + poName + ".imp.xml";
        LOG.info("PoExcelHelper.parseExcel: loading import model from vfs path={}", impPath);
        
        ImportModel importModel = (ImportModel) ResourceComponentManager.instance().loadComponentModel(impPath);

        XlsxObjectLoader loader = new XlsxObjectLoader(importModel);
        loader.setReturnDynamicObject(true);

        Object result = loader.parseFromResource(resource, XLang.newEvalScope());
        Object data = BeanTool.getProperty(result, poName);
        if (!(data instanceof List)) {
            LOG.warn("PoExcelHelper.parseExcel: result data is not a list for poName={}", poName);
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
        LOG.info("PoExcelHelper.parseExcel: finished, result size={}", list.size());
        return list;
    }

    public static ExcelWorkbook buildExportWorkbook(PoInfo poInfo, List<?> data) {
        return buildExportWorkbook(poInfo, data, null);
    }

    /**
     * 将 PoInfo 模型转换为用于导出的 ExcelWorkbook 对象。
     */
    public static ExcelWorkbook buildExportWorkbook(PoInfo poInfo, List<?> data, IPoDictService dictService) {
        if (poInfo == null) {
            return null;
        }
        String poName = poInfo.getName();
        int dataSize = data != null ? data.size() : 0;
        LOG.info("PoExcelHelper.buildExportWorkbook: po={}, dataSize={}", poName, dataSize);

        // 1. 动态生成字典文件
        if (dictService != null) {
            Set<String> dictNames = collectDictNames(poInfo);
            new PoDictGenerator(dictService).generateDictFiles(dictNames);
        }

        try {
            IResource xgenResource = VirtualFileSystem.instance()
                    .getResource("/nop/excel/po-to-export.workbook.xml.xgen");

            EvalScopeImpl evalScope = new EvalScopeImpl();
            evalScope.setLocalValue("po", poInfo);
            evalScope.setLocalValue("data", data);

            // 生成 Workbook XML 节点
            XNode workbookNode = (XNode) XLang.parseXpl(xgenResource, XLangOutputMode.node).invoke(evalScope);
            // 解析为 ExcelWorkbook 对象
            ExcelWorkbook workbook = (ExcelWorkbook) DslModelHelper.parseDslNode("/nop/schema/excel/workbook.xdef", workbookNode);
            LOG.info("PoExcelHelper.buildExportWorkbook: finished building workbook");
            return workbook;
        } catch (Exception e) {
            LOG.error("PoExcelHelper.buildExportWorkbook: error building export workbook", e);
            throw NopException.adapt(e);
        }
    }

    public static File buildExportWorkbookFile(PoInfo poInfo, List<?> data) {
        return buildExportWorkbookFile(poInfo, data, null);
    }

    public static File buildExportWorkbookFile(PoInfo poInfo, List<?> data, IPoDictService dictService) {
        ExcelWorkbook excelWorkbook = buildExportWorkbook(poInfo, data, dictService);
        File file = new File(System.getProperty("java.io.tmpdir"), "test.xlsx");
        LOG.info("PoExcelHelper.buildExportWorkbookFile: saving excel to {}", file.getAbsolutePath());
        ExcelHelper.saveExcel(new FileResource(file), excelWorkbook);
        return file;
    }

}
