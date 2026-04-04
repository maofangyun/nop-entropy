package io.nop.pdf.core;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.*;
import io.nop.commons.util.StringHelper;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.lang.xml.XNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

public class ExportPdfPageEventHelper extends PdfPageEventHelper implements IOpenPdfPageEventInit {
    private static final Logger LOG = LoggerFactory.getLogger(ExportPdfPageEventHelper.class);

    private OpenPdfDocumentGenerator generator;
    private XNode pdfConfig;
    private IEvalScope evalScope;

    @Override
    public void init(OpenPdfDocumentGenerator generator, XNode pdfConfig, IEvalScope evalScope) {
        this.generator = generator;
        this.pdfConfig = pdfConfig;
        this.evalScope = evalScope;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (pdfConfig == null || generator == null) {
            return;
        }

        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();

        try {
            XNode exportNode = pdfConfig.childByTag("export");
            if (exportNode != null && exportNode.childByTag("watermark") != null) {
                XNode watermarkNode = exportNode.childByTag("watermark");
                String wText = generator.getStyleString(watermarkNode, "text", "");
                if (!StringHelper.isEmpty(wText)) {
                    PdfContentByte watermarkCb = writer.getDirectContentUnder();
                    watermarkCb.saveState();
                    try {
                        int wFontSize = watermarkNode.attrInt("fontSize", 50);
                        float opacity = watermarkNode.attrDouble("opacity", 0.3).floatValue();
                        int angle = watermarkNode.attrInt("angle", 45);
                        String colorStr = watermarkNode.attrText("color", "#CCCCCC");
                        Color wColor = OpenPdfDocumentGenerator.parseColor(colorStr);

                        String wFontFamily = watermarkNode.attrText("fontFamily", OpenPdfDocumentGenerator.DEFAULT_FONT_FAMILY);
                        BaseFont baseFont = OpenPdfDocumentGenerator.getBaseFont(wFontFamily, wFontSize);

                        PdfGState gs = new PdfGState();
                        gs.setFillOpacity(opacity);
                        watermarkCb.setGState(gs);

                        int rows = watermarkNode.attrInt("rows", 1);
                        int columns = watermarkNode.attrInt("columns", 1);

                        watermarkCb.beginText();
                        watermarkCb.setFontAndSize(baseFont, wFontSize);
                        watermarkCb.setColorFill(wColor);

                        float pageW = document.getPageSize().getWidth();
                        float pageH = document.getPageSize().getHeight();

                        float cellW = pageW / columns;
                        float cellH = pageH / rows;

                        for (int r = 0; r < rows; r++) {
                            for (int c = 0; c < columns; c++) {
                                float x = cellW * c + cellW / 2;
                                float y = cellH * r + cellH / 2;
                                watermarkCb.showTextAligned(Element.ALIGN_CENTER, wText, x, y, angle);
                            }
                        }

                        watermarkCb.endText();
                    } finally {
                        watermarkCb.restoreState();
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("绘制水印失败: {}", e.getMessage());
        }

        cb.restoreState();
    }
}
