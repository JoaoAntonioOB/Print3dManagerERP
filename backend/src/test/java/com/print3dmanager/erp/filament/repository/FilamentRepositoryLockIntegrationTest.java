package com.print3dmanager.erp.filament.repository;

import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.model.FilamentMaterial;
import com.print3dmanager.erp.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra um Postgres real (Testcontainers), que
 * {@code findByIdForUpdate} de fato serializa o acesso concorrente à mesma
 * linha: a segunda transação só consegue ler/travar o filamento depois que a
 * primeira libera o lock (commit), o que é exatamente o que impede o
 * lost update de estoque sob concorrência (achado ALTO #4).
 */
class FilamentRepositoryLockIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FilamentRepository filamentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("findByIdForUpdate: segunda transação espera a primeira liberar o lock da linha")
    void findByIdForUpdateSerializaAcessoConcorrente() throws Exception {
        Filament filamento = new Filament();
        filamento.setNome("PLA Lock Test");
        filamento.setMaterial(FilamentMaterial.PLA);
        filamento.setCustoPorKg(new BigDecimal("90.00"));
        filamento.setQuantidadeEstoqueG(new BigDecimal("500.00"));
        Long id = filamentRepository.saveAndFlush(filamento).getId();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch primeiraTransacaoTravouLinha = new CountDownLatch(1);
        AtomicLong instanteLiberacao = new AtomicLong(-1);
        AtomicLong instanteAquisicaoSegunda = new AtomicLong(-1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread A: trava a linha, segura por um tempo e só então libera (commit).
            Future<?> tarefaA = executor.submit(() -> tx.executeWithoutResult(status -> {
                filamentRepository.findByIdForUpdate(id).orElseThrow();
                primeiraTransacaoTravouLinha.countDown();
                sleepSilenciosamente(400);
                instanteLiberacao.set(System.nanoTime());
            }));

            // Thread B: só tenta travar depois que A já travou; deve bloquear até A commitar.
            Future<?> tarefaB = executor.submit(() -> {
                await(primeiraTransacaoTravouLinha);
                tx.executeWithoutResult(status -> {
                    filamentRepository.findByIdForUpdate(id).orElseThrow();
                    instanteAquisicaoSegunda.set(System.nanoTime());
                });
            });

            tarefaA.get(10, TimeUnit.SECONDS);
            tarefaB.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(instanteLiberacao.get()).isPositive();
        assertThat(instanteAquisicaoSegunda.get()).isPositive();
        // a segunda transação só conseguiu o lock depois que a primeira liberou
        assertThat(instanteAquisicaoSegunda.get()).isGreaterThanOrEqualTo(instanteLiberacao.get());
    }

    private static void sleepSilenciosamente(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
