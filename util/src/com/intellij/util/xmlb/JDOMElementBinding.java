package com.intellij.util.xmlb;

import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

class JDOMElementBinding implements Binding {
  private Accessor myAccessor;
  private String myTagName;

  public JDOMElementBinding(final Accessor accessor) {
    myAccessor = accessor;
    final Tag tag = XmlSerializerImpl.findAnnotation(myAccessor.getAnnotations(), Tag.class);
    assert tag != null : "jdom.Element property without @Tag annotation: " + accessor;
    myTagName = tag.value();
  }

  public Object serialize(Object o, Object context) {
    throw new UnsupportedOperationException("Method serialize is not supported in " + getClass());
  }

  @Nullable
  public Object deserialize(Object context, Object... nodes) {
    Element[] result = new Element[nodes.length];

    System.arraycopy(nodes, 0, result, 0, nodes.length);

    if (myAccessor.getValueClass().isArray()) {
      myAccessor.write(context, result);
    }
    else {
      assert result.length == 1;
      myAccessor.write(context, result[0]);
    }
    return context;
  }

  public boolean isBoundTo(Object node) {
    return node instanceof Element && ((Element)node).getName().equals(myTagName);
  }

  public Class getBoundNodeType() {
    throw new UnsupportedOperationException("Method getBoundNodeType is not supported in " + getClass());
  }

  public void init() {
  }
}
