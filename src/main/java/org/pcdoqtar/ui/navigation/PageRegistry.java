
package org.pcdoqtar.ui.navigation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.scene.Node;

public final class PageRegistry {

    private final Map<String, Node> pages = new LinkedHashMap<>();

    public void register(String pageName, Node page) {
        if (pageName == null || pageName.isBlank()) {
            throw new IllegalArgumentException(
                    "Page name must not be empty."
            );
        }

        if (page == null) {
            throw new IllegalArgumentException(
                    "Page node must not be null."
            );
        }

        if (pages.containsKey(pageName)) {
            throw new IllegalArgumentException(
                    "Page already registered: " + pageName
            );
        }

        pages.put(pageName, page);
    }

    public Node get(String pageName) {
        Node page = pages.get(pageName);

        if (page == null) {
            throw new IllegalArgumentException(
                    "Page not registered: " + pageName
            );
        }

        return page;
    }

    public boolean contains(String pageName) {
        return pages.containsKey(pageName);
    }

    public Map<String, Node> getPages() {
        return Collections.unmodifiableMap(pages);
    }
}
