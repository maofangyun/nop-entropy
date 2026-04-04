package com.seago.code.excel;

import com.seago.code.po.PoConfig;
import com.seago.code.po.PoInfo;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.FileHelper;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.excel.imp.model.ImportModel;
import io.nop.excel.model.ExcelWorkbook;
import io.nop.excel.model.ExcelDataValidation;
import io.nop.excel.model.ExcelSheet;
import io.nop.excel.model.ExcelTable;
import io.nop.excel.model.ExcelCell;
import io.nop.core.model.table.ICell;
import io.nop.ooxml.xlsx.imp.ImportModelToExportModel;
import io.nop.ooxml.xlsx.util.ExcelHelper;
import io.nop.xlang.xdsl.DslModelHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static io.nop.core.CoreConfigs.CFG_INCLUDE_CURRENT_PROJECT_RESOURCES;
import static org.junit.jupiter.api.Assertions.*;

public class PoExcelHelperTest {

    @BeforeAll
    public static void init() {
        AppConfig.getConfigProvider().updateConfigValue(CFG_INCLUDE_CURRENT_PROJECT_RESOURCES, Boolean.FALSE);
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
    public void testBuildImportModelFile() {
        PoConfig poConfig = buildTestPoConfig();
        File file = PoExcelHelper.buildImportModelFile(poConfig);
        Assertions.assertTrue(file.exists());
    }

    @Test
    public void testBuildExportModelFile() {
        PoConfig poConfig = buildTestPoConfig();
        PoInfo poInfo = poConfig.getPos().get(0);
        ExcelWorkbook excelWorkbook = PoExcelHelper.buildExportWorkbook(poInfo, null, null, null);
        IResource resource = ResourceHelper.resolve("/seago/po/test.xlsx");
        ExcelHelper.saveExcel(resource, excelWorkbook);
    }


}
