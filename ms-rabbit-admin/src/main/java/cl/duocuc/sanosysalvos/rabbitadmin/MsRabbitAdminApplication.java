package cl.duocuc.sanosysalvos.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio administrador de RabbitMQ.
 * Expone una API REST para crear, consultar y eliminar colas, exchanges y
 * bindings del broker, además de publicar mensajes de prueba e inspeccionar
 * DLQs. Solo accesible para usuarios con rol ADMIN (JWT de Auth0).
 */
@SpringBootApplication
public class MsRabbitAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsRabbitAdminApplication.class, args);
    }
}
