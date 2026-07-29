package com.intellimail.mail.agent.export;

import com.intellimail.mail.entity.AgentTask;
import com.intellimail.mail.entity.AgentTaskStep;
import com.intellimail.mail.exception.ExportException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders one {@link AgentTask}'s result as a branded PDF: markdown ->
 * HTML (flexmark, with table support) -> a styled document with a running
 * header/footer and page numbers -> PDF bytes (openhtmltopdf). Synchronous
 * and in-memory throughout - agent responses are email-length text, never
 * large enough to justify async job tracking.
 */
@Service
public class AgentExportService {

    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a 'UTC'").withZone(ZoneOffset.UTC);

    private final Parser markdownParser;
    private final HtmlRenderer markdownRenderer;

    public AgentExportService() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.markdownParser = Parser.builder(options).build();
        this.markdownRenderer = HtmlRenderer.builder(options).build();
    }

    public byte[] renderTaskAsPdf(AgentTask task, List<AgentTaskStep> steps) {
        String bodyHtml = markdownRenderer.render(markdownParser.parse(
                task.getFinalResult() == null ? "" : task.getFinalResult()));
        String fullHtml = buildDocument(task, steps, bodyHtml);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(fullHtml, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Failed to render PDF export", ex);
        }
    }

    private String buildDocument(AgentTask task, List<AgentTaskStep> steps, String bodyHtml) {
        String generatedAt = GENERATED_AT_FORMAT.format(java.time.Instant.now());
        String goal = HtmlUtils.htmlEscape(task.getGoal() == null ? "" : task.getGoal());
        String status = HtmlUtils.htmlEscape(task.getStatus().name());

        return """
                <html>
                <head>
                <style>%s</style>
                </head>
                <body>
                  <div id="page-header"><div class="brand">IntelliMail</div><div class="doc-type">AI Agent Response</div></div>
                  <div id="page-footer">
                    <span>Generated %s</span>
                    <span class="page-count">Page <span class="pagenumber"></span> of <span class="pagecount"></span></span>
                  </div>

                  <h1 class="doc-title">AI Agent Response</h1>
                  <table class="meta-table">
                    <tr><th>Goal</th><td>%s</td></tr>
                    <tr><th>Status</th><td><span class="status-pill">%s</span></td></tr>
                    <tr><th>Generated</th><td>%s</td></tr>
                  </table>

                  <div class="content">%s</div>

                  %s
                </body>
                </html>
                """.formatted(CSS, generatedAt, goal, status, generatedAt, bodyHtml, buildStepsSection(steps));
    }

    private String buildStepsSection(List<AgentTaskStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        for (AgentTaskStep step : steps) {
            rows.append("<tr><td>").append(step.getStepNumber())
                    .append("</td><td>").append(HtmlUtils.htmlEscape(step.getToolName()))
                    .append("</td><td>").append(HtmlUtils.htmlEscape(step.getStatus().name()))
                    .append("</td><td>").append(HtmlUtils.htmlEscape(truncate(step.getOutputSummary())))
                    .append("</td></tr>");
        }
        return """
                <h2 class="section-title">Steps Taken</h2>
                <table class="steps-table">
                  <thead><tr><th>#</th><th>Tool</th><th>Status</th><th>Output</th></tr></thead>
                  <tbody>%s</tbody>
                </table>
                """.formatted(rows);
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    private static final String CSS = """
            @page {
              size: A4;
              margin: 90px 50px 70px 50px;
              @top-center { content: element(header); }
              @bottom-center { content: element(footer); }
            }
            #page-header { position: running(header); border-bottom: 1px solid #D6D3F7; padding-bottom: 8px; width: 100%; }
            #page-header .brand { font-size: 16px; font-weight: bold; color: #4338CA; font-family: sans-serif; }
            #page-header .doc-type { font-size: 9px; color: #666666; font-family: sans-serif; letter-spacing: 1px; text-transform: uppercase; }
            #page-footer { position: running(footer); border-top: 1px solid #D6D3F7; padding-top: 6px; width: 100%;
                           font-size: 8px; color: #888888; font-family: sans-serif; }
            #page-footer .page-count { float: right; }
            .pagenumber:before { content: counter(page); }
            .pagecount:before { content: counter(pages); }
            body { font-family: sans-serif; font-size: 11px; color: #1B1E2B; }
            .doc-title { font-size: 20px; margin: 0 0 14px 0; color: #1B1E2B; }
            .section-title { font-size: 14px; margin: 24px 0 8px 0; color: #1B1E2B; border-top: 1px solid #DFE2ED; padding-top: 14px; }
            .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 18px; font-size: 10px; }
            .meta-table th { text-align: left; color: #565F7A; width: 90px; padding: 4px 8px 4px 0; vertical-align: top; }
            .meta-table td { padding: 4px 0; }
            .status-pill { background: #ECEBFB; color: #4338CA; padding: 2px 8px; border-radius: 8px; font-size: 9px; }
            .content { font-size: 11px; line-height: 1.6; }
            .content h1, .content h2, .content h3 { color: #1B1E2B; }
            .content table { border-collapse: collapse; width: 100%; margin: 10px 0; }
            .content table th, .content table td { border: 1px solid #DFE2ED; padding: 5px 8px; font-size: 10px; }
            .content table th { background: #EDEFF7; text-align: left; }
            .content code { font-family: monospace; background: #EDEFF7; padding: 1px 4px; border-radius: 3px; font-size: 10px; }
            .content pre { background: #12141C; color: #E4E6F2; padding: 10px; border-radius: 6px; font-family: monospace; font-size: 9px; white-space: pre-wrap; }
            .content pre code { background: none; padding: 0; color: inherit; }
            .steps-table { width: 100%; border-collapse: collapse; font-size: 9px; }
            .steps-table th, .steps-table td { border: 1px solid #DFE2ED; padding: 5px 7px; text-align: left; vertical-align: top; }
            .steps-table th { background: #EDEFF7; }
            """;
}
