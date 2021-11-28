package com.helger.jsonxmlconvert.core;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public class SimplePath
{
  public static final int JSON_PATH = 1;
  public static final int XML_PATH = 2;

  private static final String ROOT_XML_PATH = "/";
  private static final String ROOT_JSON_PATH = "$";

  private final Deque <Object> m_aPath = new LinkedList <> ();
  private final int m_nPathType;

  public SimplePath (final int pathType)
  {
    if (pathType != JSON_PATH && pathType != XML_PATH)
      throw new RuntimeException ("Invalid pathType");
    m_nPathType = pathType;
  }

  public SimplePath pushElement (final String elementName)
  {
    if (elementName == null)
      throw new NullPointerException ("Element is null");
    m_aPath.push (elementName);
    return this;
  }

  public SimplePath pushXMLAttribute (final String attributeName)
  {
    // should throw exception in case of JSON_PATH
    if (attributeName == null)
      throw new NullPointerException ("Attribute is null");
    m_aPath.push ("@" + attributeName);
    return this;
  }

  public SimplePath pushIndex (final int index)
  {
    if (index < 0)
      throw new IndexOutOfBoundsException ("Index can not be negative");
    m_aPath.push (Integer.valueOf (index));
    return this;
  }

  /**
   * May throw EmptyStackException
   *
   * @return this object
   */
  public SimplePath pop ()
  {
    m_aPath.pop ();
    return this;
  }

  /**
   * @return String such as /root/children[3]/@id
   */
  private String _getFullXMLPath ()
  {
    if (m_aPath.isEmpty ())
      return ROOT_XML_PATH;

    final StringBuilder aSB = new StringBuilder ();
    final Iterator <Object> pathIterator = m_aPath.descendingIterator ();
    while (pathIterator.hasNext ())
    {
      final Object o = pathIterator.next ();
      if (o instanceof String)
        aSB.append ('/').append (o);
      else
        if (o instanceof Integer)
          aSB.append ('[').append (o).append (']');
      // else error
    }
    return aSB.toString ();
  }

  /**
   * @return String such as $.children[3].firstname
   */
  private String _getFullJSONPath ()
  {
    if (m_aPath.isEmpty ())
      return ROOT_JSON_PATH;

    final StringBuilder aSB = new StringBuilder ("$");
    final Iterator <Object> pathIterator = m_aPath.descendingIterator ();
    while (pathIterator.hasNext ())
    {
      final Object o = pathIterator.next ();
      if (o instanceof String)
        aSB.append ('.').append (o);
      else
        if (o instanceof Integer)
          aSB.append ('[').append (o).append (']');
      // else error
    }
    return aSB.toString ();
  }

  @Override
  public String toString ()
  {
    if (m_nPathType == JSON_PATH)
      return _getFullJSONPath ();
    return _getFullXMLPath ();
  }

  @Override
  public SimplePath clone ()
  {
    final SimplePath sp = new SimplePath (this.m_nPathType);
    sp.m_aPath.addAll (m_aPath);
    return sp;
  }
}
