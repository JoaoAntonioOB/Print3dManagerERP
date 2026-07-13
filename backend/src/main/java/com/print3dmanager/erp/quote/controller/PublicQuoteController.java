package com.print3dmanager.erp.quote.controller;

import com.print3dmanager.erp.quote.dto.PublicQuoteResponse;
import com.print3dmanager.erp.quote.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Link público de orçamentos: o cliente acessa pelo shareToken, sem login
 * (rota /public/** liberada no SecurityConfig). Orçamentos em RASCUNHO
 * não são visíveis e a validade vencida expira o link automaticamente.
 */
@RestController
@RequestMapping("/public/quotes")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Orçamentos (público)",
        description = "Visualização e aprovação de orçamentos pelo cliente, sem login")
public class PublicQuoteController {

    private final QuoteService quoteService;

    @GetMapping("/{shareToken}")
    @Operation(summary = "Visualiza um orçamento pelo link público",
            description = "Visão do cliente: preço proposto, sem custos internos nem markup.")
    public PublicQuoteResponse visualizar(@PathVariable String shareToken) {
        return quoteService.buscarPublico(shareToken);
    }

    @PostMapping("/{shareToken}/aprovar")
    @Operation(summary = "Aprova o orçamento (ação do cliente)",
            description = "Permitido apenas com status ENVIADO e dentro da validade.")
    public PublicQuoteResponse aprovar(@PathVariable String shareToken) {
        return quoteService.aprovarPublico(shareToken);
    }

    @PostMapping("/{shareToken}/recusar")
    @Operation(summary = "Recusa o orçamento (ação do cliente)",
            description = "Permitido apenas com status ENVIADO e dentro da validade.")
    public PublicQuoteResponse recusar(@PathVariable String shareToken) {
        return quoteService.recusarPublico(shareToken);
    }
}
