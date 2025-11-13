package com.SilverSorgo.ChatBot;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component //Se utiliza para que busque los componentes necesarios para su funcionamiento dentro del package asignado
public class CartService {
    //Es la clase encargada de manejar todas las funciones del carrito de compra
    private final Map<Long, Map<String, Integer>> userCarts = new ConcurrentHashMap<>(); //Aquí básicamente genero mi carrito se encarga de juntar el ChatId mediante un Long con el Map la misma logica del integrado a la clase Menu

    //Con esta función agregamos un producto al carrito
    public void addItem(long chatId, String productId, int quantity) {
        userCarts.computeIfAbsent(chatId, k -> new HashMap<>()).merge(productId, quantity, Integer::sum); //Acá revisa si el carrito ya fue generado con productos anteriormente o lo tiene que generar
    }
    //Con esta función retiro un producto del carrito
    public void sacarItem(long chatId, String productId) {
        Map<String, Integer> userCart = userCarts.get(chatId); //Acá buscó el carrito del usuario, a diferencia de la linea del AddItem utilizo simplemente un get, para que si el carrito no existe me devuelva un null directamente
        if (userCart == null || !userCart.containsKey(productId)) { //Si me
            return;
        }

        int currentQuantity = userCart.get(productId); //Variable para saber cuantos productos hay actualmente
        if (currentQuantity > 1) {
            //Si hay más de 1 item, solamente resto 1
            userCart.put(productId, currentQuantity - 1);
        } else {
            //Si solo quedaba 1 item, borro la entrada
            userCart.remove(productId);
        }
    }


    public Map<String, Integer> getCart(long chatId) {
        return userCarts.getOrDefault(chatId, new HashMap<>()); //Esta función busca el carrito del cliente, si existe, te lo devuelve, caso contrario, crea un carrito nuevo y vacio (Evita que crashee si no encuentra ningun carrito)
    }

    //Funcion para borrar el carrito
    public void clearCart(long chatId) {
        userCarts.remove(chatId);
    }
}