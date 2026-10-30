package ru.k1ndcat.lab1.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ru.k1ndcat.lab1.annotation.BusinessController;
import ru.k1ndcat.lab1.dto.HandlerDto;

import java.util.ArrayList;
import java.util.List;

@RestController
public class HandlersController {

    private final RequestMappingHandlerMapping handlerMapping;

    public HandlersController(
            @Qualifier("requestMappingHandlerMapping")
            RequestMappingHandlerMapping handlerMapping
    ) {
        this.handlerMapping = handlerMapping;
    }

    @GetMapping("/handlers")
    public List<HandlerDto> getHandlers() {
        List<HandlerDto> result = new ArrayList<>();

        var mappings = handlerMapping.getHandlerMethods();
        for (var entry : mappings.entrySet()) {
            var mapping = entry.getKey();
            var handler = entry.getValue();

            if (!handler.getBeanType().isAnnotationPresent(BusinessController.class)) continue;

            for (var method : mapping.getMethodsCondition().getMethods())
                for (var path : mapping.getPatternValues())
                    result.add(new HandlerDto(method.name(), path));
        }

        return result;
    }
}
