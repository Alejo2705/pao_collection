package com.proyecto.pedidos.repository;

import com.proyecto.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {
            "cliente",
            "detalles",
            "detalles.producto"
    })
    List<Pedido> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = {
            "cliente",
            "detalles",
            "detalles.producto"
    })
    Optional<Pedido> findPedidoById(Long id);
}