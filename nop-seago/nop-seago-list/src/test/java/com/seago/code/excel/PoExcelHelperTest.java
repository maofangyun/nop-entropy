package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import com.seago.code.po.PropInfo;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.excel.model.ExcelCell;
import io.nop.excel.model.ExcelSheet;
import io.nop.excel.model.ExcelTable;
import io.nop.excel.model.ExcelWorkbook;
import io.nop.ooxml.xlsx.util.ExcelHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.core.CoreConfigs.CFG_DICT_RETURN_NORMALIZED_LABEL;
import static io.nop.core.CoreConfigs.CFG_INCLUDE_CURRENT_PROJECT_RESOURCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PoExcelHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(PoExcelHelperTest.class);

    @BeforeAll
    public static void init() {
        AppConfig.getConfigProvider().updateConfigValue(CFG_INCLUDE_CURRENT_PROJECT_RESOURCES, Boolean.FALSE);
        AppConfig.getConfigProvider().updateConfigValue(CFG_DICT_RETURN_NORMALIZED_LABEL, Boolean.FALSE);
        CoreInitialization.initialize();
    }

    @AfterAll
    public static void destroy() {
        CoreInitialization.destroy();
    }

    private PoConfig buildTestPoConfig() {
        return (PoConfig) ResourceComponentManager.instance().loadComponentModel("/seago/po/test-po-config.po.xml");
    }

    @Test
    public void testBuildExportWithDynamicDict() {
        PoConfig poConfig = buildTestPoConfig();
        PoInfo poInfo = poConfig.getPos().get(0);

        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", "1001");
        row.put("userName", "张三");
        row.put("age", 25);
        row.put("departmentId","PDM");
        row.put("gender", "1");
        data.add(row);

        // 模拟字典服务
        IPoDictService mockDictService = dictName -> {
            if ("sys/gender_dict".equals(dictName)) {
                List<PoDictOption> options = new ArrayList<>();
                options.add(new PoDictOption("1", "男"));
                options.add(new PoDictOption("2", "女"));
                return options;
            }
            return Collections.emptyList();
        };

        // 收集并验证字典名
        Set<String> dictNames = PoExcelHelper.collectDictNames(poInfo);
        assertTrue(dictNames.contains("sys/gender_dict"));

        // 执行工作簿构建（会触发字典文件生成逻辑）
        File file = PoExcelHelper.buildExportWorkbookFile(poInfo, data, mockDictService);
        assertNotNull(file);
        
        LOG.info("PoExcelHelperTest: Dynamic dict test finished successfully");
    }

    @Test
    public void testParseExcel() {
        PoConfig poConfig = buildTestPoConfig();
        PoInfo poInfo = poConfig.getPos().get(0);
        IResource resource = ResourceHelper.resolve("/seago/po/test-import.xlsx");

        // 不再支持 dictService 处理字典转换
        List<Map<String, Object>> result = PoExcelHelper.parseExcel(poInfo, resource);

        assertNotNull(result, "Result list should not be null");
        assertEquals(3,result.size());
        
        Map<String, Object> firstRow = result.get(0);
        // 验证关键字段解析
        assertNotNull(firstRow.get("id"));
    }


    @Test
    public void testParseExcelValidationError() {
        PoConfig poConfig = buildTestPoConfig();
        PoInfo poInfo = poConfig.getPos().get(0);
        IResource resource = ResourceHelper.resolve("/seago/po/test-check.xlsx");

        // 执行解析并验证校验异常
        try {
            PoExcelHelper.parseExcel(poInfo, resource);
            fail("Should throw validation exception for invalid data");
        } catch (NopException e) {
            String msg = e.getMessage();
            Map<String, Object> params = e.getParams();
            System.out.println("Expected Error: " + e.getErrorCode() + ", Params: " + params + ", Message: " + msg);
            
            // 1. 验证错误码是否包含预期前缀
            assertTrue(e.getErrorCode().contains("excel") || e.getErrorCode().contains("type-conversion"), 
                       "Unexpected error code: " + e.getErrorCode());

            // 2. 根据 test-check.xlsx 的实际内容验证错误位置
            // 观测到实际报错单元格为 D4
            boolean hasExpectedPos = (params != null && "D4".equals(params.get("cellPos")))
                              || msg.contains("D4");
            
            assertTrue(hasExpectedPos, "Expected error at cell D4 in params " + params + " or message: " + msg);
        }
    }

}
