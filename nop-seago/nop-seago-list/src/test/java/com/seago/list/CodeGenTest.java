package com.seago.list;

import io.nop.api.core.config.AppConfig;
import io.nop.codegen.XCodeGenerator;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.lang.eval.EvalScopeImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.nop.core.CoreConfigs.CFG_INCLUDE_CURRENT_PROJECT_RESOURCES;

public class CodeGenTest {

    @BeforeAll
    public static void init(){
        AppConfig.getConfigProvider().updateConfigValue(CFG_INCLUDE_CURRENT_PROJECT_RESOURCES,Boolean.FALSE);
        CoreInitialization.initialize();
    }

    @AfterAll
    public static void destroy(){
        CoreInitialization.destroy();
    }

    @Test
    public void xdefTest(){
        XCodeGenerator generator = new XCodeGenerator("/seago/templates", "./");
        generator.renderModel("/nop/schema/seago/po.xdef","/nop/templates/xdsl", "/", new EvalScopeImpl());
    }

    @Test
    public void codeGenTest(){
        XCodeGenerator generator = new XCodeGenerator("/seago/templates", "D:\\Learn\\temp");
        EvalScopeImpl evalScope = new EvalScopeImpl();
        evalScope.setLocalValue("appName","seago-list");
        generator.renderModel("/seago/po/list.po.xml","/seago/templates/orm/po", "/", evalScope);
        generator.renderModel("/seago/po/list.po.xml","/seago/templates/orm/content", "/", evalScope);
    }

}
