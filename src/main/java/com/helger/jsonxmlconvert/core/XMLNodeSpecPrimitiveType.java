package com.helger.jsonxmlconvert.core;

import org.json.JSONObject;

public class XMLNodeSpecPrimitiveType extends AbstractXMLNodeSpec
{
  public XMLNodeSpecPrimitiveType (final int nodeType)
  {
    super (nodeType);
  }

  public boolean isXMLAttribute ()
  {
    return m_bIsXMLAttribute;
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

  public void loadJSONPrimitiveValue (final JSONObject schema, final String propertyName) throws JSONSchemaLoadException
  {
    loadJSONValue (schema, propertyName);

    // FIXME: first attempt to recognize "$"
    if ("$".equals (propertyName))
    {
      m_bIsXMLTextContent = true;
    }

    // FIXME: first attempt to recognize "@attributes"
    if ('@' == propertyName.charAt (0))
    {
      m_bIsXMLAttribute = true;
      if (m_sXmlName == null)
        m_sXmlName = propertyName.substring (1);
    }
  }
}
