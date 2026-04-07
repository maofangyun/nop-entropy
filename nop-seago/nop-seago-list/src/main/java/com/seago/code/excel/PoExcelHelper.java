package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import com.seago.code.po.PropInfo;
import io.nop.api.core.exceptions.NopException;
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
 * 核心辅助类：负责将 PoInfo 业务模型与 Nop 框架的 Excel 导入导出功能进行对接。
 * <p>
 * 主要功能：
 * 1. 动态收集模型中引用的字典。
 * 2. 基于 .imp.xml 导入模型，将 Excel 文件解析为 Map 列表。
 * 3. 基于 .workbook.xml.xgen 模板，将数据动态生成为 ExcelWorkbook 对象。
 */
public class PoExcelHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PoExcelHelper.class);

    /**
     * 从完整的 PoConfig 配置中收集所有实体（PoInfo）属性所引用的字典名称。
     * 用于批量生成或校验字典文件。
     *
     * @param poConfig 包含多个实体定义的配置对象
     * @return 字典名称集合
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
     * 收集单个实体（PoInfo）中所有属性引用的字典名称。
     *
     * @param poInfo 实体定义模型
     * @return 字典名称集合
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

    /**
     * 解析 Excel 文件的便捷方法（不带字典服务）。
     */
    public static List<Map<String, Object>> parseExcel(PoConfig poConfig, String poName, IResource resource) {
        return parseExcel(poConfig, poName, resource, null);
    }

    /**
     * 根据指定的实体定义，解析输入的 Excel 资源文件。
     * <p>
     * 流程：
     * 1. 如果提供了字典服务，则动态生成字典 VFS 文件。
     * 2. 根据包名和实体名定位对应的 .imp.xml 导入模型文件。
     * 3. 使用 XlsxObjectLoader 加载并解析 Excel 内容。
     * 4. 将解析出的 DynamicObject 或其他对象转换为标准的 Map 列表返回。
     *
     * @param poConfig    实体所属的配置环境
     * @param poName      要解析的实体名称（对应 Excel 中的 Sheet 或字段名）
     * @param resource    Excel 文件的资源引用
     * @param dictService 字典服务，用于动态解析字典项
     * @return 解析后的数据列表，每一项为一个 Map
     */
    public static List<Map<String, Object>> parseExcel(PoConfig poConfig, String poName, IResource resource, IPoDictService dictService) {
        LOG.info("PoExcelHelper.parseExcel: poName={}, resource={}", poName, resource);

        // 1. 动态生成字典文件：确保 Excel 解析时能正确识别字典 Label
        if (dictService != null) {
            Set<String> dictNames = collectDictNames(poConfig);
            new PoDictGenerator(dictService).generateDictFiles(dictNames);
        }

        // 2. 构造导入模型路径
        String impPath = "/excel/" + poName + ".imp.xml";
        LOG.info("PoExcelHelper.parseExcel: loading import model from vfs path={}", impPath);
        
        // 加载 Nop 导入模型
        ImportModel importModel = (ImportModel) ResourceComponentManager.instance().loadComponentModel(impPath);

        // 3. 执行解析
        XlsxObjectLoader loader = new XlsxObjectLoader(importModel);
        loader.setReturnDynamicObject(true); // 返回动态对象以获取更好的兼容性

        Object result = loader.parseFromResource(resource, XLang.newEvalScope());
        
        // 4. 提取并转换结果数据
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

    /**
     * 将实体数据导出为 ExcelWorkbook 的便捷方法（不带字典服务）。
     */
    public static ExcelWorkbook buildExportWorkbook(PoInfo poInfo, List<?> data) {
        return buildExportWorkbook(poInfo, data, null);
    }

    /**
     * 核心导出逻辑：将 PoInfo 结构和业务数据合并，动态生成 Excel 导出模型。
     * <p>
     * 流程：
     * 1. 动态生成所需的字典文件。
     * 2. 获取内置的 .workbook.xml.xgen 模板。
     * 3. 在 XLang EvalScope 中注入 po 和 data 变量，执行模板生成 XML 节点。
     * 4. 将 XML 节点解析为 Nop 标准的 ExcelWorkbook 对象。
     *
     * @param poInfo      实体元数据定义
     * @param data        要导出的业务数据列表
     * @param dictService 字典服务，用于转换导出的字典值
     * @return 内存中的 ExcelWorkbook 对象
     */
    public static ExcelWorkbook buildExportWorkbook(PoInfo poInfo, List<?> data, IPoDictService dictService) {
        if (poInfo == null) {
            return null;
        }
        String poName = poInfo.getName();
        int dataSize = data != null ? data.size() : 0;
        LOG.info("PoExcelHelper.buildExportWorkbook: po={}, dataSize={}", poName, dataSize);

        // 1. 准备字典环境
        if (dictService != null) {
            Set<String> dictNames = collectDictNames(poInfo);
            new PoDictGenerator(dictService).generateDictFiles(dictNames);
        }

        try {
            // 2. 加载 XGen 模板：该模板负责根据 poInfo 结构生成 Sheet 列头等
            IResource xgenResource = VirtualFileSystem.instance().getResource("/nop/excel/po-to-export.workbook.xml.xgen");

            EvalScopeImpl evalScope = new EvalScopeImpl();
            evalScope.setLocalValue("po", poInfo);
            evalScope.setLocalValue("data", data);

            // 3. 执行模板逻辑，生成 Workbook 的 XML 描述节点
            XNode workbookNode = (XNode) XLang.parseXpl(xgenResource, XLangOutputMode.node).invoke(evalScope);
            
            // 4. 将 XML 节点映射为 ExcelWorkbook 模型对象
            ExcelWorkbook workbook = (ExcelWorkbook) DslModelHelper.parseDslNode("/nop/schema/excel/workbook.xdef", workbookNode);
            LOG.info("PoExcelHelper.buildExportWorkbook: finished building workbook");
            return workbook;
        } catch (Exception e) {
            LOG.error("PoExcelHelper.buildExportWorkbook: error building export workbook", e);
            throw NopException.adapt(e);
        }
    }

    /**
     * 导出为临时文件的便捷方法（不带字典服务）。
     */
    public static File buildExportWorkbookFile(PoInfo poInfo, List<?> data) {
        return buildExportWorkbookFile(poInfo, data, null);
    }

    /**
     * 将数据构建为 Excel 文件并保存到本地临时目录。
     *
     * @param poInfo      实体定义
     * @param data        数据列表
     * @param dictService 字典服务
     * @return 生成的本地 File 对象
     */
    public static File buildExportWorkbookFile(PoInfo poInfo, List<?> data, IPoDictService dictService) {
        // 先构建内存模型
        ExcelWorkbook excelWorkbook = buildExportWorkbook(poInfo, data, dictService);
        
        // 创建临时文件
        File file = new File(System.getProperty("java.io.tmpdir"), poInfo.getName() + ".xlsx");
        LOG.info("PoExcelHelper.buildExportWorkbookFile: saving excel to {}", file.getAbsolutePath());
        
        // 使用 Nop ExcelHelper 将模型持久化为 .xlsx 文件
        ExcelHelper.saveExcel(new FileResource(file), excelWorkbook);
        return file;
    }

}
