package rabinizer.automata.nxt;

import java.util.Map;

public final class Util {

    // TODO: Create Mapping class

    static public <K> int getId(Map<K, Integer> map, K key) {
        Integer r = map.get(key);

        if (r == null) {
            int id = map.size();
            map.put(key, id);
            return id;
        }

        return r;
    }

}
