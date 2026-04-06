package com.seago.code.excel;

import io.nop.api.core.config.AppConfig;
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
    public void testCodegen() {
        // 1. 加载模型
        Object poConfig = ResourceComponentManager.instance().loadComponentModel("/seago/po/test-codegen.po.xml");

        // 2. 指定输出目录
        File outDir = new File("target/codegen-test");

        // 3. 执行生成
        IEvalScope scope = XLang.newEvalScope();
        // 重要：这里的变量名必须是 codeGenModel，因为 @init.xrun 依赖它
        scope.setLocalValue("codeGenModel", poConfig);
        scope.setLocalValue("appName", "test-app");

        // 执行生成
        XCodeGenerator generator = new XCodeGenerator("/seago/templates/excel", outDir.getAbsolutePath());
        generator.execute("/", null, scope);

        // 4. 验证生成的文件是否存在
        File baseImpFile = new File(outDir, "test-app/src/main/resources/_vfs/seago/test/excel/_Member.imp.xml");
        File extImpFile = new File(outDir, "test-app/src/main/resources/_vfs/seago/test/excel/Member.imp.xml");

        assertTrue(baseImpFile.exists(), "Base imp file should be generated: " + baseImpFile.getAbsolutePath());
        assertTrue(extImpFile.exists(), "Extension imp file should be generated: " + extImpFile.getAbsolutePath());

        // 5. 验证内容
        String baseContent = io.nop.commons.util.FileHelper.readText(baseImpFile, "UTF-8");
        assertTrue(baseContent.contains("name=\"age\""), "Should contain age field");
        
        String extContent = io.nop.commons.util.FileHelper.readText(extImpFile, "UTF-8");
        assertTrue(extContent.contains("x:extends=\"_Member.imp.xml\""), "Should extend base file");
        
        System.out.println("Codegen test passed. Files generated at: " + outDir.getAbsolutePath());
    }
}
