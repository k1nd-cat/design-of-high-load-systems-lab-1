package ru.k1ndcat.lab1.repository;

import org.springframework.stereotype.Repository;
import ru.k1ndcat.lab1.entity.Item;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ItemRepository {

    private final ConcurrentHashMap<Long, Item> items =
            new ConcurrentHashMap<>();

    private final AtomicLong idSequence = new AtomicLong();

    public Item create(Item item) {
        long id = idSequence.incrementAndGet();
        item.setId(id);
        items.put(id, item);

        return item;
    }

    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(items.get(id));
    }

    public Optional<Item> update(Long id, Item item) {
        Item updated = items.computeIfPresent(id, (key, existing) -> {
            item.setId(key);
            return item;
        });

        return Optional.ofNullable(updated);
    }
}
