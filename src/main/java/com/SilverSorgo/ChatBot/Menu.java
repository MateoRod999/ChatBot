package com.SilverSorgo.ChatBot;

import lombok.Data;
import java.util.List;

@Data //Evita que deba poner a mano todos los Getters y Setters, los hash y los toString
public class Menu {
    private String nombreLocal; //Guardo el nombre del local
    private List<Categoria> categorias; //Lista con las categorias de productos que hay en la carta del menú
    //Toda esta información la obtenemos del JSON llamado menu.json

}