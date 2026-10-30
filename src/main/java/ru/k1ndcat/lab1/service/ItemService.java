package ru.k1ndcat.lab1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.k1ndcat.lab1.dto.ItemDto;
import ru.k1ndcat.lab1.entity.Item;
import ru.k1ndcat.lab1.repository.ItemRepository;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemDto create(ItemDto dto) {
        Item item = dto.fromDto();
        item.setId(null);

        return ItemDto.toDto(itemRepository.create(item));
    }

    public ItemDto findById(Long id) {
        return itemRepository.findById(id)
                .map(ItemDto::toDto)
                .orElseThrow(() -> notFound(id));
    }

    public ItemDto update(Long id, ItemDto dto) {
        return itemRepository.update(id, dto.fromDto())
                .map(ItemDto::toDto)
                .orElseThrow(() -> notFound(id));
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Запись с id=" + id + " не найдена"
        );
    }
}
