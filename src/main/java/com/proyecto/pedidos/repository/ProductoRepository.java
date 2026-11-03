package com.proyecto.pedidos.repository;

import com.proyecto.pedidos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.id=:id")
    java.util.Optional<Producto> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}