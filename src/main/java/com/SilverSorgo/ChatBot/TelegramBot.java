package com.SilverSorgo.ChatBot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final ChatClient chatClient;
    private final MenuService menuService;
    private final CartService cartService;
    private final String adminChatId;
    private final String adminAlias;
    private final String adminCvu;
    private final String adminTitular;
    private Map<Long, String> userStates = new ConcurrentHashMap<>();
    private Map<String, Pedido> pedidosPendientes = new ConcurrentHashMap<>();
    private java.util.concurrent.atomic.AtomicInteger orderCounter =
            new java.util.concurrent.atomic.AtomicInteger(0);
    private Map<Long, Pedido> pedidosEnProceso = new ConcurrentHashMap<>();
    private volatile boolean isStoreOpen = true;
    public TelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.admin.chat-id}") String adminChatId,
            @Value("${telegram.admin.alias}") String adminAlias,
            @Value("${telegram.admin.cvu}") String adminCvu,
            @Value("${telegram.admin.titular}") String adminTitular,
            ChatClient.Builder chatClientBuilder,
            MenuService menuService,
            CartService cartService
    ) {
        super(botToken);
        this.botUsername = botUsername;
        this.menuService = menuService;
        this.cartService = cartService;
        this.adminChatId = adminChatId;
        this.adminAlias = adminAlias;
        this.adminCvu = adminCvu;
        this.adminTitular = adminTitular;

        String systemPrompt = """
                Sos un asistente virtual de atención al cliente para '%s'.
                Tu trabajo es ser amable, responder dudas sobre el menú y charlar.
                
                REGLAS DE ORO SOBRE COMANDOS:
                1. Si el usuario quiere VER el menú (leer, chusmear, saber qué hay),
                   tenes que decirle que use el comando: /menu
                
                2. Si el usuario quiere INICIAR un pedido (comprar, agregar al carrito),
                   tenes que decirle que use el comando: /realizar_pedido
                
                3. Si el usuario quiere ver su CARRITO (ver qué agregó, editar, finalizar),
                   tenes que decirle que use el comando: /carrito
                
                NUNCA intentes procesar un pedido hablando. Solo redirige a esos comandos.
                Si te preguntan, el horario de atención es de 20:00PM hasta 01:30AM
                Este es el menú (solo para tus consultas):
                %s
                """.formatted(menuService.getMenu().getNombreLocal(), menuService.getMenuComoTexto());

        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .build();
        registrarBot();
    }

    private void registrarBot() {
        System.out.println("===============================================");
        System.out.println("¡BOT CREADO! 🚀");
        System.out.println("Conectado como: " + this.botUsername);
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            System.out.println("Conectado con el bot y funcionando.");
            System.out.println("===============================================");
        } catch (TelegramApiException e) {
            System.err.println("\n!!! ERROR AL REGISTRAR EL BOT !!!");
            e.printStackTrace();
            System.err.println("===============================================\n");
        }
    }
    /**
     * Loguea cualquier update entrante a la consola, identificando al Admin.
     */
    private void logUpdate(Update update) {
        try {
            long chatId = 0;
            String userMessage = "";
            String userIdentifier = "";

            if (update.hasMessage() && update.getMessage().hasPhoto()) {
                chatId = update.getMessage().getChatId();
                userMessage = "[Foto] (Comprobante, ID: " + update.getMessage().getPhoto().get(0).getFileId() + ")";
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                chatId = update.getMessage().getChatId();
                userMessage = "[Texto] " + update.getMessage().getText();
            } else if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
                userMessage = "[Botón] " + update.getCallbackQuery().getData();
            } else {
                return; // No se loguean "updates" que no sean foto, texto o un boton
            }

            // Con esta función detecta si el que envía el mensaje/comando es el Jefe o un Cliente
            if (String.valueOf(chatId).equals(this.adminChatId)) {
                userIdentifier = "JEFE";
            } else {
                userIdentifier = "CLIENTE";
            }

            // El log que muestra en la consola
            System.out.println(String.format(">>> [%s] Chat %d | Mensaje: %s",
                    userIdentifier, chatId, userMessage));

        } catch (Exception e) {
            System.err.println("Error al loguear update: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {

        logUpdate(update); // Arranco el logueador

        try {
            if (!update.hasMessage()) {
                if (update.hasCallbackQuery()) {
                    handleCallbackQuery(update);
                }
                return;
            }

            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String currentState = userStates.get(chatId);

            if ("AWAITING_COMPROBANTE".equals(currentState)) {

                if (message.hasPhoto()) {

                    handlePhotoMessage(update);
                    return;

                } else {

                    enviarRespuesta(chatId, "❌ ¡Ojo! Estamos esperando tu comprobante. Por favor, **envía únicamente la imagen** de la transferencia.");
                    return;
                }
            }

            if (message.hasPhoto()) {
                handlePhotoMessage(update);
            } else if (message.hasText()) {
                handleTextMessage(update);

            } else {
                enviarRespuesta(chatId, "🤔 Solo puedo procesar comandos o mensajes de texto/fotos. Si quieres hacer un pedido, usa /realizar_pedido.");
            }

        } catch (Exception e) {
            System.err.println("Error no controlado en onUpdateReceived: " + e.getMessage());
        }
    }

    // Con esta función agrego el selector de cantidad en el momento de hacer un pedido
    private void enviarSelectorCantidad(long chatId, long messageId, String catId, String prodId, int quantity) {
        var optProd = menuService.getProducto(prodId);
        if (optProd.isEmpty()) {
            enviarRespuesta(chatId, "Error, producto no encontrado.");
            return;
        }
        Producto producto = optProd.get();
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId((int) messageId);
        editMessage.setText(String.format(
                "Producto: %s\nDescripción: %s\nPrecio: $%.2f\n\n¿Cuántos querés?",
                producto.getNombre(), producto.getDescripcion(), producto.getPrecio()
        ));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton btnMenos = new InlineKeyboardButton();
        btnMenos.setText("➖");
        btnMenos.setCallbackData("ADJUST:" + catId + ":" + prodId + ":" + (quantity - 1));

        InlineKeyboardButton btnCantidad = new InlineKeyboardButton();
        btnCantidad.setText(String.valueOf(quantity));
        btnCantidad.setCallbackData("NO_OP");

        InlineKeyboardButton btnMas = new InlineKeyboardButton();
        btnMas.setText("➕");
        btnMas.setCallbackData("ADJUST:" + catId + ":" + prodId + ":" + (quantity + 1));

        keyboard.add(List.of(btnMenos, btnCantidad, btnMas));

        InlineKeyboardButton btnConfirmar = new InlineKeyboardButton();
        btnConfirmar.setText("✅ Confirmar (" + quantity + ")");
        btnConfirmar.setCallbackData("CONFIRM_ADD:" + catId + ":" + prodId + ":" + quantity);

        keyboard.add(List.of(btnConfirmar));


        InlineKeyboardButton btnVolver = new InlineKeyboardButton();
        btnVolver.setText("⬅️ Cancelar y Volver");

        btnVolver.setCallbackData("BACK_TO_PROD:" + catId);

        keyboard.add(List.of(btnVolver));


        markup.setKeyboard(keyboard);
        editMessage.setReplyMarkup(markup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            System.err.println("Error al editar mensaje selector: " + e.getMessage());
        }
    }


    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        System.out.println(String.format(">>> [CLIENTE] Chat %d (Texto): %s", chatId, messageText));

        if (!isStoreOpen && !String.valueOf(chatId).equals(this.adminChatId)) {

            enviarRespuestaCerrado(chatId);
            return;
        }

        if (String.valueOf(chatId).equals(this.adminChatId)) {
            if (handleAdminCommands(chatId, messageText)) {
                return;
            }
        }


        String currentState = userStates.get(chatId);
        if ("AWAITING_ADDRESS".equals(currentState)) {

            userStates.remove(chatId);


            iniciarProcesoDePago(chatId, messageText);

            return;

        } else if ("AWAITING_CASH_AMOUNT".equals(currentState)) {

            userStates.remove(chatId);

            Pedido pedido = pedidosEnProceso.get(chatId);
            if (pedido == null) {
                enviarRespuesta(chatId, "Ups, hubo un error. Por favor, inicia tu pedido de nuevo con /realizar_pedido");
                return;
            }


            pedido.setMetodoDePago("Efectivo (Abona con: $" + messageText + ")");

            finalizarPedido(pedido);

            return;
        }

        if (messageText.startsWith("/menu")) {
            String menuEnTexto = menuService.getMenuComoTexto();
            enviarRespuesta(chatId, menuEnTexto + "\n\nPara empezar a comprar, usa /realizar_pedido");

        } else if (messageText.startsWith("/realizar_pedido")) {
            enviarMenuCategorias(chatId);

        } else if (messageText.startsWith("/carrito")) {
            mostrarCarrito(chatId);

        } else {
            String aiResponse = chatClient.prompt()
                    .user(messageText)
                    .call()
                    .content();
            enviarRespuesta(chatId, aiResponse);
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (!isStoreOpen && !String.valueOf(chatId).equals(this.adminChatId)) {
            enviarRespuestaCerrado(chatId);
            answerCallbackQuery(update.getCallbackQuery().getId(), "Tienda cerrada");
            return;
        }

        String[] dataParts = callbackData.split(":");
        String action = dataParts[0];

        switch (action) {
            case "CAT":
                enviarMenuProductos(chatId, messageId, dataParts[1]);
                break;

            case "PROD":
                enviarSelectorCantidad(chatId, messageId, dataParts[1], dataParts[2], 1);
                break;

            case "ADJUST":
                int newQuantity = Integer.parseInt(dataParts[3]);
                if (newQuantity < 1) newQuantity = 1;
                enviarSelectorCantidad(chatId, messageId, dataParts[1], dataParts[2], newQuantity);
                break;

            case "CONFIRM_ADD":
                String prodId = dataParts[2];
                int quantity = Integer.parseInt(dataParts[3]);
                cartService.addItem(chatId, prodId, quantity);
                enviarRespuesta(chatId, "✅ ¡" + quantity + "x agregado(s) al carrito! /carrito para ver, editar o finalizar el pedido");
                enviarMenuProductos(chatId, messageId, dataParts[1]);
                break;

            case "BACK_TO_PROD":
                enviarMenuProductos(chatId, messageId, dataParts[1]);
                break;

            case "VER_CARRITO":
                mostrarCarrito(chatId);
                break;

            case "MENU_PRINCIPAL":
                enviarMenuCategorias(chatId, messageId);
                break;

            case "EDIT_CART":
                mostrarMenuEdicion(chatId, messageId);
                break;

            case "REM":
                cartService.sacarItem(chatId, dataParts[1]);
                enviarRespuesta(chatId, "🗑️ Producto restado del carrito.");
                mostrarMenuEdicion(chatId, messageId);
                break;

            case "CONFIRM_ORDER":
                Map<String, Integer> carrito = cartService.getCart(chatId);
                if (carrito.isEmpty()) {
                    enviarRespuesta(chatId, "Tu carrito está vacío.");
                    answerCallbackQuery(update.getCallbackQuery().getId(), "Carrito vacío");
                    return;
                }

                String orderId = String.valueOf(orderCounter.get());
                Pedido nuevoPedido = new Pedido();
                nuevoPedido.setOrderId(orderId);
                nuevoPedido.setClientChatId(chatId);
                nuevoPedido.setItems(Map.copyOf(carrito));

                double total = 0;
                for (Map.Entry<String, Integer> entry : carrito.entrySet()) {
                    var optProd = menuService.getProducto(entry.getKey());
                    if (optProd.isPresent()) {
                        total += optProd.get().getPrecio() * entry.getValue();
                    }
                }
                nuevoPedido.setTotal(total);

                pedidosEnProceso.put(chatId, nuevoPedido);
                cartService.clearCart(chatId);
                userStates.put(chatId, "AWAITING_NAME");

                var message = update.getCallbackQuery().getMessage();
                EditMessageText msg = new EditMessageText();
                msg.setChatId(String.valueOf(chatId));
                msg.setMessageId((int) messageId);
                msg.setReplyMarkup(null);
                if (message instanceof org.telegram.telegrambots.meta.api.objects.Message) {
                    String cartText = ((org.telegram.telegrambots.meta.api.objects.Message) message).getText();
                    msg.setText(cartText + "\n\n-- (Pendiente de dirección...) --");
                } else {
                    msg.setText("--- 🛒 TU CARRITO ---\n\n(Pendiente de dirección...)");
                }
                try { execute(msg); } catch (TelegramApiException e) {}

                iniciarProcesoDeDireccion(chatId);
                break;

            case "PAY_TRANSFER":
                Pedido pedidoTransfer = pedidosEnProceso.get(chatId);
                if (pedidoTransfer == null) {
                    enviarRespuesta(chatId, "Hubo un error, por favor empieza de nuevo.");
                    return;
                }

                userStates.put(chatId, "AWAITING_COMPROBANTE");

                enviarRespuesta(chatId, String.format(
                        """
                        ¡Perfecto! El total es $%.2f.
                        
                        Por favor, transferí a:
                        Alias: `%s`
                        CVU: `%s`
                        Titular: %s
                        
                        ¡IMPORTANTE!
                        Para confirmar tu pedido, enviame la foto o captura del comprobante por este mismo chat.
                        """,
                        pedidoTransfer.getTotal(), this.adminAlias, this.adminCvu, this.adminTitular
                ));
                break;
            case "PAY_CASH":
                userStates.put(chatId, "AWAITING_CASH_AMOUNT");
                Pedido pedidoCash = pedidosEnProceso.get(chatId);
                enviarRespuesta(chatId, String.format(
                        "¡Genial! El total es $%.2f. \n\n¿Con cuánto vas a abonar en efectivo? " +
                                "(Por favor, ingresá solo el número, ej: 10000)",
                        pedidoCash.getTotal()
                ));
                break;

            case "NO_OP":
                break;

            default:
                System.err.println("Callback no reconocido: " + callbackData);
                enviarRespuesta(chatId, "Ese botón es viejo o no funciona.");
                break;
        }
    }
    private void handlePhotoMessage(Update update) {
        long chatId = update.getMessage().getChatId();


        String currentState = userStates.get(chatId);

        if ("AWAITING_COMPROBANTE".equals(currentState)) {

            userStates.remove(chatId); // Limpiamos el estado

            Pedido pedido = pedidosEnProceso.get(chatId);
            if (pedido == null) {
                enviarRespuesta(chatId, "Ups, hubo un error. Por favor, inicia tu pedido de nuevo con /realizar_pedido");
                return;
            }


            pedido.setMetodoDePago("Transferencia (Comprobante RECIBIDO)");

            finalizarPedido(pedido);

            ForwardMessage forwardMessage = new ForwardMessage();
            forwardMessage.setChatId(this.adminChatId); // El ID del Admin
            forwardMessage.setFromChatId(String.valueOf(chatId)); // El ID del Cliente
            forwardMessage.setMessageId(update.getMessage().getMessageId()); // El ID de la foto

            try {
                execute(forwardMessage);
            } catch (TelegramApiException e) {
                enviarRespuesta(Long.parseLong(this.adminChatId),
                        "El cliente del pedido #" + pedido.getOrderId() + " dice que envió un comprobante, pero no pude reenviártelo. Comunicate con él.");
            }
            enviarRespuesta(chatId,
                    "¡Comprobante recibido! Tu pedido (ID: #" + pedido.getOrderId() + ") fue confirmado. 🚀 En breve estará listo.");

        } else {
            // Es una foto cualquiera, la ignoramos o le decimos algo
            enviarRespuesta(chatId, "Linda foto. Si es un comprobante de pago, primero tenés que hacer un pedido y seleccionar 'Transferencia'.");
        }
    }
    private void enviarRespuestaCerrado(long chatId) {
        String closedMessage = """
            ¡Gracias por contactarnos!
            
            En este momento nos encontramos fuera de stock o cerrados. 🚫
            
            Nuestros horarios de atención son:
            Martes a Domingos, de 20:00PM a 01:00AM.
            
            ¡Te esperamos!
            """;
        enviarRespuesta(chatId, closedMessage);
    }
    private void answerCallbackQuery(String callbackId, String text) {
        org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answer =
                new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        answer.setText(text);
        answer.setShowAlert(false);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void iniciarProcesoDeDireccion(long chatId) {

        userStates.put(chatId, "AWAITING_ADDRESS");


        enviarRespuesta(chatId, """
                ¡Perfecto! Ya casi estamos.
                
                Por favor, copiá la plantilla de abajo, completala y enviala en un solo mensaje.
                """);

        String plantilla = """
- Nombre y Apellido: 
- Dirección (Calle, Número, Barrio): 
- Cualquier otra aclaración (ej: "tocar timbre 2B"): 
""";
        SendMessage msgPlantilla = new SendMessage();
        msgPlantilla.setChatId(String.valueOf(chatId));
        msgPlantilla.setText("```\n" + plantilla + "```");
        msgPlantilla.setParseMode("Markdown");

        enviarMensaje(msgPlantilla);
    }

    private void iniciarProcesoDePago(long chatId, String datosCliente) {

        Pedido pedido = pedidosEnProceso.get(chatId);
        if (pedido == null) {
            enviarRespuesta(chatId, "Ups, hubo un error con tu carrito. Por favor, inicia de nuevo con /realizar_pedido");
            return;
        }


        pedido.setClientInfo(datosCliente);

        userStates.put(chatId, "AWAITING_PAYMENT_CHOICE");


        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("¡Datos recibidos! Tu pedido es de $" + pedido.getTotal() + ".\n\n¿Cómo querés abonar?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton btnTransf = new InlineKeyboardButton();
        btnTransf.setText("💳 Transferencia");
        btnTransf.setCallbackData("PAY_TRANSFER");

        InlineKeyboardButton btnCash = new InlineKeyboardButton();
        btnCash.setText("💵 Efectivo");
        btnCash.setCallbackData("PAY_CASH");

        keyboard.add(List.of(btnTransf, btnCash));
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        enviarMensaje(message);
    }
    private void mostrarMenuEdicion(long chatId, long messageId) {
        Map<String, Integer> carrito = cartService.getCart(chatId);
        if (carrito.isEmpty()) {
            enviarRespuesta(chatId, "Tu carrito ya está vacío.");

            return;
        }

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId((int) messageId);
        editMessage.setText("Haz clic en un producto para restar 1 del carrito:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();


        for (Map.Entry<String, Integer> entry : carrito.entrySet()) {
            String prodId = entry.getKey();
            Integer cantidad = entry.getValue();
            var optProd = menuService.getProducto(prodId);

            if (optProd.isPresent()) {
                Producto p = optProd.get();
                InlineKeyboardButton btn = new InlineKeyboardButton();

                btn.setText(String.format("🗑️ %dx %s (Restar 1)", cantidad, p.getNombre()));
                btn.setCallbackData("REM_" + p.getId());
                keyboard.add(List.of(btn));
            }
        }


        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Volver al Carrito");
        backBtn.setCallbackData("VER_CARRITO");
        keyboard.add(List.of(backBtn));

        markup.setKeyboard(keyboard);
        editMessage.setReplyMarkup(markup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            System.err.println("Error al editar mensaje: " + e.getMessage());
        }
    }


    private void finalizarPedido(Pedido pedido) {


        String orderId = String.valueOf(orderCounter.incrementAndGet());
        pedido.setOrderId(orderId);

        pedidosPendientes.put(orderId, pedido);
        pedidosEnProceso.remove(pedido.getClientChatId());
        StringBuilder sbPedido = new StringBuilder();
        for (Map.Entry<String, Integer> entry : pedido.getItems().entrySet()) {
            String prodNombre = menuService.getProducto(entry.getKey())
                    .map(Producto::getNombre)
                    .orElse("Producto Desconocido");
            sbPedido.append(String.format("• %dx %s\n", entry.getValue(), prodNombre));
        }

        String msgAdmin = String.format(
                "🔔 ¡NUEVO PEDIDO! (ID: #%s) 🔔\n\n" +
                        "== DATOS DEL CLIENTE ==\n" +
                        "%s\n" +
                        "(Chat ID: %d)\n\n" +
                        "== MÉTODO DE PAGO ==\n" +
                        "%s\n\n" +
                        "== PEDIDO ==\n" +
                        "%s" +
                        "---------------------------\n" +
                        "TOTAL: $%.2f",
                pedido.getOrderId(),
                pedido.getClientInfo(),
                pedido.getClientChatId(),
                pedido.getMetodoDePago(),
                sbPedido.toString(),
                pedido.getTotal()
        );

        enviarRespuesta(Long.parseLong(this.adminChatId), msgAdmin);


        if (pedido.getMetodoDePago().startsWith("Efectivo")) {
            enviarRespuesta(pedido.getClientChatId(),
                    "¡Tu pedido (ID: #" + orderId + ") fue recibido! 🚀 " +
                            "Prepará el efectivo. ¡En breve estará listo!");
        }
    }
    /**
     * Este lo usa el comando /realizar_pedido (Crea un mensaje nuevo)
     */
    private void enviarMenuCategorias(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("\uD83C\uDF54 Este es el menu de " + menuService.getMenu().getNombreLocal() + ". ¿Qué te gustaría pedir?");
        message.setReplyMarkup(getMarkupCategorias());
        enviarMensaje(message);
    }

    /**
     * Este lo usa el botón "Volver" (Edita un mensaje existente)
     */
    private void enviarMenuCategorias(long chatId, long messageId) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId((int) messageId);
        editMessage.setText("\uD83C\uDF54 Este es el menu de " + menuService.getMenu().getNombreLocal() + ". ¿Qué te gustaría pedir?");
        editMessage.setReplyMarkup(getMarkupCategorias());
        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            enviarRespuesta(chatId, "Error al volver al menú.");
        }
    }

    /**
     * HELPER: Crea los botones de categoría para no repetir código.
     * Fíjate que el CallbackData ahora es "CAT:id" para que funcione
     * con el nuevo handleCallbackQuery.
     */
    private InlineKeyboardMarkup getMarkupCategorias() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();


        for (Categoria cat : menuService.getMenu().getCategorias()) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(cat.getNombre());

            btn.setCallbackData("CAT:" + cat.getId());
            keyboard.add(List.of(btn));
        }

        InlineKeyboardButton cartBtn = new InlineKeyboardButton();
        cartBtn.setText("🛒 Ver Carrito");
        cartBtn.setCallbackData("VER_CARRITO");
        keyboard.add(List.of(cartBtn));

        markup.setKeyboard(keyboard);
        return markup;
    }

    private void enviarMenuProductos(long chatId, long messageId, String catId) {

        Categoria categoria = menuService.getMenu().getCategorias().stream()
                .filter(c -> c.getId().equals(catId))
                .findFirst().orElse(null);

        if (categoria == null) {
            enviarRespuesta(chatId, "Ups, no encontré esa categoría.");
            return;
        }


        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId((int) messageId);
        editMessage.setText("Elegiste: " + categoria.getNombre() + ". ¿Qué producto queres?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Producto prod : categoria.getProductos()) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(prod.getNombre() + " ($" + prod.getPrecio() + ")");
            btn.setCallbackData("PROD:" + catId + ":" + prod.getId());
            keyboard.add(List.of(btn));
        }


        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Volver a Categorías");
        backBtn.setCallbackData("MENU_PRINCIPAL");
        keyboard.add(List.of(backBtn));

        markup.setKeyboard(keyboard);
        editMessage.setReplyMarkup(markup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            System.err.println("Error al editar mensaje: " + e.getMessage());
        }
    }



    private void mostrarCarrito(long chatId) {
        Map<String, Integer> carrito = cartService.getCart(chatId);

        if (carrito.isEmpty()) {
            enviarRespuesta(chatId, "Tu carrito está vacío. ¡Prueba /menu para empezar a pedir!");
            return;
        }

        StringBuilder sb = new StringBuilder("--- 🛒 TU CARRITO ---\n");
        double total = 0;

        for (Map.Entry<String, Integer> entry : carrito.entrySet()) {
            String prodId = entry.getKey();
            Integer cantidad = entry.getValue();

            var optProd = menuService.getProducto(prodId);
            if (optProd.isPresent()) {
                Producto p = optProd.get();
                double subtotal = p.getPrecio() * cantidad;
                sb.append(String.format("• %dx %s ($%.2f c/u) = $%.2f\n",
                        cantidad, p.getNombre(), p.getPrecio(), subtotal));
                total += subtotal;
            }
        }
        sb.append(String.format("\nTOTAL: $%.2f", total));


        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(sb.toString());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();


        InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
        confirmBtn.setText("✅ Confirmar Pedido");
        confirmBtn.setCallbackData("CONFIRM_ORDER");


        InlineKeyboardButton editBtn = new InlineKeyboardButton();
        editBtn.setText("✏️ Editar Carrito");
        editBtn.setCallbackData("EDIT_CART");

        keyboard.add(List.of(confirmBtn, editBtn));
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        enviarMensaje(message);
    }
    private boolean handleAdminCommands(long chatId, String text) {

        if (text.startsWith("/admin_help")) {
            enviarRespuesta(chatId, """
                Hola MOSTRO 👑. Estos son tus comandos:
                
                /abrir
                   Abre la tienda. El bot vuelve a tomar pedidos.
                   
                /cerrar
                   Cierra la tienda. El bot avisará que no hay stock.
                
                /admin_help
                   Muestra esta ayuda.
                   
                /pedidos
                   Muestra los pedidos pendientes CON DETALLE.
                   
                /listo <ID>
                   Avisa al cliente que su pedido <ID> está en camino.
                   
                /avisar <ID> <Mensaje>
                   Envía un <Mensaje> al cliente Y CANCELA el pedido <ID>.
                """);
            return true;
        }

        if (text.startsWith("/cerrar")) {

            isStoreOpen = false;
            enviarRespuesta(chatId, "✅ Tienda CERRADA.");
            return true;
        }

        if (text.startsWith("/abrir")) {

            isStoreOpen = true;
            enviarRespuesta(chatId, "✅ ¡Tienda ABIERTA!");
            return true;
        }


        if (text.startsWith("/avisar")) {
            String[] parts = text.split(" ", 3);
            if (parts.length == 3) {
                String orderId = parts[1].replace("#", "");
                String messageForClient = parts[2];

                // 1. Buscamos el PEDIDO
                Pedido pedido = pedidosPendientes.get(orderId);

                if (pedido != null) {

                    String header = "⚠️ ¡Atención! Mensaje del local sobre tu pedido #" + orderId + ":\n\n";
                    String footer = "\n\nDebido a este inconveniente, tu pedido ha sido CANCELADO. " +
                            "Por favor, realiza un nuevo pedido usando /realizar_pedido.";

                    enviarRespuesta(pedido.getClientChatId(), header + messageForClient + footer);


                    enviarRespuesta(chatId, "✅ Mensaje enviado y pedido #" + orderId + " CANCELADO.");


                    pedidosPendientes.remove(orderId);

                } else {
                    enviarRespuesta(chatId, "Error: No encontré ningún pedido pendiente con el ID #" + orderId);
                }
            } else {
                enviarRespuesta(chatId, "Error de formato. Usá: /avisar <ID_Pedido> <Mensaje para el cliente>");
            }
            return true;
        }


        if (text.startsWith("/listo")) {
            String[] parts = text.split(" ", 2);
            if (parts.length == 2) {
                String orderId = parts[1].replace("#", "");


                Pedido pedido = pedidosPendientes.get(orderId);

                if (pedido != null) {
                    enviarRespuesta(pedido.getClientChatId(),
                            "¡Tu pedido #" + orderId + " está listo y en camino! 🛵 ¡Buen provecho!");
                    enviarRespuesta(chatId, "✅ ¡Cliente del pedido #" + orderId + " notificado!");
                    pedidosPendientes.remove(orderId);
                } else {
                    enviarRespuesta(chatId, "Error: No encontré ningún pedido pendiente con el ID #" + orderId);
                }
            } else {
                enviarRespuesta(chatId, "Error de formato. Usá: /listo <ID_Pedido>");
            }
            return true;
        }

        if (text.equals("/pedidos")) {
            if (pedidosPendientes.isEmpty()) {
                enviarRespuesta(chatId, "No hay pedidos pendientes.");
            } else {
                StringBuilder sb = new StringBuilder("--- 📋 Pedidos Pendientes ---\n\n");
                for (Pedido pedido : pedidosPendientes.values()) {
                    sb.append(String.format("--- ID: #%s ---\n", pedido.getOrderId()));
                    sb.append("Cliente:\n").append(pedido.getClientInfo()).append("\n");

                    sb.append("Pago: ").append(pedido.getMetodoDePago()).append("\n");

                    sb.append("Items:\n");
                    for (Map.Entry<String, Integer> item : pedido.getItems().entrySet()) {
                        String prodNombre = menuService.getProducto(item.getKey())
                                .map(Producto::getNombre)
                                .orElse("Producto Desconocido");
                        sb.append(String.format("  • %dx %s\n", item.getValue(), prodNombre));
                    }
                    sb.append(String.format("Total: $%.2f\n\n", pedido.getTotal()));
                }
                enviarRespuesta(chatId, sb.toString());
            }
            return true;
        }

        return false;
    }
    private void enviarRespuesta(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        enviarMensaje(message);
    }


    private void enviarMensaje(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }
}