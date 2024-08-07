package tomato.gui.dungeons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class FixedSizedArray<T> extends ArrayList<T> {
    private final int size;

    public FixedSizedArray(int size) {
        this.size = size;
    }

    @Override
    public boolean add(T value) {
        if (size() < size) {
            final boolean result = super.add(value);
            this.reverse();
            return result;
        }

        remove(0);
        final boolean result = super.add(value);
        this.reverse();
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        if (size() < size + collection.size()) {
            final boolean result = super.addAll(collection);
            this.reverse();
            return result;
        }

        remove(Math.max(size - 1, collection.size()));
        final boolean result = super.addAll(collection);
        this.reverse();
        return result;
    }

    private void reverse() {
        Collections.reverse(this);
    }
}
