package com.helger.jsonxmlconvert.core;

public class PropertyXMLNodeSpec
{
  private final String m_sPropertyName;
  private final String m_sRef;
  private AbstractXMLNodeSpec m_aXmlNodeSpec;

  public PropertyXMLNodeSpec (final String propertyName, final String ref, final AbstractXMLNodeSpec xmlNodeSpec)
  {
    m_sPropertyName = propertyName;
    m_sRef = ref;
    m_aXmlNodeSpec = xmlNodeSpec;
  }

  public String getPropertyName ()
  {
    return m_sPropertyName;
  }

  public String getRef ()
  {
    return m_sRef;
  }

  public AbstractXMLNodeSpec getXmlNodeSpec ()
  {
    return m_aXmlNodeSpec;
  }

  public void setPropertyXMLNodeSpec (final AbstractXMLNodeSpec xmlNodeSpec)
  {
    m_aXmlNodeSpec = xmlNodeSpec;
  }
}
