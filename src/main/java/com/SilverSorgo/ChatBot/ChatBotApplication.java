package com.SilverSorgo.ChatBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication //es el encargado de dirigir y buscar los componentes en el package
public class ChatBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatBotApplication.class, args); //hace arrancar mi app
	}

}
