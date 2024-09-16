package futures.crawler;

import java.net.URI;
import java.util.function.Consumer;

sealed interface LinkHops {

  static LinkHops empty(URI root) {
    return new Root(root);
  }

  default Hop add(URI link) {
    return new Hop(link, this);
  }

  URI get();

  void forEach(Consumer<URI> f);

  int size();

  record Hop(URI current, LinkHops parent) implements LinkHops {
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("[");
      LinkHops currentNode = this;

      while (currentNode instanceof Hop) {
        buffer.append(((Hop) currentNode).current);
        LinkHops next = ((Hop) currentNode).parent;
        if (next instanceof Root) buffer.append(", ").append(((Root) next).root).append("]");
        else buffer.append(", ");
        currentNode = next;
      }
      return buffer.toString();
    }

    @Override
    public URI get() {
      return current;
    }

    @Override
    public void forEach(Consumer<URI> f) {
      f.accept(current);
      parent.forEach(f);
    }

    @Override
    public int size() {
      return 1 + parent.size();
    }
  }

  record Root(URI root) implements LinkHops {
    public String toString() {
      return "[" + root + "]";
    }

    @Override
    public URI get() {
      return root;
    }

    @Override
    public void forEach(Consumer<URI> f) {
      f.accept(root);
    }

    @Override
    public int size() {
      return 1;
    }
  }
}
