package io.nop.pdf.core;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfTemplate;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.lang.xml.XNode;
import io.nop.xlang.xpath.XPathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.awt.Color;

public class FooterPdfPageEventHelper extends PdfPageEventHelper implements IOpenPdfPageEventInit {
    private static final Logger LOG = LoggerFactory.getLogger(FooterPdfPageEventHelper.class);

    private OpenPdfDocumentGenerator generator;
    private XNode pdfConfig;
    private int pageNumber = 0;

    private String totalPagesPart = "";
    private boolean isCustomCoords = false;

    // 使用 PdfTemplate 真正解决总页数提取问题（因为在 onEndPage 时无法得知总页数）
    private PdfTemplate totalPagesTemplate;
    private BaseFont templateFont;
    private int templateFontSize;
    private Color templateColor;

    @Override
    public void init(OpenPdfDocumentGenerator generator, XNode pdfConfig, IEvalScope evalScope) {
        this.generator = generator;
        this.pdfConfig = pdfConfig;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        // 创建一个用于延迟填充总页数的模板，宽100 高50足够容纳常见的数字
        totalPagesTemplate = writer.getDirectContent().createTemplate(100, 50);
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        pageNumber++;
    }

    private Float parseFloat(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (pdfConfig == null || generator == null) {
            return;
        }

        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();

        try {
            @SuppressWarnings("unchecked")
            List<XNode> pages = (List<XNode>) pdfConfig.selectMany(XPathHelper.parseXSelector("pages/page"));
            
            int pageIndex = Math.min(pageNumber - 1, pages.size() - 1);
            if (pageIndex >= 0 && pageIndex < pages.size()) {
                XNode currentPageNode = pages.get(pageIndex);
                XNode footerNode = currentPageNode != null ? currentPageNode.childByTag("footer") : null;
                
                if (footerNode != null) {
                    String footerFontFamily = generator.getStyleString(footerNode, "fontFamily", OpenPdfDocumentGenerator.DEFAULT_FONT_FAMILY);
                    int footerFontSize = generator.getStyleInt(footerNode, "fontSize", 9);
                    String footerTextColor = generator.getStyleString(footerNode, "color", "#000000");
                    BaseFont footerFont = OpenPdfDocumentGenerator.getBaseFont(footerFontFamily, footerFontSize);
                    
                    this.templateFont = footerFont;
                    this.templateFontSize = footerFontSize;
                    this.templateColor = OpenPdfDocumentGenerator.parseColor(footerTextColor);

                    cb.setFontAndSize(footerFont, footerFontSize);
                    cb.setColorFill(this.templateColor);

                    // 1. 读取 XDef 设置的自定义坐标
                    Float pgMarginRight = parseFloat(footerNode.attrText("pageMarginRight"));
                    Float pgMarginTop = parseFloat(footerNode.attrText("pageMarginTop"));
                    Float tgMarginRight = parseFloat(footerNode.attrText("totalMarginRight"));
                    Float tgMarginTop = parseFloat(footerNode.attrText("totalMarginTop"));

                    this.isCustomCoords = (pgMarginRight != null && pgMarginTop != null) || (tgMarginRight != null && tgMarginTop != null);

                    XNode footerContent = footerNode.childByTag("content");
                    String footerText = footerContent != null && footerContent.text() != null ? footerContent.text() : "";

                    if (!this.isCustomCoords && footerText.trim().isEmpty()) {
                        footerText = "第 ${pageNumber} 页 | 共 ${totalPages} 页";
                    }

                    String[] split = footerText.split("\\|", -1);
                    // 将解析出的片段提取为类属性，方便在 onCloseDocument 中统一替换真实的 totalPages
                    String pageNumberPart = "";

                    for (String part : split) {
                        if (part.contains("${totalPages}")) {
                            this.totalPagesPart = part.trim();
                        } else if (part.contains("${pageNumber}")) {
                            pageNumberPart = part.trim();
                        }
                    }

                    // 兼容容错：如果没有精确匹配到占位符
                    if (pageNumberPart.isEmpty() && this.totalPagesPart.isEmpty() && split.length > 0) {
                        pageNumberPart = split[0].trim();
                        if (split.length > 1) {
                            this.totalPagesPart = split[1].trim();
                        }
                    }

                    if (this.isCustomCoords) {
                        Float pgX = null, pgY = null, tgX = null, tgY = null;

                        if (pgMarginRight != null && pgMarginTop != null) {
                            pgX = document.right() - pgMarginRight;
                            pgY = document.top() - pgMarginTop;
                        }

                        if (tgMarginRight != null && tgMarginTop != null) {
                            tgX = document.right() - tgMarginRight;
                            tgY = document.top() - tgMarginTop;
                        }

                        if (pgX != null && !pageNumberPart.isEmpty()) {
                            String pgText = pageNumberPart.replace("${pageNumber}", String.valueOf(pageNumber));
                            cb.beginText();
                            // 根据右边距设定向左延伸对齐
                            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, pgText, pgX, pgY, 0);
                            cb.endText();
                        }

                        if (tgX != null && !this.totalPagesPart.isEmpty() && this.totalPagesPart.contains("${totalPages}")) {
                            // 将总页数的整体占位符放在 tgX - 100 处，下沉 5 留出底端渲染边距，配合内部补偿
                            cb.addTemplate(totalPagesTemplate, tgX - 100, tgY - 5);
                        }
                    } else {
                        // 2. 如果没有指定坐标，默认采用组合左右对齐居中拼接
                        float centerX = (document.right() + document.left()) / 2;
                        float y = document.bottom() - 10;

                        if (!pageNumberPart.isEmpty()) {
                            String pgText = pageNumberPart.replace("${pageNumber}", String.valueOf(pageNumber));
                            cb.beginText();
                            // 页码靠右对齐放置在中心左侧，留点防粘缝隙
                            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, pgText, centerX - 5, y, 0);
                            cb.endText();
                        }

                        if (!this.totalPagesPart.isEmpty()) {
                            if (this.totalPagesPart.contains("${totalPages}")) {
                                // 模板本身是个左起画布，把它锚定在中心右侧，同理下沉 5
                                cb.addTemplate(totalPagesTemplate, centerX + 5, y - 5);
                            } else {
                                cb.beginText();
                                cb.showTextAligned(PdfContentByte.ALIGN_LEFT, this.totalPagesPart, centerX + 5, y, 0);
                                cb.endText();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("绘制页脚失败: {}", e.getMessage());
        }

        cb.restoreState();
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        // 在文档关闭前，真实的总页码终于能确定，此时向模板写入真实的总页数
        totalPagesTemplate.beginText();
        totalPagesTemplate.setFontAndSize(templateFont, templateFontSize);
        totalPagesTemplate.setColorFill(templateColor);

        if (!this.totalPagesPart.isEmpty() && this.totalPagesPart.contains("${totalPages}")) {
            // 完全按要求：直接传入整体 replace 后的字符串！不再做任何 prefix、suffix 切割
            String totalStr = this.totalPagesPart.replace("${totalPages}", String.valueOf(writer.getPageNumber() - 1));
            // y 轴在模板内提升 5，避免包含下垂笔画的中文字符（如“页”）被模版天然的 bottom 边界剪裁
            if (this.isCustomCoords) {
                totalPagesTemplate.showTextAligned(PdfContentByte.ALIGN_RIGHT, totalStr, 100, 5, 0);
            } else {
                totalPagesTemplate.showTextAligned(PdfContentByte.ALIGN_LEFT, totalStr, 0, 5, 0);
            }
        }
        totalPagesTemplate.endText();
    }
}
