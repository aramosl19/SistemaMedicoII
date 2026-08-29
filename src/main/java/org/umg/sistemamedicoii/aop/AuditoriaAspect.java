package org.umg.sistemamedicoii.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaRepository;

import java.lang.reflect.Method;
import java.util.Map;

@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "resultado")
    public void registrar(JoinPoint joinPoint, Auditable auditable, Object resultado) {
        try {
            Integer entidadId = extraerId(resultado, joinPoint.getArgs());
            AuditoriaHelper.registrar(auditoriaRepository, auditable.value(), auditable.entidad(),
                    entidadId, auditable.value());
        } catch (Exception e) {
            log.warn("No se pudo registrar la bitácora de operación: {}", e.getMessage());
        }
    }

    private Integer extraerId(Object resultado, Object[] args) {
        Integer id = extraerIdDeResultado(resultado);
        if (id != null) return id;
        for (Object arg : args) {
            if (arg instanceof Integer i) return i;
        }
        return null;
    }

    private Integer extraerIdDeResultado(Object resultado) {
        if (resultado == null) return null;
        if (resultado instanceof Map<?, ?> mapa) {
            for (Object valor : mapa.values()) {
                Integer id = extraerIdDeObjeto(valor);
                if (id != null) return id;
            }
            return null;
        }
        return extraerIdDeObjeto(resultado);
    }

    private Integer extraerIdDeObjeto(Object obj) {
        try {
            Method m = obj.getClass().getMethod("getId");
            Object val = m.invoke(obj);
            return (val instanceof Integer i) ? i : null;
        } catch (Exception e) {
            return null;
        }
    }
}