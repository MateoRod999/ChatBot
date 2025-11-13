package com.SilverSorgo.ChatBot;

import lombok.Data; //Import de Lombok para utilizar el @Data
import java.util.Map; //Import para el Map

@Data //Evita que deba poner a mano todos los Getters y Setters, los hash y los toString
public class Pedido {
    private String orderId; //Para manejar el ID del pedido
    private long clientChatId; //Contiene el Chat ID del cliente
    private String clientInfo; //Es el copy que le mando al cliente para que ponga su información
    private Map<String, Integer> items; //Es donde guardo el ID del producto en un String y lo asocio a un Integer que es la cantidad de cierto producto que encargo el cliente
    private double total; //Es la variable que guarda el valor completo del pedido
    private String metodoDePago; //Acá guardo como fue realizado el pago: efectivo o transferencia

    //Toda esta información la obtenemos del JSON llamado menu.json

}