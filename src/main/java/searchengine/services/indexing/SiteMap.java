package searchengine.services.indexing;

import java.util.concurrent.CopyOnWriteArrayList;

public class SiteMap {
    private SiteMap parent;
    private int depth;
    private final String url;
    private final CopyOnWriteArrayList<SiteMap> children;

    public SiteMap(String url) {
        this.url = url;
        this.depth = 0;
        this.parent = null;
        this.children = new CopyOnWriteArrayList<>();
    }

    private int calculateDepth() {
        int result = 0;
        if (parent != null) {
            result = 1 + parent.calculateDepth();
        }
        return result;
    }

    public void addChild(SiteMap element) {
        SiteMap root = getRootElement();
        if (!root.contains(element.getUrl())) {
            element.setParent(this);
            children.add(element);
        }
    }

    private boolean contains(String url) {
        if (this.url.equals(url)) {
            return true;
        }
        for (SiteMap child : children) {
            if (child.contains(url))
                return true;
        }
        return false;
    }

    public String getUrl() {
        return url;
    }

    private void setParent(SiteMap sitemapNode) {
        synchronized (this) {
            this.parent = sitemapNode;
            this.depth = calculateDepth();
        }
    }

    public SiteMap getRootElement() {
        return parent == null ? this : parent.getRootElement();
    }

    public CopyOnWriteArrayList<SiteMap> getChildren() {
        return children;

    }
}
