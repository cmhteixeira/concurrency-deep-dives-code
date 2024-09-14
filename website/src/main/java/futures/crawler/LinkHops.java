package futures.crawler;

import java.util.function.Consumer;

sealed interface LinkHops {

  static LinkHops empty(String root) {
    return new Root(root);
  }

  default Hop add(String link) {
    return new Hop(link, this);
  }

  String get();

  void forEach(Consumer<String> f);

  int size();

  record Hop(String current, LinkHops parent) implements LinkHops {
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
    public String get() {
      return current;
    }

    @Override
    public void forEach(Consumer<String> f) {
      f.accept(current);
      parent.forEach(f);
    }

    @Override
    public int size() {
      return 1 + parent.size();
    }
  }

  record Root(String root) implements LinkHops {
    public String toString() {
      return "[" + root + "]";
    }

    @Override
    public String get() {
      return root;
    }

    @Override
    public void forEach(Consumer<String> f) {
      f.accept(root);
    }

    @Override
    public int size() {
      return 1;
    }
  }
}
