package com.proyecto.pedidos.service;

import com.proyecto.pedidos.exception.RecursoNoEncontradoException;
import com.proyecto.pedidos.model.Producto;
import com.proyecto.pedidos.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> listar() {
        return repository.findAll();
    }

    public Producto obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Producto no encontrado con id: " + id
                        )
                );
    }

    public Producto guardar(Producto producto) {

        if (producto.getStock() == null) {
            producto.setStock(0);
        }

        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }

        return repository.save(producto);
    }

    public Producto actualizar(Long id, Producto datos) {

        Producto producto = obtenerPorId(id);

        producto.setNombre(datos.getNombre());
        producto.setDescripcion(datos.getDescripcion());
        producto.setCategoria(datos.getCategoria());
        producto.setMaterial(datos.getMaterial());
        producto.setColor(datos.getColor());
        producto.setImagenUrl(datos.getImagenUrl());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());

        if (datos.getActivo() != null) {
            producto.setActivo(datos.getActivo());
        }

        return repository.save(producto);
    }

    public void eliminar(Long id) {
        Producto producto = obtenerPorId(id);
        repository.delete(producto);
    }
}
