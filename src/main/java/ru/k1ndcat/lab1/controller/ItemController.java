package ru.k1ndcat.lab1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.k1ndcat.lab1.annotation.BusinessController;
import ru.k1ndcat.lab1.dto.ItemDto;
import ru.k1ndcat.lab1.service.ItemService;

@BusinessController
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@RequestBody ItemDto dto) {
        return itemService.create(dto);
    }

    @GetMapping("/{id}")
    public ItemDto findById(@PathVariable("id") Long id) {
        return itemService.findById(id);
    }

    @PutMapping("/{id}")
    public ItemDto update(
            @PathVariable("id") Long id,
            @RequestBody ItemDto dto
    ) {
        return itemService.update(id, dto);
    }
}
