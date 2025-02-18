package com.application.vaccine_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.application.vaccine_system.model.Cashier;

public interface CashierRepository extends JpaRepository<Cashier, Long>, JpaSpecificationExecutor<Cashier> {
}
