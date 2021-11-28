package com.helger.jsonxmlconvert.core;

public class XMLNodeSpecTextContent extends AbstractXMLNodeSpec
{
  public XMLNodeSpecTextContent (final int nodeType)
  {
    super (nodeType);
  }

  public boolean isXMLTextContent ()
  {
    return m_bIsXMLTextContent;
  }

  @Override
  public boolean isPrimitiveType ()
  {
    return true;
  }
}
