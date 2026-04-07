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
        // 直接定位到模块的 src/test/resources/_vfs 目录
        File moduleDir = MavenDirHelper.projectDir(PoExcelCodegenTest.class);
        File vfsDir = new File(moduleDir, "src/test/resources/_vfs");

        // 3. 执行生成
        IEvalScope scope = XLang.newEvalScope();
        scope.setLocalValue("codeGenModel", poConfig);
        // 这里 appName 设置为空字符串，配合模板结构 {appName}/src/main/resources/_vfs/excel
        // 最终会生成到 vfsDir/src/main/resources/_vfs/excel 目录下
        scope.setLocalValue("appName", "");

        // 执行生成
        XCodeGenerator generator = new XCodeGenerator("/seago/templates/excel", vfsDir.getCanonicalPath());
        generator.execute("/", null, scope);

        // 4. 验证生成的文件是否存在
        // 模板路径是 {appName}/src/main/resources/_vfs/excel/
        File baseImpFile = new File(vfsDir, "src/main/resources/_vfs/excel/_gen/_TestEntity.imp.xml");
        File extImpFile = new File(vfsDir, "src/main/resources/_vfs/excel/TestEntity.imp.xml");

        assertTrue(baseImpFile.exists(), "Base imp file should be generated: " + baseImpFile.getAbsolutePath());
        assertTrue(extImpFile.exists(), "Extension imp file should be generated: " + extImpFile.getAbsolutePath());

        // 5. 验证内容
        String baseContent = io.nop.commons.util.FileHelper.readText(baseImpFile, "UTF-8");
        assertTrue(baseContent.contains("userName"), "Should contain userName field");
        
        System.out.println("Codegen test passed. Files generated at: " + vfsDir.getAbsolutePath());
    }
}
