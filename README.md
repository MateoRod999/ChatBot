📦 Bot de Pedidos para Telegram – README Oficial

Un bot de Telegram completamente funcional para la gestión de pedidos gastronómicos.
Incluye menú dinámico, carrito por usuario, flujo de datos del cliente, métodos de pago y panel administrativo.

📘 Índice

Características principales

Arquitectura del sistema

Diagrama de clases

Descripción de módulos

Flujo del usuario

Flujo del administrador

Estados del bot (FSM)

Persistencia

Seguridad

Dependencias

Posibles mejoras

Cómo ejecutar el bot

🚀 Características principales

📱 Interfaz con teclados inline

🛒 Carrito por usuario con edición

🍽️ Menú dinámico por categorías

👤 Flujo completo: menú → carrito → datos → pago → confirmación

👨‍💼 Panel de administración

🔄 Sistema de estados FSM

📦 Gestión de pedidos pendientes

🧠 Arquitectura lista para integrar IA

🧱 Arquitectura del sistema

El bot está construido sobre:

TelegramLongPollingBot

Servicios propios (MenuService, CartService)

Modelos (Producto, Pedido, Categoría)

Mapas en memoria para persistencia temporal

Callbacks como interfaz interactiva

FSM para controlar el flujo de cada usuario

🖼️ Diagrama de Clases


![Diagrama de Clases](<img width="1943" height="1296" alt="VLPDajis4zth55sp-QYoHv-TfMfAPLapIIarzYYMd4qgYnROmq4PH3C0E8cSyf6ooX5yiG0a93VrOsqaiL__e5-_9jxuW2wqjHdz3wwlhs71zb5NhKEbl_vhuLVjMpJ9ClAVj6tGOGtAeFVp49pUje6MYkrQVoPu9I16MmgxXe1TU69XSDpQKh6pvE07wU6ViEG_-z-lNuroEIlyewL0K1" src="https://github.com/user-attachments/assets/2bd411a5-df9b-4fba-aaee-a99c060ae371" />
)


📊 Descripción del diagrama

El diagrama muestra claramente:

🔹 1. La clase principal TelegramBot

Hereda de TelegramLongPollingBot

Usa:

MenuService

CartService

ChatClient (IA)

Gestiona:

Estados por usuario

Carritos

Pedidos en proceso

Pedidos pendientes

Callbacks y mensajes

🔹 2. MenuService

Responsable del menú y categorías.

🔹 3. CartService

Encargado del carrito por usuario.

🔹 4. Modelos

Menu

Categoria

Producto

Pedido

🔹 5. Librerías externas

TelegramBots API

ChatClient (LLM)

Update (Telegram)

En conjunto, el diagrama refleja la arquitectura modular y escalable del bot.

🧩 Descripción detallada de cada módulo
🔹 TelegramBot.java

Core del sistema. Maneja todo el flujo:

Mensajes / callbacks

Carrito

Datos del cliente

Métodos de pago

Administración

🔹 MenuService.java

Carga el menú inicial y permite consultar categorías/productos.

Funciones:

cargarMenu()

getMenuComoTexto()

getProductoPorId()

🔹 CartService.java

Carrito personalizado por usuario.

Métodos:

addItem()

clearCart()

getCart()

removeItem()

🔹 Pedido.java

Modelo del pedido final.

Atributos:

orderId

clientChatId

items

total

metodoDePago

direccion

🧭 Flujo del usuario
/realizar_pedido
    ↓
Categorías
    ↓
Productos
    ↓
Carrito (ver/editar/confirmar)
    ↓
Datos personales
    ↓
Método de pago
    ↓
Pedido finalizado → Admin + Cliente

🧭 Flujo del Administrador
Comando	Función
/abrir	Habilita pedidos
/cerrar	Bloquea pedidos
/pedidos	Lista pedidos pendientes
/listo ID	Marca pedido como listo
/avisar ID mensaje	Notifica al cliente y cancela el pedido
🔄 Sistema de Estados (FSM)
NONE
AWAITING_CLIENT_DATA
AWAITING_PAYMENT_CHOICE
ORDER_CONFIRMED


Controlan el flujo del usuario para evitar inconsistencias.

💽 Persistencia

Se usan Maps en memoria:

Carrito por usuario

Pedido en proceso

Pedido pendiente

Estado del usuario

Fácil de reemplazar por base de datos.

🔒 Seguridad

Admin con chatId fijo

Callbacks controlados por prefijos

Sin persistencia de datos sensibles

Flujo guiado con FSM

🛠️ Dependencias necesarias
telegrambots
telegrambotsextensions
google-genai (opcional)

🌱 Posibles mejoras futuras

Base de datos real

Panel web

Integración completa con IA

MercadoPago / QR

Multi-sucursal

Registro persistente de pedidos

▶️ Cómo ejecutar el bot
git clone https://github.com/usuario/repositorio.git


Insertar tu token de Telegram:

@Override
public String getBotToken() {
    return "TOKEN_AQUÍ";
}


Ejecutar:

mvn spring-boot:run
