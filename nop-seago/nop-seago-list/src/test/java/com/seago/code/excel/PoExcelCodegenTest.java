package com.seago.code.excel;

import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.MavenDirHelper;
import io.nop.codegen.XCodeGenerator;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.resource.component.ResourceComponentManager;
import io.nop.core.unittest.BaseTestCase;
import io.nop.xlang.api.XLang;
import io.nop.core.lang.eval.IEvalScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.nop.core.CoreConfigs.CFG_INCLUDE_CURRENT_PROJECT_RESOURCES;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PoExcelCodegenTest extends BaseTestCase {

    @BeforeAll
    public static void init() {
        AppConfig.getConfigProvider().updateConfigValue(CFG_INCLUDE_CURRENT_PROJECT_RESOURCES, Boolean.TRUE);
        CoreInitialization.initialize();
    }

    @AfterAll
    public static void destroy() {
        CoreInitialization.destroy();
    }

    @Test
    public void testCodegen() throws Exception {
        // 1. 加载模型
        Object poConfig = ResourceComponentManager.instance().loadComponentModel("/seago/po/test-po-config.po.xml");

        // 2. 指定输出目录
        // 修正：将输出目录上移，以抵消模板中多余的 src/main/resources/_vfs 层级
        File moduleDir = MavenDirHelper.projectDir(PoExcelCodegenTest.class);
        File vfsDir = new File(moduleDir, "src/test/resources/_vfs");
        // 如果模板生成器会自动补全 src/main/resources/_vfs，我们指向其父目录
        File targetDir = moduleDir.getCanonicalFile(); // 直接指向模块根目录

        // 3. 执行生成
        IEvalScope scope = XLang.newEvalScope();
        scope.setLocalValue("codeGenModel", poConfig);
        scope.setLocalValue("appName", "src/test/resources/_vfs");

        // 执行生成
        XCodeGenerator generator = new XCodeGenerator("/seago/templates/excel", targetDir.getCanonicalPath());
        generator.execute("/", null, scope);

        // 4. 验证生成的文件是否存在
        // 目标：src/test/resources/_vfs/excel/_gen/_TestEntity.imp.xml
        File baseImpFile = new File(vfsDir, "excel/_gen/_TestEntity.imp.xml");
        File extImpFile = new File(vfsDir, "excel/TestEntity.imp.xml");

        assertTrue(baseImpFile.exists(), "Base imp file should be generated: " + baseImpFile.getAbsolutePath());
        assertTrue(extImpFile.exists(), "Extension imp file should be generated: " + extImpFile.getAbsolutePath());

        // 5. 验证内容
        String baseContent = io.nop.commons.util.FileHelper.readText(baseImpFile, "UTF-8");
        assertTrue(baseContent.contains("userName"), "Should contain userName field");
        
        System.out.println("Codegen test passed. Files generated at: " + vfsDir.getAbsolutePath());
    }
}
