package com.proyecto.pedidos.service;

import com.proyecto.pedidos.model.Cliente;
import com.proyecto.pedidos.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import com.proyecto.pedidos.exception.ConflictoException;
import com.proyecto.pedidos.exception.RecursoNoEncontradoException;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listar() {
        return repository.findAll();
    }

    public Cliente obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Cliente no encontrado" + id));
    }

    public Cliente guardar(Cliente cliente) {

        if (repository.existsByDocumento(cliente.getDocumento())) {
            throw new ConflictoException(
                    "Ya existe un cliente con el documento "
                            + cliente.getDocumento()
            );
        }

        return repository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente datos) {

        Cliente cliente = obtenerPorId(id);

        cliente.setNombres(datos.getNombres());
        cliente.setDocumento(datos.getDocumento());
        cliente.setTelefono(datos.getTelefono());
        cliente.setEmail(datos.getEmail());

        return repository.save(cliente);
    }

    public void eliminar(Long id) {
        Cliente cliente = obtenerPorId(id);
        repository.delete(cliente);
    }
}