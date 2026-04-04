package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import com.seago.code.po.PropInfo;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.FileHelper;
import io.nop.core.lang.eval.EvalScopeImpl;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.excel.imp.model.ImportModel;
import io.nop.excel.model.ExcelCell;
import io.nop.excel.model.ExcelDataValidation;
import io.nop.excel.model.ExcelSheet;
import io.nop.excel.model.ExcelTable;
import io.nop.excel.model.ExcelWorkbook;
import io.nop.excel.model.constants.ExcelDataValidationType;
import io.nop.core.model.table.ICell;
import io.nop.ooxml.xlsx.imp.ImportModelToExportModel;
import io.nop.ooxml.xlsx.util.ExcelHelper;
import io.nop.xlang.api.XLang;
import io.nop.xlang.ast.XLangOutputMode;
import io.nop.xlang.xdsl.DslModelHelper;
import io.nop.xlang.xdsl.json.DslXNodeToJsonTransformer;
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
 * <p>
 * 导入：生成符合 imp.xdef 规范的 XNode，可通过 DslModelHelper 加载为 ImportModel 后使用 XlsxObjectLoader 执行导入。
 * 导出：生成包含多行合并表头、数据验证（下拉框）和数据行的 ExcelWorkbook。
 */
public class PoExcelHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PoExcelHelper.class);

    /**
     * 根据类型字符串映射到 Nop schema 中的 stdDomain 值。
     */
    private static String toStdDomain(String type) {
        if (type == null)
            return "string";
        switch (type) {
            case "int":
            case "integer":
                return "int";
            case "long":
                return "long";
            case "double":
            case "float":
            case "decimal":
            case "number":
                return "double";
            case "boolean":
                return "boolean";
            case "date":
                return "date";
            case "datetime":
            case "timestamp":
                return "datetime";
            default:
                return "string";
        }
    }

    /**
     * 使用 xgen 方式动态生成符合 imp.xdef 规范的导入模型 XNode。
     * <p>
     * 参考 orm-gen.xlib 的实现方式,通过 xgen 模板将 po.xml 转换为 imp.xml
     * <p>
     * 生成后可通过如下方式使用：
     * <pre>
     * ImportModel importModel = DslModelHelper.loadDslModel(impNode);
     * Object result = new XlsxObjectLoader(importModel).parseFromResource(resource);
     * </pre>
     *
     * @param poConfig PO 配置对象
     * @return 符合 imp.xdef 规范的 XNode
     */
    public static XNode buildImportModelNode(PoConfig poConfig) {
        try {
            // 使用 xgen 模板生成 imp.xml
            IResource xgenResource = VirtualFileSystem.instance()
                    .getResource("/nop/excel/po-to-imp.imp.xml.xgen");
            
            // 准备模板变量
            EvalScopeImpl evalScope = new EvalScopeImpl();
            evalScope.setLocalValue("poConfig", poConfig);
            
            // 执行 xgen 模板
            return (XNode) XLang.parseXpl(xgenResource, XLangOutputMode.node).invoke(evalScope);
            
        } catch (Exception e) {
            throw NopException.adapt(e);
        }
    }


    public static File buildImportModelFile(PoConfig poConfig) {
        XNode impNode = buildImportModelNode(poConfig);
        ImportModel model = (ImportModel) DslModelHelper.parseDslNode("/nop/schema/excel/imp.xdef", impNode);
        ImportModelToExportModel transform = new ImportModelToExportModel();
        ExcelWorkbook wk = transform.build(model);
        IResource resource = ResourceHelper.getTempResource();
        ExcelHelper.saveExcel(resource, wk);
        return resource.toFile();
    }


    /**
     * 构建用于导出的 ExcelWorkbook。
     *
     * @param poInfo     模型定义
     * @param data        导出数据列表（每条数据为 Map 或 JavaBean）
     * @param dictOptions 字典选项数据，key 为 dict 名称，value 为选项列表
     * @param listOptions 接口选项数据，key 为 listProvider 名称，value 为选项列表
     */
    public static ExcelWorkbook buildExportWorkbook(PoInfo poInfo, List<?> data,
                                                    Map<String, List<String>> dictOptions,
                                                    Map<String, List<String>> listOptions) {
        // 使用 xgen 模板生成 imp.xml
        IResource xgenResource = VirtualFileSystem.instance()
                .getResource("/nop/excel/po-to-export.workbook.xml.xgen");

        // 准备模板变量
        EvalScopeImpl evalScope = new EvalScopeImpl();
        evalScope.setLocalValue("po", poInfo);
        evalScope.setLocalValue("data", data);
        evalScope.setLocalValue("dictOptions", dictOptions);
        evalScope.setLocalValue("listOptions", listOptions);

        // 执行 xgen 模板
        XNode xNode = (XNode) XLang.parseXpl(xgenResource, XLangOutputMode.node).invoke(evalScope);

        ExcelWorkbook excelWorkbook = (ExcelWorkbook) DslModelHelper.parseDslNode("/nop/schema/excel/workbook.xdef", xNode);
        excelWorkbook.init();
        return excelWorkbook;
    }


    @SuppressWarnings("unchecked")
    static Object getPropertyValue(Object obj, String propName) {
        if (obj == null)
            return null;
        if (obj instanceof Map)
            return ((Map<String, Object>) obj).get(propName);
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(propName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            try {
                String getterName = "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
                java.lang.reflect.Method method = obj.getClass().getMethod(getterName);
                return method.invoke(obj);
            } catch (Exception ex) {
                return null;
            }
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 将列索引转换为 Excel 列名字母（A, B, ..., Z, AA, AB, ...）
     */
    static String getColLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        int n = colIndex;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        }
        return sb.toString();
    }

}
