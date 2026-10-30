package ru.k1ndcat.lab1.dto;

import lombok.Builder;
import ru.k1ndcat.lab1.entity.Item;

@Builder
public record ItemDto(
        Long id,
        String name
) {

    public Item fromDto() {
        return Item.builder()
                .id(id)
                .name(name)
                .build();
    }

    public static ItemDto toDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .build();
    }
}
