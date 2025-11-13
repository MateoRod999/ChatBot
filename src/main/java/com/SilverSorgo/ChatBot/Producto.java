package com.SilverSorgo.ChatBot;

import lombok.Data;

@Data //Evita que deba poner a mano todos los Getters y Setters, los hash y los toString
public class Producto {
    private String id; //Acá guardo el ID del producto
    private String nombre; // Su nombre
    private String descripcion; //La descripción
    private double precio; //y Precio
    //Toda esta información la obtenemos del JSON llamado menu.json
}