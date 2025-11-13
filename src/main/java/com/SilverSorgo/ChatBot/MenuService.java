package com.SilverSorgo.ChatBot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service //Es para que Spring la haga funcionar cuando arranque el bot así está lista para cuando se la necesite
public class MenuService {

    private Menu menu;

    @PostConstruct //Es para que ejecute el metodo cargarMenu() UNA SOLA VEZ
    public void cargarMenu() { //Es la encargada de leer el archivo json con todo el menú
        try {
            ClassPathResource resource = new ClassPathResource("menu.json");
            try (InputStream inputStream = resource.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                menu = mapper.readValue(inputStream, Menu.class);
                System.out.println(">>> ¡Menú cargado! Local: " + menu.getNombreLocal()); //Con esta linea sé si se pudo cargar el menú o no
            }
        } catch (IOException e) {
            System.err.println("!!! ERROR AL CARGAR EL menu.json !!!");
            throw new RuntimeException("No se pudo cargar el menú.", e);
        }
    }

    public Menu getMenu() { //Funcion para obtener el menu
        return menu;
    }

    public String getMenuComoTexto() { //convierte los objetos en Strings para que el bot le pase el menú al cliente cuando use /menu
        StringBuilder sb = new StringBuilder(); //uso StringBuilder para agregar texto utilizando simplemente .append()
        sb.append("Este es el menú de ").append(menu.getNombreLocal()).append(":\n\n");

        for (Categoria cat : menu.getCategorias()) { //Busca en las categorias del menú y cada categoría que encuentra la mete en una variable temporal cat para mostrarle al cliente
            sb.append("").append(cat.getNombre()).append("\n");

            for (Producto prod : cat.getProductos()) { //Lo mismo que lo anterior pero con productos
                sb.append("  - ").append(prod.getNombre()).append(": ").append(prod.getDescripcion()).append(" ($").append(prod.getPrecio()).append(")\n");
            }
            sb.append("\n"); //Agrego salto de linea meramente estetico
        }
        return sb.toString();
    }

    public Optional<Producto> getProducto(String productId) { //Una variable Optional que contiene al producto, para que al buscar un producto no se crashee si no encuentra o no existe el producto
        return menu.getCategorias().stream().flatMap(cat -> cat.getProductos().stream()).filter(prod -> prod.getId().equals(productId)).findFirst(); // Vaciá todos los productos de todas las categorías en una sola fila, busca por su ID y devuelve el primero.
    }
}