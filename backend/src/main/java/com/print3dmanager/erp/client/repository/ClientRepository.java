package com.print3dmanager.erp.client.repository;

import com.print3dmanager.erp.client.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientRepository
        extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    boolean existsByCpfCnpj(String cpfCnpj);

    boolean existsByCpfCnpjAndIdNot(String cpfCnpj, Long id);
}
