package com.proyecto.pedidos.service;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.proyecto.pedidos.dto.InterpretacionMensajeResponse;
import com.proyecto.pedidos.dto.InterpretarMensajeRequest;
import com.proyecto.pedidos.dto.ItemInterpretadoResponse;
import com.proyecto.pedidos.dto.ia.AiInterpretacionPedido;
import com.proyecto.pedidos.dto.ia.AiItemPedido;
import com.proyecto.pedidos.exception.NegocioException;
import com.proyecto.pedidos.exception.RecursoNoEncontradoException;
import com.proyecto.pedidos.exception.ServicioIaException;
import com.proyecto.pedidos.model.Cliente;
import com.proyecto.pedidos.model.Producto;
import com.proyecto.pedidos.repository.ClienteRepository;
import com.proyecto.pedidos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InterpretacionPedidoService {

    private static final double UMBRAL_CONFIANZA = 0.72;

    private static final String INSTRUCCIONES = """
            Eres el componente de comprensión de mensajes de Pao Collection,
            una tienda de joyería y accesorios.

            Tu única función es interpretar el mensaje del cliente y asociarlo
            con PRODUCTOS REALES del catálogo suministrado.

            REGLAS OBLIGATORIAS:
            1. Nunca inventes productos, IDs, precios, stock, descuentos ni variantes.
            2. Solo puedes devolver productoId que aparezca literalmente en el catálogo.
            3. Si el cliente pide algo ambiguo (por ejemplo "algo dorado bonito")
               y existen varias posibilidades, marca requiereRevision=true.
            4. Si no existe un producto suficientemente compatible, usa productoId=0
               para ese elemento y marca requiereRevision=true.
            5. Extrae la cantidad indicada. Si el producto está claro pero no existe
               una cantidad explícita, usa cantidad=1.
            6. La confianza debe estar entre 0 y 1.
            7. CREAR_PEDIDO se usa cuando existe intención clara de comprar/pedir/separar.
               CONSULTA_PRODUCTO se usa para preguntas de disponibilidad, precio,
               características o recomendaciones sin intención clara de compra.
               OTRO para mensajes ajenos a esas situaciones.
            8. No calcules el total. El backend lo hará con los datos reales.
            9. Si requiereRevision=false, aclaracion debe ser una cadena vacía.
            10. No ejecutes ninguna acción: únicamente interpreta.
            """;

    private final OpenAIClient openAIClient;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final String modelo;

    public InterpretacionPedidoService(
            OpenAIClient openAIClient,
            ProductoRepository productoRepository,
            ClienteRepository clienteRepository,
            @Value("${openai.model:gpt-5.6-luna}") String modelo) {

        this.openAIClient = openAIClient;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.modelo = modelo;
    }

    public InterpretacionMensajeResponse interpretar(InterpretarMensajeRequest request) {

        validarRequest(request);

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cliente no encontrado con id: " + request.getClienteId()
                ));

        List<Producto> catalogo = productoRepository.findAll().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .toList();

        if (catalogo.isEmpty()) {
            throw new NegocioException(
                    "No existen productos activos en el catálogo para analizar el mensaje."
            );
        }

        AiInterpretacionPedido interpretacionAi =
                consultarModelo(request.getMensaje(), catalogo);

        return validarYConvertir(
                cliente,
                request.getMensaje(),
                catalogo,
                interpretacionAi
        );
    }

    private void validarRequest(InterpretarMensajeRequest request) {

        if (request.getClienteId() == null) {
            throw new NegocioException(
                    "Selecciona un cliente antes de analizar el mensaje."
            );
        }

        if (request.getMensaje() == null || request.getMensaje().isBlank()) {
            throw new NegocioException("El mensaje no puede estar vacío.");
        }

        if (request.getMensaje().length() > 2000) {
            throw new NegocioException(
                    "El mensaje es demasiado largo para el simulador."
            );
        }
    }

    private AiInterpretacionPedido consultarModelo(
            String mensaje,
            List<Producto> catalogo) {

        String entrada = construirEntrada(mensaje, catalogo);

        try {
            StructuredResponseCreateParams<AiInterpretacionPedido> params =
                    ResponseCreateParams.builder()
                            .model(modelo)
                            .instructions(INSTRUCCIONES)
                            .input(entrada)
                            .text(AiInterpretacionPedido.class)
                            .maxOutputTokens(900)
                            .build();

            return openAIClient.responses()
                    .create(params)
                    .output()
                    .stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(messageResponse -> messageResponse.content().stream())
                    .flatMap(content -> content.outputText().stream())
                    .findFirst()
                    .orElseThrow(() -> new ServicioIaException(
                            "La IA no devolvió una interpretación estructurada."
                    ));

        } catch (ServicioIaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServicioIaException(
                    "No se pudo consultar el servicio de IA. "
                    + "Verifica OPENAI_API_KEY, conexión a Internet y disponibilidad del modelo.",
                    ex
            );
        }
    }

    private String construirEntrada(String mensaje, List<Producto> catalogo) {

        StringBuilder sb = new StringBuilder();

        sb.append("CATÁLOGO AUTORIZADO DE PAO COLLECTION:\n");

        for (Producto p : catalogo) {
            sb.append("- ID=").append(p.getId())
                    .append(" | nombre=").append(valor(p.getNombre()))
                    .append(" | categoria=").append(valor(p.getCategoria()))
                    .append(" | material=").append(valor(p.getMaterial()))
                    .append(" | color=").append(valor(p.getColor()))
                    .append("\n");
        }

        sb.append("\nMENSAJE DEL CLIENTE:\n");
        sb.append(mensaje);

        return sb.toString();
    }

    private InterpretacionMensajeResponse validarYConvertir(
            Cliente cliente,
            String mensajeOriginal,
            List<Producto> catalogo,
            AiInterpretacionPedido ai) {

        if (ai == null) {
            throw new ServicioIaException("La IA devolvió una respuesta vacía.");
        }

        Map<Long, Producto> productosPorId = catalogo.stream()
                .collect(Collectors.toMap(
                        Producto::getId,
                        Function.identity()
                ));

        Map<Long, Acumulado> acumulados = new LinkedHashMap<>();
        List<String> observaciones = new ArrayList<>();

        boolean requiereRevision = ai.requiereRevision;

        List<AiItemPedido> itemsAi =
                ai.items == null ? List.of() : ai.items;

        for (AiItemPedido itemAi : itemsAi) {

            if (itemAi == null) {
                requiereRevision = true;
                continue;
            }

            if (itemAi.productoId <= 0) {
                requiereRevision = true;

                observaciones.add(
                        "La IA detectó una referencia a producto que no pudo asociar "
                        + "con seguridad al catálogo."
                );
                continue;
            }

            Producto producto = productosPorId.get(itemAi.productoId);

            // Defensa contra alucinaciones: un ID inventado jamás llega al pedido.
            if (producto == null) {
                requiereRevision = true;

                observaciones.add(
                        "La IA devolvió un producto que no pertenece al catálogo autorizado."
                );
                continue;
            }

            int cantidad = Math.max(1, itemAi.cantidad);

            double confianza = Math.max(
                    0.0,
                    Math.min(1.0, itemAi.confianza)
            );

            if (confianza < UMBRAL_CONFIANZA) {
                requiereRevision = true;

                observaciones.add(
                        "La asociación con " + producto.getNombre()
                        + " tiene confianza baja ("
                        + Math.round(confianza * 100)
                        + "%)."
                );
            }

            acumulados.compute(
                    producto.getId(),
                    (id, actual) -> {
                        if (actual == null) {
                            return new Acumulado(
                                    producto,
                                    cantidad,
                                    confianza
                            );
                        }

                        return new Acumulado(
                                producto,
                                actual.cantidad() + cantidad,
                                Math.max(actual.confianza(), confianza)
                        );
                    }
            );
        }

        List<ItemInterpretadoResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Acumulado acumulado : acumulados.values()) {

            Producto producto = acumulado.producto();
            int cantidad = acumulado.cantidad();

            BigDecimal subtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(cantidad));

            if (producto.getStock() < cantidad) {
                requiereRevision = true;

                observaciones.add(
                        "Stock insuficiente para " + producto.getNombre()
                        + ". Solicitado: " + cantidad
                        + ", disponible: " + producto.getStock() + "."
                );
            }

            items.add(new ItemInterpretadoResponse(
                    producto.getId(),
                    producto.getNombre(),
                    producto.getCategoria(),
                    producto.getMaterial(),
                    producto.getColor(),
                    cantidad,
                    producto.getPrecio(),
                    producto.getStock(),
                    subtotal,
                    acumulado.confianza()
            ));

            total = total.add(subtotal);
        }

        String intencion = normalizarIntencion(ai.intencion);

        if (!"CREAR_PEDIDO".equals(intencion)) {
            requiereRevision = true;
        }

        if (items.isEmpty()) {
            requiereRevision = true;

            observaciones.add(
                    "No se identificó una joya válida del catálogo."
            );
        }

        if (ai.aclaracion != null && !ai.aclaracion.isBlank()) {
            observaciones.add("Aclaración sugerida por IA: " + ai.aclaracion);
        }

        String respuestaSugerida = construirRespuesta(
                cliente,
                intencion,
                items,
                total,
                requiereRevision,
                ai.aclaracion
        );

        return new InterpretacionMensajeResponse(
                cliente.getId(),
                cliente.getNombres(),
                mensajeOriginal,
                intencion,
                items,
                total,
                requiereRevision,
                observaciones,
                respuestaSugerida
        );
    }

    private String construirRespuesta(
            Cliente cliente,
            String intencion,
            List<ItemInterpretadoResponse> items,
            BigDecimal total,
            boolean requiereRevision,
            String aclaracion) {

        if (requiereRevision) {
            if (aclaracion != null && !aclaracion.isBlank()) {
                return "Hola " + cliente.getNombres() + ". " + aclaracion;
            }

            return "Hola " + cliente.getNombres()
                    + ". Entendí tu mensaje, pero necesito revisar algunos detalles "
                    + "antes de confirmar el pedido.";
        }

        String detalle = items.stream()
                .map(item ->
                        item.getCantidad() + " x " + item.getNombreProducto()
                )
                .collect(Collectors.joining(", "));

        return "Hola " + cliente.getNombres()
                + ". Identifiqué: " + detalle
                + ". Total estimado: S/ " + total
                + ". ¿Deseas confirmar tu pedido?";
    }

    private String normalizarIntencion(String valor) {

        if (valor == null) {
            return "OTRO";
        }

        String normalizado = valor
                .trim()
                .toUpperCase(Locale.ROOT);

        return switch (normalizado) {
            case "CREAR_PEDIDO", "CONSULTA_PRODUCTO", "OTRO" -> normalizado;
            default -> "OTRO";
        };
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    private record Acumulado(
            Producto producto,
            int cantidad,
            double confianza) {
    }
}
