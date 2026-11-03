package com.proyecto.pedidos.repository;
import com.proyecto.pedidos.model.WebChat;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface WebChatRepository extends JpaRepository<WebChat,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from WebChat c where c.id=:id")
    Optional<WebChat> bloquear(@Param("id") String id);
    List<WebChat> findTop100ByOrderByActualizadoDesc();
}
