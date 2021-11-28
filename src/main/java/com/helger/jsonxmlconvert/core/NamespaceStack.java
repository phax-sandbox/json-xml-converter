package com.helger.jsonxmlconvert.core;

import java.util.LinkedList;

public class NamespaceStack <E> extends LinkedList <E>
{
  /**
   * Pushes namespace E e on the stack. If e is null then the old and therefore
   * current namespace is pushed
   */
  @Override
  public void push (final E e)
  {
    if (isEmpty ())
      super.push (e);
    else
      if (e == null)
        super.push (this.peek ());
      else
        super.push (e);
  }
}
