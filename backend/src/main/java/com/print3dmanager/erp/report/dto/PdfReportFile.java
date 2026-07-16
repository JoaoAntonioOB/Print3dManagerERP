package com.print3dmanager.erp.report.dto;

/** Relatório PDF pronto para download: nome do arquivo + bytes do documento. */
public record PdfReportFile(String nomeArquivo, byte[] conteudo) {
}
