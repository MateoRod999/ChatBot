# 🍔 Charly Burguer Bot

Bot de Telegram para gestionar pedidos de hamburguesas con IA integrada. Desarrollado con Spring Boot, Telegram Bots API y Groq (LLaMA 3.1).

## 📋 Características

- **Menú interactivo** con categorías y productos
- **Carrito de compras** con edición en tiempo real
- **Gestión de pedidos** con seguimiento por ID
- **Múltiples métodos de pago**: Efectivo y Transferencia
- **IA conversacional** para atención al cliente
- **Panel de administración** para gestionar pedidos
- **Sistema de estados** para manejo de flujos complejos

## 🚀 Tecnologías

- Java 21
- Spring Boot 3.5.6
- Spring AI 1.0.3 (con Groq API)
- Telegram Bots API 6.9.7.1
- Maven 3.9.11
- Lombok
- Jackson (JSON)

## 📦 Instalación

### Prerrequisitos

- Java 21 o superior
- Maven 3.x
- Cuenta de Telegram y bot creado (vía @BotFather)
- API Key de Groq

### Configuración

1. **Clonar el repositorio**
```bash
git clone <url-del-repositorio>
cd ChatBot
```

2. **Configurar variables de entorno**

Crear un archivo `.env` o configurar las siguientes variables:

```properties
GROQ_API_KEY=tu_api_key_de_groq
TELEGRAM_BOT_USERNAME=tu_bot_username
TELEGRAM_BOT_TOKEN=tu_bot_token
TELEGRAM_ADMIN_ID=tu_chat_id_de_admin
ADMIN_ALIAS=tu_alias_mp
ADMIN_CVU=tu_cvu
ADMIN_TITULAR=nombre_del_titular
```

3. **Personalizar el menú**

Editar el archivo `src/main/resources/menu.json` con tus productos:

```json
{
  "nombreLocal": "Tu Local",
  "categorias": [
    {
      "nombre": "Categoría",
      "id": "id_categoria",
      "descripcion": "Descripción",
      "productos": [
        {
          "id": "PROD001",
          "nombre": "Producto",
          "descripcion": "Descripción del producto",
          "precio": 1000.0
        }
      ]
    }
  ]
}
```

### Compilación y Ejecución

**Modo desarrollo:**
```bash
./mvnw spring-boot:run
```

**Compilar JAR:**
```bash
./mvnw clean package -DskipTests
java -jar target/ChatBot-0.0.1-SNAPSHOT.jar
```

**Docker:**
```bash
docker build -t charly-bot .
docker run -p 8080:8080 --env-file .env charly-bot
```

## 📱 Uso del Bot

### Comandos para Clientes

- `/menu` - Ver el menú completo
- `/realizar_pedido` - Iniciar un nuevo pedido
- `/carrito` - Ver y editar el carrito actual

### Comandos para Administradores

- `/admin_help` - Ayuda de comandos administrativos
- `/abrir` - Abrir la tienda
- `/cerrar` - Cerrar la tienda
- `/pedidos` - Ver todos los pedidos pendientes
- `/listo <ID>` - Marcar pedido como listo y notificar cliente
- `/avisar <ID> <Mensaje>` - Enviar mensaje al cliente y cancelar pedido

### Flujo de Pedido

1. Cliente usa `/realizar_pedido`
2. Selecciona categoría → producto → cantidad
3. Revisa el carrito con `/carrito`
4. Confirma el pedido
5. Ingresa datos de entrega (nombre, dirección)
6. Selecciona método de pago:
   - **Transferencia**: Envía comprobante por foto
   - **Efectivo**: Indica con cuánto va a pagar
7. Recibe confirmación con ID de pedido
8. Admin gestiona el pedido y notifica cuando está listo

## 🤖 IA Conversacional

El bot utiliza LLaMA 3.1 (vía Groq) para:

- Responder preguntas sobre el menú
- Proporcionar información sobre horarios
- Guiar a los usuarios hacia los comandos correctos
- Mantener conversaciones naturales

**Configuración del modelo:**
- Modelo: `llama-3.1-8b-instant`
- Temperatura: 0.7
- Max Tokens: 2048

## 🏗️ Arquitectura

```
src/main/java/com/SilverSorgo/ChatBot/
├── ChatBotApplication.java      # Punto de entrada
├── TelegramBot.java             # Lógica principal del bot
├── MenuService.java             # Gestión del menú
├── CartService.java             # Gestión de carritos
├── Menu.java                    # Modelo del menú
├── Categoria.java               # Modelo de categoría
├── Producto.java                # Modelo de producto
└── Pedido.java                  # Modelo de pedido

src/main/resources/
├── application.properties       # Configuración
└── menu.json                   # Datos del menú
```

### Componentes Principales

- **TelegramBot**: Maneja updates, comandos y callbacks
- **MenuService**: Carga y proporciona acceso al menú desde JSON
- **CartService**: Gestiona carritos por usuario (ConcurrentHashMap)
- **ChatClient**: Integración con IA para conversaciones

## 🔒 Seguridad

- Validación de admin por Chat ID
- Estados de usuario aislados por sesión
- Manejo de errores y casos edge
- Sanitización de entradas

## 📊 Estados del Usuario

El bot mantiene estados para cada usuario:

- `AWAITING_ADDRESS` - Esperando datos de entrega
- `AWAITING_PAYMENT_CHOICE` - Esperando selección de pago
- `AWAITING_COMPROBANTE` - Esperando foto del comprobante
- `AWAITING_CASH_AMOUNT` - Esperando monto en efectivo

## 🐛 Troubleshooting

**Bot no responde:**
- Verificar que el token sea correcto
- Comprobar que las variables de entorno estén configuradas
- Revisar logs en consola

**Error al cargar el menú:**
- Verificar que `menu.json` tenga formato válido
- Comprobar que todos los IDs sean únicos

**IA no responde:**
- Verificar la API Key de Groq
- Comprobar conexión a internet
- Revisar límites de rate en la API

## 📊 Diagrama de Clases

![Diagrama de Clases](Diagrama de clases.png)

El proyecto está estructurado con las siguientes clases principales:
- **TelegramBot**: Controlador principal del bot
- **MenuService**: Servicio de gestión del menú
- **CartService**: Servicio de gestión de carritos
- **Menu, Categoria, Producto, Pedido**: Modelos de datos

---

⭐ Si te gustó el proyecto, dale una estrella en GitHub!
