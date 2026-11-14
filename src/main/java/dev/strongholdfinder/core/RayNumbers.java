package dev.strongholdfinder.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The number a player sees for each ray: its 1-based place in the throw list, with no gaps.
 * Deleting a ray shifts every later ray down by one. The real {@link Throw#id()} stays for saves and pair keys.
 */
public final class RayNumbers {
    private final Map<Integer, Integer> numbers;

    private RayNumbers(Map<Integer, Integer> numbers) {
        this.numbers = numbers;
    }

    public static RayNumbers of(List<Throw> all) {
        Map<Integer, Integer> numbers = new HashMap<>();
        for (int i = 0; i < all.size(); i++) {
            numbers.put(all.get(i).id(), i + 1);
        }
        return new RayNumbers(numbers);
    }

    public int of(Throw source) {
        return of(source.id());
    }

    /** 0 when the id is not in the list. */
    public int of(int id) {
        return numbers.getOrDefault(id, 0);
    }
}
