package com.SilverSorgo.ChatBot;

import lombok.Data;
import java.util.List;

@Data  //Evita que deba poner a mano todos los Getters y Setters, los hash y los toString
public class Categoria {
    private String nombre; //Nombre de la categoría
    private String id; //Su id
    private String descripcion; //Su descripción
    private List<Producto> productos; //Y toda la lista de productos que se encuentra en una determinada categoría
    //Toda esta información la obtenemos del JSON llamado menu.json

}