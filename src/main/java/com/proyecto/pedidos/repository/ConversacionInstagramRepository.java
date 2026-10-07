package com.proyecto.pedidos.repository;

import com.proyecto.pedidos.model.ConversacionInstagram;
import com.proyecto.pedidos.model.ContactoInstagram;
import com.proyecto.pedidos.model.EstadoConversacionInstagram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversacionInstagramRepository
        extends JpaRepository<ConversacionInstagram, Long> {

    boolean existsByInstagramMessageId(String instagramMessageId);

    Optional<ConversacionInstagram>
    findFirstByContactoAndEstadoOrderByFechaDesc(
            ContactoInstagram contacto,
            EstadoConversacionInstagram estado
    );

    List<ConversacionInstagram> findTop100ByOrderByFechaDesc();
}
