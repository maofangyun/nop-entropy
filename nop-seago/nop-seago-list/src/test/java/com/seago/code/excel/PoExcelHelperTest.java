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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.core.CoreConfigs.CFG_DICT_RETURN_NORMALIZED_LABEL;
import static io.nop.core.CoreConfigs.CFG_INCLUDE_CURRENT_PROJECT_RESOURCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PoExcelHelperTest {

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
    public void testBuildExportModelFile() {
        PoConfig poConfig = buildTestPoConfig();
        PoInfo poInfo = poConfig.getPos().get(0);

        // 准备一些导出数据
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", "1001");
        row.put("userName", "ZhangSan");
        row.put("gender", "1");
        data.add(row);

        ExcelWorkbook excelWorkbook = PoExcelHelper.buildExportWorkbook(poInfo, data);
        IResource resource = ResourceHelper.resolve("/seago/po/test.xlsx");
        ExcelHelper.saveExcel(resource, excelWorkbook);
        
        assertTrue(resource.exists());
    }

    @Test
    public void testParseExcel() {
        PoConfig poConfig = buildTestPoConfig();
        IResource resource = ResourceHelper.resolve("/seago/po/test-imp.xlsx");

        // 不再支持 dictService 处理字典转换
        List<Map<String, Object>> result = PoExcelHelper.parseExcel(poConfig, "TestEntity", resource);

        assertEquals(3, result.size());
        
        Map<String, Object> firstRow = result.get(0);
        // 验证关键字段解析
        assertNotNull(firstRow.get("id"));
    }


    @Test
    public void testParseExcelValidationError() {
        PoConfig poConfig = new PoConfig();
        PoInfo po = new PoInfo();
        po.setName("User");
        po.setComment("用户列表");

        PropInfo p1 = new PropInfo();
        p1.setName("age");
        p1.setExcelHeader("年龄");
        p1.setType("int");
        p1.setMin(18.0);

        po.setProps(Arrays.asList(p1));
        poConfig.setPos(Arrays.asList(po));

        ExcelWorkbook wk = new ExcelWorkbook();
        ExcelSheet sheet = new ExcelSheet();
        sheet.setName("用户列表");
        wk.addSheet(sheet);
        ExcelTable table = sheet.getTable();
        setCell(table, 0, 0, "年龄");
        setCell(table, 1, 0, "15"); // 这里的 15 小于 min(18)

        IResource resource = ResourceHelper.getTempResource();
        ExcelHelper.saveExcel(resource, wk);

        try {
            PoExcelHelper.parseExcel(poConfig, "User", resource);
            fail("Should throw exception");
        } catch (NopException e) {
            // 验证报错信息或参数包含行列号
            boolean hasA2 = e.getMessage().contains("A2") || 
                            (e.getParams() != null && "A2".equals(e.getParams().get("cellPos")));
            if(!hasA2){
                System.out.println("Error: " + e.getErrorCode() + ", Message: " + e.getMessage() + ", Params: " + e.getParams());
            }
            assertTrue(hasA2);
        }
    }

    private void setCell(ExcelTable table, int row, int col, Object value) {
        ExcelCell cell = new ExcelCell();
        cell.setValue(value);
        table.setCell(row, col, cell);
    }
}
