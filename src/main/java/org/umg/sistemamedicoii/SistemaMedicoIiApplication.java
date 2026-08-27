package org.umg.sistemamedicoii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class SistemaMedicoIiApplication {

    static {
        // Fuerza la zona horaria de Guatemala ANTES de que arranque Spring,
        // así LocalDateTime.now() siempre devuelve hora de Guatemala sin
        // importar dónde corra el contenedor (local, Azure, etc.)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Guatemala"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SistemaMedicoIiApplication.class, args);
    }

}