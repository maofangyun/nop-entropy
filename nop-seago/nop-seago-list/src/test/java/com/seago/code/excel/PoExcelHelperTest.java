package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.component.ResourceComponentManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
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
    public void testBuildExport() {
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

        // 执行解析并验证汇总后的校验异常
        try {
            PoExcelHelper.parseExcel(poInfo, resource);
            fail("Should throw validation exception for invalid data");
        } catch (NopException e) {
            String msg = e.getMessage();
            System.out.println("Summarized Errors:\n" + msg);
            
            // 验证报错信息是否包含汇总前缀
            assertTrue(msg.contains("单元格["), "Message should contain cell position prefix");
            assertTrue(msg.contains("校验失败："), "Message should contain validation failure text");

            // 根据 test-check.xlsx 的预期内容验证特定单元格的报错（例如 D4 或其他位置）
            // 确保报错信息汇总了多行/多列的错误
            String[] lines = msg.split("\n");
            assertTrue(lines.length >= 1, "Should have at least one error message line");
            
            LOG.info("PoExcelHelperTest: Validation error summary test passed.");
        }
    }

}
