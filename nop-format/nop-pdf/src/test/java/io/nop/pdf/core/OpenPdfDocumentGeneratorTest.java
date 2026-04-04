package io.nop.pdf.core;

import com.lowagie.text.DocumentException;
import io.nop.core.initialize.CoreInitialization;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.impl.ClassPathResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ResourceUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.nop.core.unittest.BaseTestCase.forceStackTrace;


class OpenPdfDocumentGeneratorTest {

    @BeforeAll
    public static void init() {
        forceStackTrace();
        CoreInitialization.initialize();
    }

    @AfterAll
    public static void destroy() {
        CoreInitialization.destroy();
    }

    @Test
    public void testPdf() throws IOException, DocumentException {
        OpenPdfDocumentGenerator generator = new OpenPdfDocumentGenerator();

        // 准备模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "销售报告");
        variables.put("author", "销售部");
        variables.put("date", "2026-03-16");
        variables.put("department", "销售部门");
        variables.put("quarter", "Q1");
        // 生成 PDF
        String pdfXmlPath = "/config/pdf/sales-report-custom-delta.xml";
        String outputPath = "output/sales-report.pdf";
        // 确保输出目录存在
        new java.io.File("output").mkdirs();
        generator.generatePdfFromXml(pdfXmlPath, outputPath, variables);
        System.out.println("✓ PDF 已生成: " + outputPath);
    }

    /**
     * 测试: 运行时执行 XGen 模板生成 Delta 文件,然后与基础PDF模板合并
     *
     * 工作流程:
     * 1. 运行时: OpenPdfDocumentGenerator 检测到 .xgen 后缀
     * 2. 运行时: 使用 XLang.parseXpl() 执行 XGen 模板
     * 3. 运行时: 生成的 XML 使用 x:extends 与 sales-report-base.xml 合并
     * 4. 运行时: 合并后的配置用于生成 PDF
     *
     * 配置文件路径说明:
     * - sales-data-delta.xml.xgen: XGen模板(使用<c:script>、<c:for>等标签)
     * - sales-report-base.xml: 基础PDF模板
     *
     * @throws IOException
     * @throws DocumentException
     */
    @Test
    public void testRuntimeXGenExecution() throws IOException, DocumentException {
        OpenPdfDocumentGenerator generator = new OpenPdfDocumentGenerator();

        // 准备变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "销售业绩报告");
        variables.put("author", "销售部");
        variables.put("date", "2026-03-17");
        variables.put("department", "全国销售部");
        variables.put("quarter", "Q1 2026");

        java.util.List<Map<String, Object>> salesData = java.util.Arrays.asList(
            Map.of("category", "华北区", "target", "3,000,000", "actual", "3,200,000", "achievement", "106.7%", "achievementNum", 106.7),
            Map.of("category", "华东区", "target", "4,500,000", "actual", "4,200,000", "achievement", "93.3%", "achievementNum", 93.3),
            Map.of("category", "华南区", "target", "4,000,000", "actual", "5,100,000", "achievement", "127.5%", "achievementNum", 127.5),
            Map.of("category", "西南区", "target", "2,500,000", "actual", "2,300,000", "achievement", "92.0%", "achievementNum", 92.0),
            Map.of("category", "西北区", "target", "1,800,000", "actual", "2,000,000", "achievement", "111.1%", "achievementNum", 111.1),
            Map.of("category", "东北区", "target", "2,200,000", "actual", "2,400,000", "achievement", "109.1%", "achievementNum", 109.1),
            Map.of("category", "华中区", "target", "3,200,000", "actual", "3,500,000", "achievement", "109.4%", "achievementNum", 109.4),
            Map.of("category", "北京销售部", "target", "1,500,000", "actual", "1,650,000", "achievement", "110.0%", "achievementNum", 110.0),
            Map.of("category", "上海销售部", "target", "2,000,000", "actual", "1,900,000", "achievement", "95.0%", "achievementNum", 95.0),
            Map.of("category", "广州销售部", "target", "1,800,000", "actual", "2,000,000", "achievement", "111.1%", "achievementNum", 111.1),
            Map.of("category", "深圳销售部", "target", "2,100,000", "actual", "2,600,000", "achievement", "123.8%", "achievementNum", 123.8),
            Map.of("category", "成都销售部", "target", "1,400,000", "actual", "1,350,000", "achievement", "96.4%", "achievementNum", 96.4),
            Map.of("category", "西安销售部", "target", "1,200,000", "actual", "1,300,000", "achievement", "108.3%", "achievementNum", 108.3),
            Map.of("category", "武汉销售部", "target", "1,600,000", "actual", "1,800,000", "achievement", "112.5%", "achievementNum", 112.5),
            Map.of("category", "天津销售部", "target", "1,300,000", "actual", "1,250,000", "achievement", "96.2%", "achievementNum", 96.2),
            Map.of("category", "重庆销售部", "target", "1,700,000", "actual", "1,850,000", "achievement", "108.8%", "achievementNum", 108.8),
            Map.of("category", "南京销售部", "target", "1,500,000", "actual", "1,600,000", "achievement", "106.7%", "achievementNum", 106.7),
            Map.of("category", "杭州销售部", "target", "1,800,000", "actual", "2,000,000", "achievement", "111.1%", "achievementNum", 111.1),
            Map.of("category", "青岛销售部", "target", "1,400,000", "actual", "1,300,000", "achievement", "92.9%", "achievementNum", 92.9),
            Map.of("category", "大连销售部", "target", "1,100,000", "actual", "1,250,000", "achievement", "113.6%", "achievementNum", 113.6),
            Map.of("category", "沈阳销售部", "target", "1,000,000", "actual", "1,150,000", "achievement", "115.0%", "achievementNum", 115.0),
            Map.of("category", "长沙销售部", "target", "1,300,000", "actual", "1,450,000", "achievement", "111.5%", "achievementNum", 111.5),
            Map.of("category", "郑州销售部", "target", "1,200,000", "actual", "1,100,000", "achievement", "91.7%", "achievementNum", 91.7),
            Map.of("category", "昆明销售部", "target", "900,000", "actual", "1,000,000", "achievement", "111.1%", "achievementNum", 111.1),
            Map.of("category", "贵阳销售部", "target", "800,000", "actual", "850,000", "achievement", "106.3%", "achievementNum", 106.3),
            Map.of("category", "兰州销售部", "target", "700,000", "actual", "750,000", "achievement", "107.1%", "achievementNum", 107.1),
            Map.of("category", "太原销售部", "target", "850,000", "actual", "900,000", "achievement", "105.9%", "achievementNum", 105.9),
            Map.of("category", "石家庄销售部", "target", "950,000", "actual", "1,000,000", "achievement", "105.3%", "achievementNum", 105.3),
            Map.of("category", "合肥销售部", "target", "1,100,000", "actual", "1,200,000", "achievement", "109.1%", "achievementNum", 109.1),
            Map.of("category", "福州销售部", "target", "1,250,000", "actual", "1,350,000", "achievement", "108.0%", "achievementNum", 108.0),
            Map.of("category", "厦门销售部", "target", "1,200,000", "actual", "1,300,000", "achievement", "108.3%", "achievementNum", 108.3),
            Map.of("category", "南昌销售部", "target", "950,000", "actual", "1,000,000", "achievement", "105.3%", "achievementNum", 105.3),
            Map.of("category", "济南销售部", "target", "1,350,000", "actual", "1,400,000", "achievement", "103.7%", "achievementNum", 103.7),
            Map.of("category", "哈尔滨销售部", "target", "1,000,000", "actual", "1,100,000", "achievement", "110.0%", "achievementNum", 110.0),
            Map.of("category", "长春销售部", "target", "800,000", "actual", "850,000", "achievement", "106.3%", "achievementNum", 106.3),
            Map.of("category", "海口销售部", "target", "600,000", "actual", "700,000", "achievement", "116.7%", "achievementNum", 116.7),
            Map.of("category", "南宁销售部", "target", "700,000", "actual", "650,000", "achievement", "92.9%", "achievementNum", 92.9),
            Map.of("category", "呼和浩特销售部", "target", "550,000", "actual", "600,000", "achievement", "109.1%", "achievementNum", 109.1),
            Map.of("category", "银川销售部", "target", "500,000", "actual", "550,000", "achievement", "110.0%", "achievementNum", 110.0),
            Map.of("category", "西宁销售部", "target", "450,000", "actual", "500,000", "achievement", "111.1%", "achievementNum", 111.1),
            Map.of("category", "拉萨销售部", "target", "400,000", "actual", "450,000", "achievement", "112.5%", "achievementNum", 112.5),
            Map.of("category", "乌鲁木齐销售部", "target", "650,000", "actual", "700,000", "achievement", "107.7%", "achievementNum", 107.7),
            Map.of("category", "珠海销售部", "target", "1,000,000", "actual", "1,100,000", "achievement", "110.0%", "achievementNum", 110.0),
            Map.of("category", "苏州销售部", "target", "1,800,000", "actual", "1,900,000", "achievement", "105.6%", "achievementNum", 105.6),
            Map.of("category", "宁波销售部", "target", "1,400,000", "actual", "1,500,000", "achievement", "107.1%", "achievementNum", 107.1),
            Map.of("category", "无锡销售部", "target", "1,200,000", "actual", "1,250,000", "achievement", "104.2%", "achievementNum", 104.2)
        );
        variables.put("salesData", salesData);

        // 使用 XGen 模板路径(注意 .xgen 后缀)
        // 运行时会自动执行 XGen 模板并生成临时 XML
        String xgenPath = "/config/pdf/sales-data-delta.xml.xgen";
        String outputPath = "output/sales-report-xgen-runtime.pdf";

        // 确保输出目录存在
        new java.io.File("output").mkdirs();

        // 生成PDF - 运行时 XGen 执行 + Delta 合并由 OpenPdfDocumentGenerator 自动处理
        generator.generatePdfFromXml(xgenPath, outputPath, variables);
        System.out.println("✓ 运行时 XGen 执行 + Delta 合并 PDF 已生成: " + outputPath);
        System.out.println("  工作流程: XGen模板执行 -> 生成Delta XML -> 与基础模板合并 -> 生成PDF");
    }

    /**
     * 测试: 使用已编译的 Delta XML 文件生成 PDF
     * (此方法用于编译期 XGen 执行的情况)
     */
    @Test
    public void testXGenDeltaMerge() throws IOException, DocumentException {
        OpenPdfDocumentGenerator generator = new OpenPdfDocumentGenerator();

        // 准备变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "销售业绩报告");
        variables.put("author", "销售部");
        variables.put("date", "2026-03-17");
        variables.put("department", "全国销售部");
        variables.put("quarter", "Q1 2026");

        // 使用已编译的 Delta XML 文件路径(由 Maven precompile 阶段生成)
        String deltaXmlPath = "/config/pdf/sales-data-delta.xml";
        String outputPath = "output/sales-report-xgen-delta.pdf";

        // 确保输出目录存在
        new java.io.File("output").mkdirs();

        // Check if the precompiled file exists. If not, it means we are not running a full build, skip this test.
        if (!io.nop.core.resource.VirtualFileSystem.instance().getResource(deltaXmlPath).exists()) {
            System.out.println("Skip test because XGen delta file not generated. Run maven with precompile to test this.");
            return;
        }

        // 生成PDF - Delta合并由Nop自动处理
        generator.generatePdfFromXml(deltaXmlPath, outputPath, variables);
        System.out.println("✓ XGen Delta 合并 PDF 已生成: " + outputPath);
        System.out.println("  说明: 此PDF基于 XGen模板编译生成的Delta文件,该文件已x:extends基础模板");
    }

    @Test
    public void testTags() throws Exception {
        OpenPdfDocumentGenerator generator = new OpenPdfDocumentGenerator();
        Map<String, Object> variables = new HashMap<>();

        String xmlPath = "/config/pdf/test-tags.xml";
        String outputPath = "output/test-tags.pdf";

        new java.io.File("output").mkdirs();

        generator.generatePdfFromXml(xmlPath, outputPath, variables);
        System.out.println("✓ 标签测试 PDF 已生成: " + outputPath);
    }

    @Test
    public void testPartListDeltaExecution() throws Exception {
        OpenPdfDocumentGenerator generator = new OpenPdfDocumentGenerator();
        Map<String, Object> variables = new HashMap<>();
        
        // 分页触发测试要求: 必须使用大量数据触发 PdfPTable 分页机制
        // 此处组装多个分类的数据，模拟大量的托盘表明细
        variables.put("serialCode", "V-10086");
        variables.put("partAreaCode", "A1区");
        variables.put("installDwgNo", "DWG-12345");
        variables.put("fabDwgNo", "DWG-54321");
        variables.put("code", "PALLET-001");
        variables.put("departName", "设计一部");
        variables.put("userPhone", "138-1234-5678");
        variables.put("name", "测试主甲板材料托盘");

        // 组装 map 数据，符合 part.xlib 迭代的逻辑: items="${map}" --> Map.Entry(key=String, value=List)
        Map<String, java.util.List<Map<String, Object>>> mapData = new java.util.LinkedHashMap<>();
        
        // 添加 3 个大组，每组 10 条数据，总共 30 条明细，绝对触发多页生成
        for (int groupIdx = 1; groupIdx <= 3; groupIdx++) {
            java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("assemCode", "G" + groupIdx + "-" + i);
                
                // 覆盖 item.source != null 和 item.source == null 的情况
                if (i % 2 == 0) {
                    item.put("source", "外购件");
                    item.put("partName", "部件 PartP-" + i); 
                } else {
                    item.put("source", null);
                    item.put("materialName", "材料 MatM-" + i); 
                }
                
                item.put("materialSpec", "Spec-" + i);
                item.put("materialModel", "Model-" + i);
                item.put("materialTexture", "Tx-" + i);
                item.put("amount", String.valueOf(i * 10));
                item.put("defaultUnitName", "kg");
                item.put("weight", String.valueOf(i * 2.5));
                item.put("paintingArea", "5.0");
                item.put("paintingCode", "P-" + i);
                item.put("porNumber", "POR-" + groupIdx + "-" + i);
                item.put("workType", "W-1");
                item.put("note", "备注信息" + i);
                
                items.add(item);
            }
            mapData.put("装配组 " + groupIdx, items);
        }

        variables.put("map", mapData);
        // 原模板通过 partData 注入，但 part.xlib 内使用了 ${map} 遍历，所以冗余放入
        variables.put("partData", mapData);

        String xgenPath = "/config/pdf/part-list-delta.xml.xgen";
        String outputPath = "output/part-list-output.pdf";

        new java.io.File("output").mkdirs();
        generator.generatePdfFromXml(xgenPath, outputPath, variables);
        
        System.out.println("✓ Part List 包含逻辑覆盖及跨页效果的 PDF 已生成: " + outputPath);
    }
}