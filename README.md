🛒 Bot de Pedidos para Telegram — Sistema de Gestión de Pedidos con Java

Un Bot de Telegram desarrollado en Java y Spring Boot, diseñado para gestionar pedidos gastronómicos mediante menús interactivos, carrito inteligente, mensajes guiados y panel administrativo.
Incluye un flujo completo desde la selección de productos hasta la confirmación del pedido, con manejo de estados y arquitectura modular.

🚀 Características Principales
🧾 Menú Interactivo Completo

El bot permite navegar categorías, ver productos y agregarlos al carrito mediante botones inline totalmente interactivos.

🍔 Selección por categorías

🧃 Productos con precio

➕ Cantidades ajustables

🛒 Carrito persistente por usuario

🛍️ Carrito Inteligente

Cada usuario posee un carrito propio almacenado en memoria:

🟢 Agregar productos

🔄 Modificar cantidades

🗑️ Eliminar productos

📦 Confirmar pedido

El bot muestra el total en tiempo real y acompaña el flujo paso a paso.

🧑‍💻 Panel Administrativo

Incluye comandos exclusivos para el administrador:

Comando	Función
/abrir	Abre la tienda
/cerrar	Cierra la tienda
/pedidos	Lista pedidos pendientes
/listo <ID>	Marca un pedido como completado
/avisar <ID> <msg>	Notifica al cliente y cancela el pedido

Se gestiona desde Telegram sin necesidad de backend adicional.

📝 Flujo Guiado del Cliente

El proceso es completamente intuitivo:

🧾 Selección del menú

🛒 Construcción del carrito

📄 Ingreso de datos del cliente

💳 Selección del método de pago

📬 Confirmación final

El sistema utiliza un FSM (Finite State Machine) para asegurar que el cliente no pueda saltar pasos.

🧠 Funcionamiento Interno

El bot se compone de servicios independientes y modelos bien definidos:

Componente	Función
TelegramBot	Núcleo del bot. Maneja mensajes, callbacks y estados
MenuService	Carga y administra el menú, categorías y productos
CartService	Administra el carrito por usuario
Pedido	Modelo del pedido del cliente
ChatClient (opcional)	Integración con IA para respuestas contextualizadas
🏗️ Estructura del Proyecto
src/
└── main/
    ├── java/
    │   └── com.tubot.telegram/
    │       ├── bot/
    │       │   └── TelegramBot.java
    │       ├── model/
    │       │   ├── Pedido.java
    │       │   ├── Producto.java
    │       │   └── Categoria.java
    │       ├── service/
    │       │   ├── MenuService.java
    │       │   └── CartService.java
    │       └── BotApplication.java
    └── resources/
        └── application.properties

🛠️ Tecnologías Utilizadas

☕ Java 17+

🍃 Spring Boot

💬 Telegram Bot API

📄 JSON / Maps en memoria

🤖 IA opcional (ChatClient)

💾 Persistencia

No utiliza base de datos.
Los datos se almacenan temporalmente en memoria:

🛒 Carritos por usuario

📦 Pedidos en proceso

🧾 Pedidos pendientes

🔄 Estados del usuario

Esto permite un despliegue muy simple y sin infraestructura adicional.

🔐 Seguridad

El administrador está identificado por un chatId único configurable

Los comandos críticos son solo para admin

Los callbacks usan formato controlado (CAT:, PROD:, PAY:…) evitando manipulaciones

No se guarda información sensible fuera de la sesión

📊 Diagrama de Clases

Incluye la arquitectura completa del sistema según tu diseño UML.

📌 Colocá la imagen en la raíz del proyecto con el nombre:
diagrama_bot.png

![Diagrama de Clases](https://imgur.com/a/pSRlORl)
