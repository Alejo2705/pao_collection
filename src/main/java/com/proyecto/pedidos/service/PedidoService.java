package com.proyecto.pedidos.service;

import com.proyecto.pedidos.dto.CrearPedidoRequest;
import com.proyecto.pedidos.dto.ItemPedidoRequest;
import com.proyecto.pedidos.model.*;
import com.proyecto.pedidos.repository.ClienteRepository;
import com.proyecto.pedidos.repository.PedidoRepository;
import com.proyecto.pedidos.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.proyecto.pedidos.dto.ClienteResumenResponse;
import com.proyecto.pedidos.dto.DetallePedidoResponse;
import com.proyecto.pedidos.dto.PedidoResponse;
import com.proyecto.pedidos.dto.ProductoResumenResponse;
import com.proyecto.pedidos.exception.NegocioException;
import com.proyecto.pedidos.exception.RecursoNoEncontradoException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository) {

        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
    }

    public List<Pedido> listar() {
        return pedidoRepository.findAllByOrderByFechaDesc();
    }

    public Pedido obtenerPorId(Long id) {
        return pedidoRepository.findPedidoById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Pedido no encontrado" + id));
    }

    @Transactional
    public Pedido crear(CrearPedidoRequest request) {

        if (request.getClienteId() == null) {
            throw new NegocioException("Debe indicar un cliente");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new NegocioException(
                    "El pedido debe contener al menos un producto"
            );
        }

        Cliente cliente = clienteRepository
                .findById(request.getClienteId())
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Cliente no encontrado con id:" + request.getClienteId()));

        Pedido pedido = new Pedido();

        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(BigDecimal.ZERO);

        BigDecimal totalPedido = BigDecimal.ZERO;

        for (ItemPedidoRequest item : request.getItems()) {

            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new NegocioException(
                        "La cantidad debe ser mayor a cero"
                );
            }

            Producto producto = productoRepository
                    .findById(item.getProductoId())
                    .orElseThrow(() ->
                            new RecursoNoEncontradoException(
                                    "Producto no encontrado con id: "
                                            + item.getProductoId()
                            )
                    );

            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new NegocioException(
                        "El producto " + producto.getNombre()
                                + " no está disponible"
                );
            }

            if (producto.getStock() < item.getCantidad()) {
                throw new NegocioException(
                        "Stock insuficiente para: "
                                + producto.getNombre()
                                +". Stock disponible: "
                                + producto.getStock()
                );
            }

            BigDecimal subtotal = producto
                    .getPrecio()
                    .multiply(
                            BigDecimal.valueOf(item.getCantidad())
                    );

            DetallePedido detalle = new DetallePedido();

            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotal);

            pedido.getDetalles().add(detalle);

            totalPedido = totalPedido.add(subtotal);

            producto.setStock(
                    producto.getStock() - item.getCantidad()
            );

            productoRepository.save(producto);
        }

        pedido.setTotal(totalPedido);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cambiarEstado(
            Long id,
            EstadoPedido nuevoEstado) {

        Pedido pedido = obtenerPorId(id);

        pedido.setEstado(nuevoEstado);

        return pedidoRepository.save(pedido);
    }

    public PedidoResponse convertirAResponse(Pedido pedido) {

    ClienteResumenResponse clienteResponse =
            new ClienteResumenResponse(
                    pedido.getCliente().getId(),
                    pedido.getCliente().getNombres(),
                    pedido.getCliente().getDocumento()
            );

    List<DetallePedidoResponse> detalles =
            pedido.getDetalles()
                    .stream()
                    .map(detalle -> {

                        ProductoResumenResponse producto =
                                new ProductoResumenResponse(
                                        detalle.getProducto().getId(),
                                        detalle.getProducto().getNombre(),
                                        detalle.getProducto().getPrecio()
                                );

                        return new DetallePedidoResponse(
                                detalle.getId(),
                                producto,
                                detalle.getCantidad(),
                                detalle.getPrecioUnitario(),
                                detalle.getSubtotal()
                        );
                    })
                    .toList();

    return new PedidoResponse(
            pedido.getId(),
            clienteResponse,
            pedido.getFecha(),
            pedido.getEstado(),
            pedido.getTotal(),
            detalles
    );
}

public List<PedidoResponse> listarResponses() {

    return pedidoRepository
            .findAllByOrderByFechaDesc()
            .stream()
            .map(this::convertirAResponse)
            .toList();
}

public PedidoResponse obtenerResponsePorId(Long id) {

    Pedido pedido = obtenerPorId(id);

    return convertirAResponse(pedido);
}
}
