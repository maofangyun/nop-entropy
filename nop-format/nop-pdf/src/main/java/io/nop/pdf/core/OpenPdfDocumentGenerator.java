package io.nop.pdf.core;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import io.nop.core.lang.eval.EvalScopeImpl;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.core.resource.impl.FileResource;
import io.nop.xlang.api.XLang;
import io.nop.xlang.ast.XLangOutputMode;
import io.nop.xlang.xdsl.DslNodeLoader;
import io.nop.xlang.xdsl.XDslExtendPhase;
import io.nop.xlang.xdsl.XDslExtendResult;
import io.nop.xlang.xpath.XPathHelper;
import io.nop.commons.util.StringHelper;
import io.nop.commons.util.CollectionHelper;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.exceptions.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.awt.Color;

/**
 * 基于 OpenPDF 的 PDF 文档生成器
 * 使用 iText 开源版本 (OpenPDF) 来处理复杂的 PDF 生成任务
 */
public class OpenPdfDocumentGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(OpenPdfDocumentGenerator.class);

    // 样式缓存，key为样式名称，value为样式属性Map
    private final Map<String, Map<String, Object>> styleCache = new HashMap<>();

    // PDF 文档级默认字体，从 pdfConfig 的 defaultFontFamily 属性读取
    static String DEFAULT_FONT_FAMILY = "Microsoft YaHei";

    // 页面尺寸常量
    private static final Map<String, Rectangle> PAGE_SIZES = new HashMap<>();

    static {
        PAGE_SIZES.put("A4", PageSize.A4);
        PAGE_SIZES.put("A3", PageSize.A3);
        PAGE_SIZES.put("A5", PageSize.A5);
        PAGE_SIZES.put("Letter", PageSize.LETTER);
        PAGE_SIZES.put("Legal", PageSize.LEGAL);

        // 关键修复：注册所有系统字体，使得能够通过字体名加载（如 SimSun, Microsoft YaHei）
        try {
            // OpenPDF 推荐使用 registerDirectories() 自动扫描系统字体目录
            FontFactory.registerDirectories();
            
            // 兜底方案：在 Windows 环境下明确注册字体目录
            String windir = System.getenv("WINDIR");
            if (windir != null) {
                FontFactory.registerDirectory(windir + "\\Fonts");
            }
            
            LOG.info("已完成系统字体扫描与注册");
        } catch (Exception e) {
            LOG.warn("注册系统字体失败", e);
        }
    }

    /**
     * 获取 BaseFont（供直接操作 PdfContentByte 时使用）。
     * 优先使用 Identity-H 编码（支持 Unicode/中文），若字体不支持则回退到 WINANSI。
     * 若字体名称无效，FontFactory 将抛出异常。
     */
    static BaseFont getBaseFont(String fontFamily, float fontSize) throws Exception {
        try {
            Font font = FontFactory.getFont(fontFamily, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, fontSize);
            return font.getCalculatedBaseFont(false);
        } catch (Exception e) {
            Font font = FontFactory.getFont(fontFamily, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED, fontSize);
            return font.getCalculatedBaseFont(false);
        }
    }

    /**
     * 辅助方法：获取字体。
     * 优先使用 Identity-H 编码（支持 Unicode/中文），若字体不支持则回退到 WINANSI。
     * 若字体名称无效，FontFactory 将抛出异常。
     */
    private static Font getFont(String fontFamily, float fontSize, int style, Color color) {
        try {
            return FontFactory.getFont(fontFamily, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, fontSize, style, color);
        } catch (Exception e) {
            return FontFactory.getFont(fontFamily, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED, fontSize, style, color);
        }
    }

    /**
     * 从 PDF XML 配置文件生成 PDF
     * 如果是 .xgen 后缀，会先执行 XGen 模板生成 XML，并自动处理 x:extends 合并
     */
    public void generatePdfFromXml(String pdfXmlPath, String outputPath,
                                   Map<String, Object> variables) throws IOException, DocumentException {
        LOG.info("开始生成PDF文件: {}", pdfXmlPath);

        XNode pdfConfig;
        // 创建评估作用域，注入变量
        IEvalScope evalScope = createEvalScope(variables);

        if (pdfXmlPath.endsWith(".xgen")) {
            // 1a. 路径是 .xgen 文件：执行 XGen 模板，得到含 x:extends 的 XNode，再做 DSL 合并
            XNode rawNode = executeXGenTemplate(pdfXmlPath, evalScope);
            LOG.info("XGen 模板已执行，开始处理 x:extends 差量合并");
            XDslExtendResult extendResult = DslNodeLoader.INSTANCE.loadFromNode(
                    rawNode, "/nop/schema/pdf.xdef", XDslExtendPhase.mergeBase);
            pdfConfig = extendResult.getNode();
            LOG.info("x:extends 差量合并完成");
        } else {
            // 1b. 普通 XML 文件：直接加载
            pdfConfig = loadPdfConfig(pdfXmlPath);
        }
        LOG.debug("已加载PDF配置文件");

        // 2. 生成 PDF
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            generatePdfFromNode(pdfConfig, evalScope, fos);
            LOG.info("PDF文件已成功生成: {}", outputPath);
        }
    }

    /**
     * 执行 XGen 模板，返回生成的 XNode
     *
     * <p>工作流程:
     * 1. 通过 VirtualFileSystem 加载 .xgen 资源
     * 2. 使用 XLang.parseXpl(resource, XLangOutputMode.node) 编译并执行模板
     * 3. 直接获取生成包含 x:extends 的 XNode
     *
     * @param xgenPath  虚拟路径，如 /config/pdf/sales-data-delta.xml.xgen
     * @param evalScope 已注入变量的评估作用域
     * @return 模板执行产生的 XNode（含 x:extends 属性）
     * @throws IOException 模板文件不存在或执行失败时抛出
     */
    private XNode executeXGenTemplate(String xgenPath, IEvalScope evalScope) throws IOException {
        // 通过 VirtualFileSystem 加载 XGen 模板资源
        IResource xgenResource = VirtualFileSystem.instance().getResource(xgenPath);
        if (!xgenResource.exists()) {
            throw new IOException("XGen 模板文件不存在: " + xgenPath);
        }

        try {
            // xgen 模板以 node 的模式输出
            Object result = XLang.parseXpl(xgenResource, XLangOutputMode.node).invoke(evalScope);

            if (result == null) {
                throw new IOException("XGen 模板执行后未生成内容: " + xgenPath);
            }

            if (result instanceof XNode) {
                XNode node = (XNode) result;
                // outputMode=node 时若有多个根子节点，Nop 会用 <_> 虚拟节点包裹
                // 取其第一个子节点作为真正的文档根节点（即 <pdf> 等节点）
                if ("_".equals(node.getTagName()) && node.hasChild()) {
                    node = node.child(0);
                    LOG.info("XGen 模板执行成功（node 模式），解除 <_> 包装，实际根节点: <{}>", node.getTagName());
                } else {
                    LOG.info("XGen 模板执行成功（node 模式），根节点: <{}>", node.getTagName());
                }
                return node;
            }

            throw new IOException("XGen 模板应该以 node 模式输出并生成 XNode。实际收到: " + result.getClass());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("执行 XGen 模板失败: " + xgenPath, e);
        }
    }



    /**
     * 应用 export 相关配置策略 (压缩及加密权限等)
     */
    private void applyExportConfig(XNode pdfConfig, PdfWriter writer, IEvalScope evalScope) {
        XNode exportNode = pdfConfig.childByTag("export");
        if (exportNode == null) return;

        boolean compression = exportNode.attrBoolean("compression", true);
        if (compression) {
            writer.setFullCompression();
            LOG.debug("PDF 全局压缩已启用");
        }

        XNode securityNode = exportNode.childByTag("security");
        if (securityNode != null) {
            try {
                String userPassword = evaluateString(securityNode.attrText("userPassword", ""), evalScope);
                String ownerPassword = evaluateString(securityNode.attrText("ownerPassword", ""), evalScope);

                boolean allowPrint = securityNode.attrBoolean("allowPrint", true);
                boolean allowCopy = securityNode.attrBoolean("allowCopy", true);
                boolean allowModify = securityNode.attrBoolean("allowModify", true);

                int permissions = 0;
                if (allowPrint) permissions |= PdfWriter.ALLOW_PRINTING;
                if (allowCopy) permissions |= PdfWriter.ALLOW_COPY;
                if (allowModify) permissions |= PdfWriter.ALLOW_MODIFY_CONTENTS;

                byte[] userBytes = StringHelper.isEmpty(userPassword) ? null : userPassword.getBytes(StandardCharsets.UTF_8);
                byte[] ownerBytes = StringHelper.isEmpty(ownerPassword) ? null : ownerPassword.getBytes(StandardCharsets.UTF_8);

                writer.setEncryption(userBytes, ownerBytes, permissions, PdfWriter.STANDARD_ENCRYPTION_128);
                LOG.info("PDF 加密权限配置已应用");
            } catch (Throwable e) {
                LOG.error("设置 PDF 安全属性失败 (可能缺少 bouncycastle 依赖)", e);
            }
        }
    }

    /**
     * 从 XNode 配置生成 PDF
     */
    private void generatePdfFromNode(XNode pdfConfig, IEvalScope evalScope,
                                     OutputStream outputStream) throws IOException, DocumentException {
        // 加载样式定义
        loadStyles(pdfConfig);

        // 读取文档级默认字体
        String configFont = pdfConfig.attrText("defaultFontFamily");
        if (!StringHelper.isEmpty(configFont)) {
            DEFAULT_FONT_FAMILY = configFont;
        }

        // 提取 PDF 根属性
        String title = evaluateString(pdfConfig.attrText("title", "PDF Document"), evalScope);
        String pageSize = pdfConfig.attrText("pageSize", "A4");
        String orientation = pdfConfig.attrText("pageOrientation", "portrait");

        int marginTop = pdfConfig.attrInt("marginTop", 20);
        int marginBottom = pdfConfig.attrInt("marginBottom", 20);
        int marginLeft = pdfConfig.attrInt("marginLeft", 20);
        int marginRight = pdfConfig.attrInt("marginRight", 20);

        // 创建 Document 对象
        Rectangle pageRectangle = getPageRectangle(pageSize, orientation);
        Document document = new Document(pageRectangle, marginLeft, marginRight, marginTop, marginBottom);

        // 创建 PdfWriter
        PdfWriter pdfWriter = PdfWriter.getInstance(document, outputStream);

        // 应用 export 相关配置策略 (压缩及加密权限等)
        applyExportConfig(pdfConfig, pdfWriter, evalScope);

        // 设置页面事件监听器（用于页头页脚）
        CompositePdfPageEventHelper forwarder = new CompositePdfPageEventHelper();

        // 实例化 footer 的 PdfPageEventHelper
        PdfPageEventHelper footerEvent = null;
        try {
            @SuppressWarnings("unchecked")
            List<XNode> pages = (List<XNode>) pdfConfig.selectMany(XPathHelper.parseXSelector("pages/page"));
            String footerInstance = null;
            if (pages != null && !pages.isEmpty() && pages.get(0).childByTag("footer") != null) {
                footerInstance = pages.get(0).childByTag("footer").attrText("instance");
            }
            if (StringHelper.isEmpty(footerInstance)) {
                footerInstance = "io.nop.pdf.core.FooterPdfPageEventHelper";
            }
            footerEvent = createAndInitEventHelper(footerInstance, pdfConfig, evalScope);
        } catch (Exception e) {
            LOG.warn("无法实例化 footer PdfPageEventHelper", e);
        }
        if (footerEvent != null) {
            forwarder.addEvent(footerEvent);
        }

        // 实例化 export 的 PdfPageEventHelper
        PdfPageEventHelper exportEvent = null;
        try {
            XNode exportNode = pdfConfig.childByTag("export");
            String exportInstance = null;
            if (exportNode != null) {
                exportInstance = exportNode.attrText("instance");
            }
            if (StringHelper.isEmpty(exportInstance)) {
                exportInstance = "io.nop.pdf.core.ExportPdfPageEventHelper";
            }
            exportEvent = createAndInitEventHelper(exportInstance, pdfConfig, evalScope);
        } catch (Exception e) {
            LOG.warn("无法实例化 export PdfPageEventHelper", e);
        }
        if (exportEvent != null) {
            forwarder.addEvent(exportEvent);
        }

        pdfWriter.setPageEvent(forwarder);

        // 设置文档元数据
        document.addTitle(title);
        document.addAuthor(evaluateString(pdfConfig.attrText("author", ""), evalScope));
        document.addSubject(evaluateString(pdfConfig.attrText("subject", ""), evalScope));
        document.addCreator(evaluateString(pdfConfig.attrText("creator", ""), evalScope));
        document.addProducer();

        // 打开文档
        document.open();

        try {
            // 处理页面，使用XPath简化遍历
            @SuppressWarnings("unchecked")
            List<XNode> pages = (List<XNode>) pdfConfig.selectMany(XPathHelper.parseXSelector("pages/page"));
            if (CollectionHelper.isEmpty(pages)) {
                // 如果没有页面定义，添加空白页
                document.add(new Paragraph(" "));
            } else {
                for (int i = 0; i < pages.size(); i++) {
                    XNode pageNode = pages.get(i);
                    renderPage(document, pageNode, evalScope);

                    // 如果不是最后一页，添加分页符
                    if (i < pages.size() - 1) {
                        document.newPage();
                    }
                }
            }
        } finally {
            document.close();
        }
    }

    /**
     * 渲染单个页面内容
     */
    private void renderPage(Document document, XNode pageNode, IEvalScope evalScope)
            throws DocumentException, IOException {
        
        // 渲染页头（如果存在）
        XNode headerNode = pageNode.childByTag("header");
        if (headerNode != null) {
            String headerText = evaluateString(headerNode.childByTag("content") != null ?
                headerNode.childByTag("content").text() : "", evalScope);
            if (!StringHelper.isEmpty(headerText)) {
                String headerFontFamily = headerNode.attrText("fontFamily", DEFAULT_FONT_FAMILY);
                Integer headerFontSize = headerNode.attrInt("fontSize", 10);
                String headerTextColor = getStyleString(headerNode, "color", "#000000");
                Paragraph header = new Paragraph(headerText, getFont(headerFontFamily, headerFontSize, Font.NORMAL, parseColor(headerTextColor)));
                header.setAlignment(Element.ALIGN_CENTER);
                header.setSpacingAfter(10);
                document.add(header);
            }
        }

        // 渲染页面内容
        XNode contentNode = pageNode.childByTag("content");
        if (contentNode != null) {
            for (XNode child : contentNode.getChildren()) {
                Element element = renderElement(child, evalScope);
                if (element != null) {
                    document.add(element);
                }
            }
        }
    }

    /**
     * 渲染单个元素（段落、表格等）
     */
    private Element renderElement(XNode elementNode, IEvalScope evalScope)
            throws DocumentException, IOException {
        String tagName = elementNode.getTagName();

        switch (tagName) {
            case "paragraph":
                return renderParagraph(elementNode, evalScope);
            case "table":
                return renderTable(elementNode, evalScope);
            case "image":
                return renderImage(elementNode, evalScope);
            case "chart":
                return renderChart(elementNode, evalScope);
            case "spacer":
                return renderSpacer(elementNode);
            case "pageBreak":
                return new Chunk("", FontFactory.getFont(DEFAULT_FONT_FAMILY, 1));
            default:
                LOG.warn("未知的元素类型: {}", tagName);
                return null;
        }
    }

    /**
     * 渲染图表（当前仅生成基础的预格式化占位区域，未来可扩展真实图表绘制）
     */
    private Element renderChart(XNode chartNode, IEvalScope evalScope) throws DocumentException {
        String type = chartNode.attrText("type", "bar");
        String title = evaluateString(chartNode.attrText("title", "Chart"), evalScope);
        String height = chartNode.attrText("height", "300");

        LOG.info("渲染图表占位符: type={}, title={}", type, title);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        
        PdfPCell cell = new PdfPCell(new Paragraph("[ 预留图表位置 ] " + title + " (" + type + ")", getFont(DEFAULT_FONT_FAMILY, 12, Font.BOLD, Color.GRAY)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Float minHeight = ConvertHelper.toFloat(height.replaceAll("[^0-9.]", ""));
            cell.setMinimumHeight(minHeight == null ? 300f : minHeight);
        } catch(Exception e) {
            cell.setMinimumHeight(300f);
        }
        cell.setBackgroundColor(new Color(245, 245, 245));
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setBorderWidth(1);
        
        table.addCell(cell);
        return table;
    }

    /**
     * 渲染段落
     */
    private Element renderParagraph(XNode paragraphNode, IEvalScope evalScope) throws DocumentException {
        // 获取文本（优先从text属性，否则从节点内容）
        String rawText = paragraphNode.attrText("text");
        if (StringHelper.isEmpty(rawText)) {
            rawText = paragraphNode.text() != null ? paragraphNode.text() : "";
        }
        String text = evaluateString(rawText, evalScope);

        // 使用样式系统获取属性（支持className和内联属性）
        int fontSize = getStyleInt(paragraphNode, "fontSize", 12);
        String fontFamily = getStyleString(paragraphNode, "fontFamily", DEFAULT_FONT_FAMILY);
        String align = getStyleString(paragraphNode, "align", "left");
        boolean bold = getStyleBoolean(paragraphNode, "bold", false);
        boolean italic = getStyleBoolean(paragraphNode, "italic", false);
        String color = getStyleString(paragraphNode, "color", "#000000");
        float lineHeight = (float) getStyleDouble(paragraphNode, "lineHeight", 1.5);

        // 创建字体
        int fontStyle = Font.NORMAL;
        if (bold) fontStyle |= Font.BOLD;
        if (italic) fontStyle |= Font.ITALIC;

        Font font = getFont(fontFamily, fontSize, fontStyle, parseColor(color));

        // 创建段落
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setLeading(fontSize * lineHeight);

        // 设置对齐方式
        switch (align.toLowerCase()) {
            case "center":
                paragraph.setAlignment(Element.ALIGN_CENTER);
                break;
            case "right":
                paragraph.setAlignment(Element.ALIGN_RIGHT);
                break;
            case "justify":
                paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
                break;
            default:
                paragraph.setAlignment(Element.ALIGN_LEFT);
        }

        // 设置间距
        int spaceBefore = getStyleInt(paragraphNode, "spaceBefore", 0);
        int spaceAfter = getStyleInt(paragraphNode, "spaceAfter", 0);
        paragraph.setSpacingBefore(spaceBefore);
        paragraph.setSpacingAfter(spaceAfter);

        return paragraph;
    }

    /**
     * 渲染表格 - 使用新的行结构
     */
    private Element renderTable(XNode tableNode, IEvalScope evalScope) throws DocumentException {
        int borderWidth = tableNode.attrInt("borderWidth", 1);
        String borderColor = tableNode.attrText("borderColor", "#000000");
        int cellPadding = tableNode.attrInt("cellPadding", 5);
        
        // 获取列定义
        XNode columnsNode = tableNode.childByTag("columns");
        List<XNode> columnNodes = columnsNode != null ? columnsNode.getChildren() : List.of();
        int columnCount = CollectionHelper.isEmpty(columnNodes) ? 3 : columnNodes.size();

        // 创建表格
        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);

        // 设置列宽
        if (!CollectionHelper.isEmpty(columnNodes)) {
            float[] widths = new float[columnCount];
            for (int i = 0; i < columnNodes.size(); i++) {
                String width = columnNodes.get(i).attrText("width", "");
                if (width.endsWith("%")) {
                    widths[i] = Float.parseFloat(width.substring(0, width.length() - 1));
                } else {
                    widths[i] = 100f / columnCount;
                }
            }
            table.setWidths(widths);
        }

        // 设置表格样式
        table.getDefaultCell().setBorderWidth(borderWidth);
        table.getDefaultCell().setBorderColor(parseColor(borderColor));
        table.getDefaultCell().setPadding(cellPadding);
        
        int headerRowCount = 0;

        // 1. 渲染独立的 <header> 标签内部行
        XNode headerNode = tableNode.childByTag("header");
        if (headerNode != null) {
            List<XNode> headerRows = headerNode.getChildren();
            for (XNode rowNode : headerRows) {
                renderTableRow(table, rowNode, evalScope);
                headerRowCount++;
            }
        }

        // 2. 渲染通常的行
        XNode rowsNode = tableNode.childByTag("rows");
        if (rowsNode != null) {
            List<XNode> allRows = rowsNode.getChildren();
            for (XNode rowNode : allRows) {
                renderTableRow(table, rowNode, evalScope);
            }
        }

        // 设置表头行数，支持自动重复换页表头
        if (headerRowCount > 0) {
            table.setHeaderRows(headerRowCount);
            LOG.debug("设置表头行数: {}", headerRowCount);
        }

        return table;
    }

    /**
     * 辅助方法：渲染表格的一个单独的行
     */
    private void renderTableRow(PdfPTable table, XNode rowNode, IEvalScope evalScope) throws DocumentException {
        // 获取行高配置
        int rowHeight = rowNode.attrInt("height", 0);
        
        XNode cellsNode = rowNode.childByTag("cells");
        if (cellsNode != null) {
            List<XNode> cellNodes = cellsNode.getChildren();
            LOG.debug("行中包含 {} 个单元格", cellNodes.size());

            for (int i = 0; i < cellNodes.size(); i++) {
                XNode cellNode = cellNodes.get(i);
                LOG.debug("渲染单元格 [{}]: tagName={}, text={}",
                        i, cellNode.getTagName(), cellNode.attrText("text", ""));

                PdfPCell cell = renderTableCell(cellNode, evalScope);

                if (rowHeight > 0) {
                    // 设置行高
                    cell.setMinimumHeight(rowHeight);
                }
                table.addCell(cell);
            }
        } else {
            LOG.warn("行没有 cells 子节点");
        }
    }

    /**
     * 修复：正确处理所有类型的单元格
     */
    private PdfPCell renderTableCell(XNode cellNode, IEvalScope evalScope)
            throws DocumentException {
        // 获取文本内容 - 重要修复
        String text = evaluateString(cellNode.attrText("text", ""), evalScope);

        // 如果 text 为空，尝试获取节点内容
        if (StringHelper.isEmpty(text)) {
            Object content = cellNode.getValue();
            if (content != null) {
                text = content.toString();
            } else {
                text = "";
            }
        }

        LOG.debug("单元格内容: text='{}', tagName={}", text, cellNode.getTagName());

        // 使用样式系统获取属性
        int colSpan = getStyleInt(cellNode, "colSpan", 1);
        int rowSpan = getStyleInt(cellNode, "rowSpan", 1);

        String align = getStyleString(cellNode, "align", "left");
        String verticalAlign = getStyleString(cellNode, "verticalAlign", "middle");

        // 背景色处理 - 使用样式系统优先级
        String backgroundColor = getStyleString(cellNode, "backgroundColor", "#FFFFFF");
        String color = getStyleString(cellNode, "color", "#000000");
        boolean bold = getStyleBoolean(cellNode, "bold", true);
        int fontSize = getStyleInt(cellNode, "fontSize", 10);
        String fontFamily = getStyleString(cellNode, "fontFamily", DEFAULT_FONT_FAMILY);

        // 创建字体
        Font font = getFont(fontFamily, fontSize, bold ? Font.BOLD : Font.NORMAL, parseColor(color));

        // 创建段落
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(getAlignmentValue(align));

        // 创建单元格
        PdfPCell cell = new PdfPCell(paragraph);

        // 设置合并
        if (colSpan > 1) {
            cell.setColspan(colSpan);
        }
        if (rowSpan > 1) {
            cell.setRowspan(rowSpan);
        }

        // 设置背景色
        Color bgColor = parseColor(backgroundColor);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }

        // 设置内边距和边框
        int cellPadding = getStyleInt(cellNode, "padding", -1);
        if (cellPadding >= 0) {
            cell.setPadding(cellPadding);
        }

        // 设置边框宽度
        cell.setBorderWidth(getStyleInt(cellNode, "borderWidth", 1));

        // 设置水平对齐
        cell.setHorizontalAlignment(getAlignmentValue(align));

        // 设置垂直对齐
        switch (verticalAlign.toLowerCase()) {
            case "top":
                cell.setVerticalAlignment(Element.ALIGN_TOP);
                break;
            case "bottom":
                cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
                break;
            default:
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        }

        return cell;
    }

    /**
     * 辅助方法：获取对齐值
     */
    private int getAlignmentValue(String align) {
        if (align == null) {
            return Element.ALIGN_LEFT;
        }

        switch (align.toLowerCase()) {
            case "center":
                return Element.ALIGN_CENTER;
            case "right":
                return Element.ALIGN_RIGHT;
            case "justify":
                return Element.ALIGN_JUSTIFIED;
            default:
                return Element.ALIGN_LEFT;
        }
    }

    /**
     * 渲染图片
     */
    private Element renderImage(XNode imageNode, IEvalScope evalScope) throws DocumentException, IOException {
        String src = evaluateString(imageNode.attrText("src", ""), evalScope);
        String width = imageNode.attrText("width", "100");
        String height = imageNode.attrText("height", "100");
        String align = imageNode.attrText("align", "left");

        if (StringHelper.isEmpty(src)) {
            LOG.warn("图片源路径为空");
            return null;
        }

        Image image = Image.getInstance(src);

        // 设置大小
        if (!StringHelper.isEmpty(width) && !StringHelper.isEmpty(height)) {
            Float wF = ConvertHelper.toFloat(width.replaceAll("[^0-9.]", ""));
            float w = wF == null ? 100f : wF;
            Float hF = ConvertHelper.toFloat(height.replaceAll("[^0-9.]", ""));
            float h = hF == null ? 100f : hF;
            image.scaleToFit(w, h);
        }

        // 设置对齐
        image.setAlignment(getAlignmentValue(align));

        return image;
    }

    /**
     * 渲染空白间隔
     */
    private Element renderSpacer(XNode spacerNode) {
        int height = spacerNode.attrInt("height", 10);
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(height);
        return spacer;
    }

    /**
     * 加载并解析 PDF XML 配置文件
     */
    private XNode loadPdfConfig(String pdfXmlPath) throws IOException {
        IResource resource = VirtualFileSystem.instance().getResource(pdfXmlPath);
        if (!resource.exists()) {
            resource = new FileResource(new File(pdfXmlPath));
        }
        if (!resource.exists()) {
            throw new NopException(ErrorCode.define("nop.err.pdf.config-not-exists", "PDF配置文件不存在")).param("path", pdfXmlPath);
        }
        XDslExtendResult extendResult = DslNodeLoader.INSTANCE.loadFromResource(resource, "/nop/schema/pdf.xdef");
        return extendResult.getNode();
    }


    /**
     * 创建评估作用域并注入变量
     */
    private IEvalScope createEvalScope(Map<String, Object> variables) {
        IEvalScope scope = new EvalScopeImpl();

        if (variables != null) {
            variables.forEach(scope::setLocalValue);
        }

        return scope;
    }

    /**
     * 评估字符串表达式（支持 ${variable} 形式）
     */
    private String evaluateString(String text, IEvalScope evalScope) {
        if (StringHelper.isEmpty(text) || !text.contains("${")) {
            return text == null ? "" : text;
        }

        return StringHelper.renderTemplate(text, "${","}",name -> {
            Object val = evalScope.getLocalValue(name);
            return val == null ? "" : val.toString();
        });
    }

    /**
     * 获取页面尺寸
     */
    private Rectangle getPageRectangle(String pageSize, String orientation) {
        Rectangle rect = PAGE_SIZES.getOrDefault(pageSize, PageSize.A4);

        // 处理方向：横向
        if ("landscape".equalsIgnoreCase(orientation)) {
            return new Rectangle(rect.getHeight(), rect.getWidth());
        }

        return rect;
    }

    /**
     * 将颜色字符串转换为 Color 对象
     */
    static Color parseColor(String colorStr) {
        if (StringHelper.isEmpty(colorStr)) {
            return Color.BLACK;
        }

        try {
            // 处理 #RRGGBB 格式
            if (colorStr.startsWith("#")) {
                String hex = colorStr.substring(1);
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                return new Color(r, g, b);
            }

            // 处理 rgb(r,g,b) 格式
            if (colorStr.startsWith("rgb")) {
                String[] parts = colorStr.replaceAll("[^0-9,]", "").split(",");
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            }
        } catch (Exception e) {
            LOG.warn("无法解析颜色: {}", colorStr, e);
        }

        return Color.BLACK;
    }

    private PdfPageEventHelper createAndInitEventHelper(String instance, XNode pdfConfig, IEvalScope evalScope) throws Exception {
        Object bean = null;
        try {
            Class<?> containerClass = Class.forName("io.nop.api.core.ioc.BeanContainer");
            Object containerInstance = containerClass.getMethod("instance").invoke(null);
            if (containerInstance != null) {
                bean = containerClass.getMethod("getBean", String.class).invoke(containerInstance, instance);
            }
        } catch (Throwable ignore) {
        }
        
        if (bean == null) {
            bean = Class.forName(instance).getDeclaredConstructor().newInstance();
        }
        
        if (bean instanceof IOpenPdfPageEventInit) {
            ((IOpenPdfPageEventInit) bean).init(this, pdfConfig, evalScope);
        }
        
        return (PdfPageEventHelper) bean;
    }

    private static class CompositePdfPageEventHelper extends PdfPageEventHelper {
        private final List<PdfPageEventHelper> events = new ArrayList<>();

        public void addEvent(PdfPageEventHelper event) {
            if (event != null) {
                events.add(event);
            }
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            for (PdfPageEventHelper event : events) {
                event.onOpenDocument(writer, document);
            }
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            for (PdfPageEventHelper event : events) {
                event.onStartPage(writer, document);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            for (PdfPageEventHelper event : events) {
                event.onEndPage(writer, document);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            for (PdfPageEventHelper event : events) {
                event.onCloseDocument(writer, document);
            }
        }
    }

    /**
     * 加载样式定义（使用VirtualFileSystem）
     */
    private void loadStyles(XNode pdfConfig) throws IOException {
        styleCache.clear();

        // 处理内联样式，使用XPath简化遍历
        @SuppressWarnings("unchecked")
        List<XNode> styleEntries = (List<XNode>) pdfConfig.selectMany(XPathHelper.parseXSelector("/pdf/styles/style/styleEntry"));
        for (XNode styleEntry : styleEntries) {
            String name = styleEntry.attrText("name");
            styleCache.put(name, styleEntry.getAttrs());
            LOG.debug("加载内联样式: {}", name);
        }

        // 处理外部样式文件引用，使用XPath简化遍历
        @SuppressWarnings("unchecked")
        List<XNode> styleRefs = (List<XNode>) pdfConfig.selectMany(XPathHelper.parseXSelector("/pdf/styles/styleRefs/styleRef[@enabled!='false']"));
        for (XNode styleRef : styleRefs) {
            String path = styleRef.attrText("path");
            if (!StringHelper.isEmpty(path)) {
                loadExternalStyles(path);
            }
        }

        LOG.info("样式加载完成, 共加载 {} 个样式定义", styleCache.size());
    }

    /**
     * 使用VirtualFileSystem加载外部样式文件
     */
    private void loadExternalStyles(String stylePath) throws IOException {
        LOG.info("开始加载外部样式文件: {}", stylePath);
        try {
            // 使用VirtualFileSystem获取资源
            IResource resource = VirtualFileSystem.instance().getResource(stylePath);
            if (!resource.exists()) {
                throw new IOException("样式文件不存在: " + stylePath);
            }

            // 使用DslNodeLoader加载样式文件
            XDslExtendResult extendResult = DslNodeLoader.INSTANCE.loadFromResource(resource, null);
            XNode resultNode = extendResult.getNode();

            // 使用XPath提取样式定义
            @SuppressWarnings("unchecked")
            List<XNode> styleEntries = (List<XNode>) resultNode.selectMany(XPathHelper.parseXSelector("/pdf/styles/style/styleEntry"));
            for (XNode styleEntry : styleEntries) {
                String name = styleEntry.attrText("name");
                styleCache.put(name, styleEntry.getAttrs());
            }

            LOG.info("外部样式文件加载成功: {}", stylePath);
        } catch (IOException e) {
            throw new IOException("加载外部样式文件失败: " + stylePath, e);
        }
    }

    /**
     * 根据className获取样式字符串值
     */
    String getStyleString(XNode node, String attrName, String defaultValue) {
        // 优先级：节点内联属性 > className引用样式 > 默认值

        // 1. 检查内联属性
        String inlineValue = node.attrText(attrName, null);
        if (!StringHelper.isEmpty(inlineValue)) {
            return inlineValue;
        }

        // 2. 检查className引用样式
        String className = node.attrText("className", null);
        if (className != null) {
            Map<String, Object> style = styleCache.get(className);
            if (style != null && style.containsKey(attrName)) {
                return style.get(attrName).toString();
            }
        }

        return defaultValue;
    }

    /**
     * 根据className获取样式整数值
     */
    int getStyleInt(XNode node, String attrName, int defaultValue) {
        String value = getStyleString(node, attrName, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOG.warn("无法解析整数值: {} = {}", attrName, value);
            }
        }
        return defaultValue;
    }

    /**
     * 根据className获取样式布尔值
     */
    private boolean getStyleBoolean(XNode node, String attrName, boolean defaultValue) {
        String value = getStyleString(node, attrName, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * 根据className获取样式浮点值
     */
    private double getStyleDouble(XNode node, String attrName, double defaultValue) {
        String value = getStyleString(node, attrName, null);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                LOG.warn("无法解析浮点数值: {} = {}", attrName, value);
            }
        }
        return defaultValue;
    }
}