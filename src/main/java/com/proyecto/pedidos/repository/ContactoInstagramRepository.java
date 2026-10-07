package com.proyecto.pedidos.repository;

import com.proyecto.pedidos.model.ContactoInstagram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactoInstagramRepository
        extends JpaRepository<ContactoInstagram, Long> {

    Optional<ContactoInstagram> findByInstagramScopedId(String instagramScopedId);

    List<ContactoInstagram> findAllByOrderByCreadoEnDesc();
}
