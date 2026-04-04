package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import com.seago.code.po.PropInfo;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.FileHelper;
import io.nop.core.lang.eval.EvalScopeImpl;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.IResource;
import io.nop.core.resource.impl.FileResource;
import io.nop.core.resource.VirtualFileSystem;
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
import java.util.List;
import java.util.Map;

/**
 * 核心类：动态解析 PoInfo 模型转换为 Nop 框架原生支持的 imp.xml DSL 节点或 ExcelWorkbook。
 */
public class PoExcelHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PoExcelHelper.class);

    public static List<Map<String, Object>> parseExcel(PoConfig poConfig, String poName, IResource resource) {
        XNode impNode = buildImportModelNode(poConfig);

        ImportModel importModel = (ImportModel) DslModelHelper.parseDslNode("/nop/schema/excel/imp.xdef", impNode);
        io.nop.ooxml.xlsx.imp.XlsxObjectLoader loader = new io.nop.ooxml.xlsx.imp.XlsxObjectLoader(importModel);
        loader.setReturnDynamicObject(true);

        Object result = loader.parseFromResource(resource, XLang.newEvalScope());
        Object data = io.nop.core.reflect.bean.BeanTool.getProperty(result, poName);
        if (!(data instanceof List)) return Collections.emptyList();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : (List<?>) data) {
            if (item instanceof Map) {
                list.add(new HashMap<>((Map<String, Object>) item));
            } else if (item instanceof io.nop.core.model.object.DynamicObject) {
                list.add(new HashMap<>(((io.nop.core.model.object.DynamicObject) item).toMap()));
            }
        }
        return list;
    }

    private static String toStdDomain(String type) {
        if (type == null) return "string";
        switch (type) {
            case "int": case "integer": return "int";
            case "long": return "long";
            case "double": case "float": case "decimal": case "number": return "double";
            case "boolean": return "boolean";
            case "date": return "date";
            case "datetime": case "timestamp": return "datetime";
            default: return "string";
        }
    }

    public static XNode buildImportModelNode(PoConfig poConfig) {
        try {
            IResource xgenResource = VirtualFileSystem.instance().getResource("/nop/excel/po-to-imp.imp.xml.xgen");
            EvalScopeImpl evalScope = new EvalScopeImpl();
            evalScope.setLocalValue("poConfig", poConfig);
            return (XNode) XLang.parseXpl(xgenResource, XLangOutputMode.node).invoke(evalScope);
        } catch (Exception e) {
            throw NopException.adapt(e);
        }
    }

    public static File buildImportModelFile(PoConfig poConfig) {
        XNode node = buildImportModelNode(poConfig);
        File file = new File(System.getProperty("java.io.tmpdir"), "imp.xml");
        FileHelper.writeText(file, node.xml(), "UTF-8");
        return file;
    }

    /**
     * 将 PoInfo 模型转换为用于导出的 ExcelWorkbook 对象。
     */
    public static ExcelWorkbook buildExportWorkbook(PoInfo poInfo, List<?> data) {
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

            return workbook;
        } catch (Exception e) {
            throw NopException.adapt(e);
        }
    }

    public static File buildExportWorkbookFile(PoInfo poInfo, List<?> data) {
        ExcelWorkbook excelWorkbook = buildExportWorkbook(poInfo, data);
        File file = new File(System.getProperty("java.io.tmpdir"), "test.xlsx");
        ExcelHelper.saveExcel(new FileResource(file), excelWorkbook);
        return file;
    }

    public static String getColLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        int n = colIndex;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        }
        return sb.toString();
    }

    public static Object getPropertyValue(Object row, String propName) {
        if (row == null) return null;
        return BeanTool.getProperty(row, propName);
    }

    public static String getDictLabel(String dictName, Object value) {
        if (value == null) return null;
        io.nop.api.core.beans.DictBean dict = io.nop.core.dict.DictProvider.instance().getDict(null, dictName, null, null);
        if (dict == null) return String.valueOf(value);
        String label = dict.getLabelByValue(value);
        return label != null ? label : String.valueOf(value);
    }
}
