package com.print3dmanager.erp.report.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder de relatórios PDF em A4 com identidade única: cabeçalho com o nome
 * do sistema, título, período e data de geração; tabela de dados com colunas
 * numéricas alinhadas à direita; bloco de totais; rodapé com numeração de
 * páginas. Os services de relatório só descrevem o conteúdo — todo o layout
 * OpenPDF fica concentrado aqui.
 */
public class PdfReportBuilder {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'");

    private static final Font FONTE_SISTEMA =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
    private static final Font FONTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font FONTE_SUBTITULO =
            FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font FONTE_CABECALHO =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONTE_CELULA = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font FONTE_TOTAL_ROTULO =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONTE_RODAPE =
            FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final Font FONTE_VAZIO =
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.DARK_GRAY);

    private static final Color COR_CABECALHO = new Color(52, 73, 94);
    private static final Color COR_LINHA_ALTERNADA = new Color(242, 244, 246);

    private final String titulo;
    private LocalDate periodoDe;
    private LocalDate periodoAte;
    private List<String> cabecalhos;
    private float[] larguras;
    private boolean[] colunasNumericas;
    private final List<List<String>> linhas = new ArrayList<>();
    private final List<String[]> totais = new ArrayList<>();

    public PdfReportBuilder(String titulo) {
        this.titulo = titulo;
    }

    public PdfReportBuilder comPeriodo(LocalDate de, LocalDate ate) {
        this.periodoDe = de;
        this.periodoAte = ate;
        return this;
    }

    /**
     * Define as colunas da tabela de dados. {@code larguras} são proporções
     * relativas; {@code colunasNumericas} marca as alinhadas à direita.
     */
    public PdfReportBuilder comColunas(List<String> cabecalhos, float[] larguras,
                                       boolean[] colunasNumericas) {
        this.cabecalhos = cabecalhos;
        this.larguras = larguras;
        this.colunasNumericas = colunasNumericas;
        return this;
    }

    public PdfReportBuilder comLinha(List<String> valores) {
        linhas.add(valores);
        return this;
    }

    public PdfReportBuilder comLinhas(List<List<String>> valores) {
        linhas.addAll(valores);
        return this;
    }

    /** Acrescenta um par rótulo → valor ao bloco de totais do fim do relatório. */
    public PdfReportBuilder comTotal(String rotulo, String valor) {
        totais.add(new String[] {rotulo, valor});
        return this;
    }

    public byte[] gerar() {
        Document documento = new Document(PageSize.A4, 40, 40, 40, 48);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(documento, saida);
            documento.setFooter(montarRodape());
            documento.open();
            adicionarCabecalho(documento);
            adicionarConteudo(documento);
            adicionarTotais(documento);
            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do relatório.", e);
        }
        return saida.toByteArray();
    }

    private HeaderFooter montarRodape() {
        HeaderFooter rodape = new HeaderFooter(
                new Phrase("Print3D Manager ERP — página ", FONTE_RODAPE), true);
        rodape.setBorder(Rectangle.TOP);
        rodape.setBorderColor(Color.LIGHT_GRAY);
        rodape.setAlignment(Element.ALIGN_RIGHT);
        return rodape;
    }

    private void adicionarCabecalho(Document documento) {
        Paragraph sistema = new Paragraph("PRINT3D MANAGER ERP", FONTE_SISTEMA);
        documento.add(sistema);

        Paragraph paragrafoTitulo = new Paragraph(titulo, FONTE_TITULO);
        paragrafoTitulo.setSpacingAfter(2);
        documento.add(paragrafoTitulo);

        StringBuilder subtitulo = new StringBuilder();
        if (periodoDe != null && periodoAte != null) {
            subtitulo.append("Período: ").append(FORMATO_DATA.format(periodoDe))
                    .append(" a ").append(FORMATO_DATA.format(periodoAte)).append("   —   ");
        }
        subtitulo.append("Gerado em ")
                .append(FORMATO_DATA_HORA.format(LocalDateTime.now(ZoneOffset.UTC)));
        Paragraph paragrafoSubtitulo = new Paragraph(subtitulo.toString(), FONTE_SUBTITULO);
        paragrafoSubtitulo.setSpacingAfter(14);
        documento.add(paragrafoSubtitulo);
    }

    private void adicionarConteudo(Document documento) {
        if (linhas.isEmpty()) {
            Paragraph vazio = new Paragraph("Nenhum registro no período.", FONTE_VAZIO);
            vazio.setSpacingBefore(8);
            documento.add(vazio);
            return;
        }

        PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);

        for (String cabecalho : cabecalhos) {
            PdfPCell celula = new PdfPCell(new Phrase(cabecalho, FONTE_CABECALHO));
            celula.setBackgroundColor(COR_CABECALHO);
            celula.setPadding(5);
            celula.setBorderColor(Color.LIGHT_GRAY);
            tabela.addCell(celula);
        }

        boolean alternada = false;
        for (List<String> linha : linhas) {
            for (int i = 0; i < linha.size(); i++) {
                PdfPCell celula = new PdfPCell(new Phrase(linha.get(i), FONTE_CELULA));
                celula.setPadding(4);
                celula.setBorderColor(Color.LIGHT_GRAY);
                if (alternada) {
                    celula.setBackgroundColor(COR_LINHA_ALTERNADA);
                }
                if (colunasNumericas[i]) {
                    celula.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                tabela.addCell(celula);
            }
            alternada = !alternada;
        }
        documento.add(tabela);
    }

    private void adicionarTotais(Document documento) {
        if (totais.isEmpty()) {
            return;
        }
        PdfPTable tabela = new PdfPTable(new float[] {3, 1});
        tabela.setWidthPercentage(45);
        tabela.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.setSpacingBefore(12);
        for (String[] total : totais) {
            PdfPCell rotulo = new PdfPCell(new Phrase(total[0], FONTE_TOTAL_ROTULO));
            rotulo.setBorder(Rectangle.NO_BORDER);
            rotulo.setPadding(3);
            tabela.addCell(rotulo);
            PdfPCell valor = new PdfPCell(new Phrase(total[1], FONTE_CELULA));
            valor.setBorder(Rectangle.NO_BORDER);
            valor.setPadding(3);
            valor.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabela.addCell(valor);
        }
        documento.add(tabela);
    }
}
