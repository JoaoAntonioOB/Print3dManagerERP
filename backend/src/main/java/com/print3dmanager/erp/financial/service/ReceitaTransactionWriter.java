package com.print3dmanager.erp.financial.service;

import com.print3dmanager.erp.financial.model.FinancialTransaction;
import com.print3dmanager.erp.financial.repository.FinancialTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isola o INSERT da receita de faturamento em uma transação própria
 * ({@code REQUIRES_NEW}), separada da transação chamadora (que pode ser a
 * de {@code OrderBillingService.faturar}/{@code gerarReceitaSeNecessario}
 * ou, no caso do faturamento automático, a mesma transação de
 * {@code OrderService.alterarStatus}).
 *
 * <p>Isso é necessário porque, no PostgreSQL, uma violação de constraint
 * (aqui, a única parcial de {@code transacoes_financeiras} da migração
 * V12) deixa a transação corrente em estado abortado — qualquer comando
 * subsequente, incluindo o COMMIT final, seria silenciosamente tratado
 * como ROLLBACK. Se o INSERT rodasse na mesma transação da mudança de
 * status do pedido para ENTREGUE, uma colisão rara aqui derrubaria essa
 * mudança de status junto, sem erro visível ao usuário. Com
 * {@code REQUIRES_NEW}, só esta transação pequena é abortada; a
 * transação chamadora segue saudável e pode decidir o que fazer com o
 * conflito (ver {@code OrderBillingService}).</p>
 */
@Service
@RequiredArgsConstructor
class ReceitaTransactionWriter {

    private final FinancialTransactionRepository financialTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    FinancialTransaction salvar(FinancialTransaction receita) {
        return financialTransactionRepository.save(receita);
    }
}
