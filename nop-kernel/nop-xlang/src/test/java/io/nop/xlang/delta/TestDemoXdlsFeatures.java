package io.nop.xlang.delta;

import io.nop.core.initialize.CoreInitialization;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.core.resource.IResource;
import io.nop.xlang.xdsl.DslNodeLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDemoXdlsFeatures {
    @BeforeAll
    public static void init() {
        CoreInitialization.initialize();
    }

    @AfterAll
    public static void destroy() {
        CoreInitialization.destroy();
    }

    @Test
    public void testXdslFeatures() {
        // 从虚拟文件系统加载
        IResource resource = VirtualFileSystem.instance().getResource("/demo/xdsl/feature.xml");
        
        // 使用 DslNodeLoader 进行加载，此过程会自动执行合并 (x:extends, x:gen-extends, override等)
        XNode node = DslNodeLoader.INSTANCE.loadFromResource(resource).getNode();
        
        System.out.println("================ 合并推导后的最终 XML 结果 ================");
        node.dump(); // 打印到系统控制台
    }
}
